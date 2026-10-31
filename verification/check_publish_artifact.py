#!/usr/bin/env python3
"""Fail-closed release gate after BOTH independently pinned simulator jobs.

Reads only the untrusted intermediate build artifact directory from this run.
The workflow 'needs: [verify, pack_source]' is separately enforced by repository
safeguards; this byte checker cannot itself attest GitHub job status.
"""
import hashlib
import io
import json
import os
from pathlib import Path
import re
import stat
import sys
import tempfile
import unittest
import warnings
import zipfile

MAX_JAR = 100 * 1024 * 1024
HASH_PATTERN = re.compile(r'[a-f0-9]{64}')


def reject():
    return ValueError('Verified JAR release candidate rejected')


def project_version(root):
    values = [
        line.split('=', 1)[1]
        for line in (root / 'gradle.properties').read_text().splitlines()
        if line.startswith('version=')
    ]
    if len(values) != 1 or not re.fullmatch(r'[1-9][0-9]*\.[0-9]+\.[0-9]+', values[0]):
        raise reject()
    return values[0]


def validate(directory, version):
    if directory.is_symlink() or not directory.is_dir():
        raise reject()
    jar_name = f'maestroai-{version}.jar'
    if {p.name for p in directory.iterdir()} != {jar_name, 'sha256.txt'}:
        raise reject()
    jar, receipt = directory / jar_name, directory / 'sha256.txt'
    for p in (jar, receipt):
        if p.is_symlink() or not p.is_file():
            raise reject()
    if not 100 <= jar.stat().st_size <= MAX_JAR or not 64 <= receipt.stat().st_size <= 256:
        raise reject()
    value = receipt.read_bytes()
    if not value.isascii():
        raise reject()
    digest = value.decode('ascii').split('  ', 1)[0]
    if not HASH_PATTERN.fullmatch(digest) or value != f'{digest}  {jar_name}\n'.encode():
        raise reject()
    if hashlib.sha256(jar.read_bytes()).hexdigest() != digest:
        raise reject()
    try:
        with zipfile.ZipFile(jar) as zipf:
            names = zipf.namelist()
            if len(names) != len(set(names)) or 'fabric.mod.json' not in names or \
                    'com/maestroai/MaestroAI.class' not in names:
                raise reject()
            info = zipf.getinfo('fabric.mod.json')
            if not 0 < info.file_size <= 64 * 1024:
                raise reject()
            data = json.loads(zipf.read('fabric.mod.json'))
            if data.get('id') != 'maestroai' or data.get('version') != version:
                raise reject()
    except (OSError, zipfile.BadZipFile, KeyError, UnicodeError,
            ValueError, RuntimeError) as e:
        raise reject() from None
    return digest


class ReleaseGateRegression(unittest.TestCase):
    version = '1.0.94'

    def make(self, directory):
        jar = directory / f'maestroai-{self.version}.jar'
        with zipfile.ZipFile(jar, 'w') as z:
            z.writestr('fabric.mod.json', json.dumps(
                {'id':'maestroai','version':self.version}))
            z.writestr('com/maestroai/MaestroAI.class',
                       b'\xca\xfe\xba\xbe\x00\x00\x00A')
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        (directory / 'sha256.txt').write_text(
            f'{digest}  {jar.name}\n', encoding='ascii')
        return jar, digest

    def test_exact_candidate(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            _, digest = self.make(directory)
            self.assertEqual(validate(directory, self.version), digest)

    def test_tampered_byte_or_stale_digest(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            jar,_ = self.make(directory)
            with jar.open('ab') as output: output.write(b'untrusted')
            with self.assertRaises(ValueError): validate(directory, self.version)

    def test_wrong_version_and_extra_files(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            self.make(directory)
            with self.assertRaises(ValueError): validate(directory, '1.0.93')
            (directory / 'private-replay.log').write_text('forbidden')
            with self.assertRaises(ValueError): validate(directory, self.version)

    def test_duplicate_zip_entries(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            jar,_ = self.make(directory)
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(jar, 'a') as z:
                    z.writestr('fabric.mod.json', '{}')
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            (directory / 'sha256.txt').write_text(
                f'{digest}  {jar.name}\n')
            with self.assertRaises(ValueError): validate(directory, self.version)

    def test_bad_checksum_and_traversal_name(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            self.make(directory)
            (directory / 'sha256.txt').write_text('f'*64+'  ../other.jar\n')
            with self.assertRaises(ValueError): validate(directory, self.version)

    def test_symlink_candidate(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)
            jar,_ = self.make(directory)
            jar.unlink()
            try: jar.symlink_to(directory / 'sha256.txt')
            except (OSError, NotImplementedError): return
            with self.assertRaises(ValueError): validate(directory, self.version)


def main(argv):
    if argv == ['--self-test']:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(ReleaseGateRegression)
        return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1
    if len(argv) != 1:
        print('VerifiedRelease[verdict=ABSTAIN]', file=sys.stderr); return 2
    try:
        version = project_version(Path.cwd())
        digest = validate(Path(argv[0]), version)
        print('VerifiedRelease[verdict=CANDIDATE_BYTES_MATCH;sha256=' + digest
              + ';sourceCommit=GITHUB_RUN_PIN_REQUIRED;simulator=EXTERNAL_JOB_GATE]')
        return 0
    except (ValueError, OSError, RuntimeError):
        print('VerifiedRelease[verdict=ABSTAIN]', file=sys.stderr); return 2


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
