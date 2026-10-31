#!/usr/bin/env python3
"""Verify a downloaded Maestro CI artifact without extracting or installing it.

The SHA-256 must be obtained INDEPENDENTLY from the exact GitHub Actions
'Check JAR and checker regressions' job output. A checksum shipped in the
same ZIP, by itself, is NOT proof of authenticity, run success or permissions.
This is a local integrity check, not Minecraft/Phase 5 acceptance.
"""
import hashlib
import io
import json
from pathlib import Path
import re
import stat
import sys
import unittest
import warnings
import zipfile

MAX_ARTIFACT = 128 * 1024 * 1024
MAX_JAR = 96 * 1024 * 1024
VERSION = re.compile(r'[1-9][0-9]*\.[0-9]+\.[0-9]+')
DIGEST = re.compile(r'[0-9a-f]{64}')


def rejected():
    return ValueError('Untrusted Maestro CI artifact; installation refused')


def verify_bytes(bundle, version, independent_jar_sha256):
    if not isinstance(bundle, bytes) or not 0 < len(bundle) <= MAX_ARTIFACT:
        raise rejected()
    if not VERSION.fullmatch(version) or not DIGEST.fullmatch(independent_jar_sha256):
        raise rejected()
    jar_name = f'maestroai-{version}.jar'
    try:
        with zipfile.ZipFile(io.BytesIO(bundle)) as outer:
            entries = outer.infolist()
            if len(entries) != 2 or {i.filename for i in entries} != {jar_name, 'sha256.txt'}:
                raise rejected()
            for entry in entries:
                mode = entry.external_attr >> 16
                if entry.is_dir() or entry.flag_bits & 1 or stat.S_ISLNK(mode) or stat.S_ISDIR(mode):
                    raise rejected()
                if entry.file_size < 1 or entry.file_size > (MAX_JAR if entry.filename == jar_name else 256):
                    raise rejected()
            checksum = outer.read('sha256.txt').decode('ascii')
            expected_line = f'{independent_jar_sha256}  {jar_name}\n'
            if checksum != expected_line:
                raise rejected()
            payload = outer.read(jar_name)
            if hashlib.sha256(payload).hexdigest() != independent_jar_sha256:
                raise rejected()
        with zipfile.ZipFile(io.BytesIO(payload)) as mod:
            names = mod.namelist()
            if len(names) != len(set(names)) or 'fabric.mod.json' not in names or 'LICENSE_maestroai' not in names:
                raise rejected()
            if mod.getinfo('fabric.mod.json').file_size > 64 * 1024:
                raise rejected()
            metadata = json.loads(mod.read('fabric.mod.json'))
            if metadata.get('id') != 'maestroai' or metadata.get('version') != version:
                raise rejected()
            if not any(name.startswith('com/maestroai/') and name.endswith('.class') for name in names):
                raise rejected()
        return True
    except (zipfile.BadZipFile, KeyError, UnicodeError, OSError, ValueError, RuntimeError, TypeError):
        raise rejected() from None


class ArtifactGuards(unittest.TestCase):
    VERSION = '1.0.88'

    @staticmethod
    def jar(version='1.0.88'):
        file = io.BytesIO()
        with zipfile.ZipFile(file, 'w') as z:
            z.writestr('fabric.mod.json', json.dumps({'id': 'maestroai', 'version': version}))
            z.writestr('LICENSE_maestroai', 'Synthetic fixture notice')
            z.writestr('com/maestroai/Acceptance.class', b'\xca\xfe\xba\xbe\x00\x00\x00A')
        return file.getvalue()

    @staticmethod
    def bundle(jar, extra=None, sha=None):
        digest = hashlib.sha256(jar).hexdigest()
        file = io.BytesIO()
        with zipfile.ZipFile(file, 'w') as z:
            z.writestr('maestroai-1.0.88.jar', jar)
            z.writestr('sha256.txt', (sha or digest) + '  maestroai-1.0.88.jar\n')
            for name, content in (extra or {}).items():
                z.writestr(name, content)
        return file.getvalue(), digest

    def test_valid_exact_digest(self):
        z, digest = self.bundle(self.jar())
        self.assertTrue(verify_bytes(z, self.VERSION, digest))

    def test_bundled_checksum_is_not_an_independent_pin(self):
        z, digest = self.bundle(self.jar())
        with self.assertRaises(ValueError):
            verify_bytes(z, self.VERSION, 'e' * 64)

    def test_tampered_checksum(self):
        z, digest = self.bundle(self.jar(), sha='0' * 64)
        with self.assertRaises(ValueError):
            verify_bytes(z, self.VERSION, digest)

    def test_wrong_mod_version(self):
        z, digest = self.bundle(self.jar('1.0.87'))
        with self.assertRaises(ValueError):
            verify_bytes(z, self.VERSION, digest)

    def test_extra_private_content(self):
        z, digest = self.bundle(self.jar(), {'private-replay.log': 'no'})
        with self.assertRaises(ValueError):
            verify_bytes(z, self.VERSION, digest)

    def test_traversal_payload(self):
        z, digest = self.bundle(self.jar(), {'../other.jar': 'no'})
        with self.assertRaises(ValueError):
            verify_bytes(z, self.VERSION, digest)

    def test_duplicate_jar_entries(self):
        z, digest = self.bundle(self.jar())
        stream = io.BytesIO(z)
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(stream, 'a') as output:
                output.writestr('maestroai-1.0.88.jar', self.jar())
        with self.assertRaises(ValueError):
            verify_bytes(stream.getvalue(), self.VERSION, digest)

    def test_invalid_zip(self):
        with self.assertRaises(ValueError):
            verify_bytes(b'not a zip archive', self.VERSION, 'a' * 64)

    def test_invalid_commit_independent_digest(self):
        z, digest = self.bundle(self.jar())
        for bad in ('0'*63, 'A'*64, '../unsafe'):
            with self.assertRaises(ValueError):
                verify_bytes(z, self.VERSION, bad)

    def test_symlink_payload(self):
        z, digest = self.bundle(self.jar())
        stream = io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(z)) as original, zipfile.ZipFile(stream, 'w') as output:
            for entry in original.infolist():
                copied = zipfile.ZipInfo(entry.filename)
                if entry.filename == 'sha256.txt':
                    copied.create_system = 3
                    copied.external_attr = (stat.S_IFLNK | 0o777) << 16
                output.writestr(copied, original.read(entry.filename))
        with self.assertRaises(ValueError):
            verify_bytes(stream.getvalue(), self.VERSION, digest)


def main(args):
    if args == ['--self-test']:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(ArtifactGuards)
        return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1
    if len(args) != 3:
        print('Usage: python verification/phase5_verify_download.py ARTIFACT.zip VERSION INDEPENDENT_JAR_SHA256',
              file=sys.stderr)
        return 2
    try:
        file, version, digest = args
        if not Path(file).is_file() or Path(file).stat().st_size > MAX_ARTIFACT:
            raise rejected()
        verify_bytes(Path(file).read_bytes(), version, digest)
        print('Phase5Artifact[checksum=INDEPENDENTLY_PINNED_MATCH;'
              'jar=LOCAL_STRUCTURE_VERIFIED;'
              'source_run=EXTERNAL_CHECK_REQUIRED;historical_archive=UNVERIFIED;'
              'minecraft=UNTESTED;authority=NONE]')
        return 0
    except (ValueError, OSError, RuntimeError):
        print('Phase5Artifact[verdict=ABSTAIN;authority=NONE]', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
