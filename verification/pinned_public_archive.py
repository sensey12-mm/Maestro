"""Strict, reusable acquisition of hash-pinned public verification archives.

Only the transport/cache plumbing is shared. Each caller still independently
verifies its own archive membership, Java classes, factory wires, and behavior.
"""
import hashlib
import io
import os
from pathlib import Path
import re
import tempfile
import unittest
from unittest import mock
import urllib.request


def load_pinned_public_jar(home, url, sha256, maximum, label):
    """Return (cached_path, exact_bytes) or reject without trusting/extracting data.

    Failed or tampered caches are never silently replaced by network content.
    Download size is bounded before hashing and cache writes are atomic.
    """
    if (not isinstance(home, Path) or not isinstance(url, str)
            or not url.startswith('https://')
            or re.fullmatch('[0-9a-f]{64}', sha256) is None
            or not isinstance(maximum, int) or maximum <= 0
            or label not in ('addon', 'raid addon', 'installer')):
        raise ValueError('Invalid pinned public archive specification')
    directory = home / 'caches/maestro-verification'
    target = directory / (sha256 + '.jar')
    if directory.is_symlink() or target.is_symlink():
        raise ValueError('Untrusted public archive cache symlink')
    if target.exists():
        if not target.is_file() or target.stat().st_size > maximum:
            raise ValueError('Cached public ' + label + ' size/type mismatch')
        data = target.read_bytes()
        if len(data) > maximum or hashlib.sha256(data).hexdigest() != sha256:
            raise ValueError('Cached public ' + label + ' checksum mismatch')
        return target, data

    with urllib.request.urlopen(url, timeout=60) as response:
        data = response.read(maximum + 1)
    if len(data) > maximum or hashlib.sha256(data).hexdigest() != sha256:
        raise ValueError('Public ' + label + ' checksum mismatch')
    directory.mkdir(parents=True, exist_ok=True)
    if directory.is_symlink() or target.is_symlink():
        raise ValueError('Untrusted public archive cache symlink')
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(dir=directory, prefix='.archive-', suffix='.tmp', delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(data)
        os.replace(temporary, target)
        temporary = None
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    # Return read-back content rather than assuming the cache remained intact.
    if target.is_symlink() or not target.is_file() or target.stat().st_size > maximum:
        raise ValueError('Untrusted public archive cache entry')
    cached = target.read_bytes()
    if hashlib.sha256(cached).hexdigest() != sha256:
        raise ValueError('Cached public ' + label + ' checksum mismatch')
    return target, cached


class PinnedPublicArchiveGuards(unittest.TestCase):
    DATA = b'public synthetic archive bytes\n'

    def pin(self):
        return hashlib.sha256(self.DATA).hexdigest()

    def acquire(self, home, maximum=1024):
        return load_pinned_public_jar(home, 'https://example.invalid/archive.jar',
                                      self.pin(), maximum, 'addon')

    def test_download_and_cached_reuse_exact_bytes_without_second_request(self):
        with tempfile.TemporaryDirectory() as folder:
            home = Path(folder)
            with mock.patch('pinned_public_archive.urllib.request.urlopen',
                            return_value=io.BytesIO(self.DATA)) as network:
                target, contents = self.acquire(home)
                self.assertEqual(contents, self.DATA)
                self.assertEqual(target.read_bytes(), self.DATA)
                self.assertEqual(target.name, self.pin() + '.jar')
                self.assertEqual(network.call_count, 1)
                self.assertEqual(self.acquire(home), (target, self.DATA))
                self.assertEqual(network.call_count, 1)
                self.assertEqual(list(target.parent.glob('*.tmp')), [])

    def test_bad_download_and_oversized_download_leave_no_cache(self):
        for changed, limit in ((b'forged', 1024), (self.DATA, 3)):
            with self.subTest(maximum=limit), tempfile.TemporaryDirectory() as folder:
                home = Path(folder)
                with mock.patch('pinned_public_archive.urllib.request.urlopen',
                                return_value=io.BytesIO(changed)):
                    with self.assertRaises(ValueError):
                        self.acquire(home, maximum=limit)
                target = home / 'caches/maestro-verification' / (self.pin() + '.jar')
                self.assertFalse(target.exists())

    def test_tampered_cached_bytes_fail_closed_without_network_fallback(self):
        with tempfile.TemporaryDirectory() as folder:
            home = Path(folder)
            target = home / 'caches/maestro-verification' / (self.pin() + '.jar')
            target.parent.mkdir(parents=True)
            target.write_bytes(b'corrupt')
            with mock.patch('pinned_public_archive.urllib.request.urlopen') as network:
                with self.assertRaisesRegex(ValueError, 'Cached public addon checksum'):
                    self.acquire(home)
                network.assert_not_called()
            target.write_bytes(self.DATA)
            with self.assertRaisesRegex(ValueError, 'size/type'):
                self.acquire(home, maximum=len(self.DATA) - 1)

    def test_all_three_independent_callers_keep_their_pins_and_limits(self):
        from addon_reload_oracle import URL as addon_url, SHA256 as addon_sha, addon_sources
        from raid_condition_oracle import URL as raid_url, SHA256 as raid_sha, raid_sources
        from pack_loader_oracle import PACK_JAR_URL, PACK_JAR_SHA256, installer_jar
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            resources = root / 'resources'
            resources.mkdir()
            (resources / 'runtime-pack-definition-wires.tsv').write_text('')
            first = io.BytesIO()
            with __import__('zipfile').ZipFile(first, 'w') as archive:
                archive.writestr('empty.txt', 'nothing')
            with mock.patch('addon_reload_oracle.load_pinned_public_jar',
                            return_value=(root / 'addon.jar', first.getvalue())) as cache:
                with self.assertRaisesRegex(ValueError, 'factory inventory/wires differ'):
                    addon_sources(root, resources)
                cache.assert_called_once_with(root, addon_url, addon_sha, 8 * 1024 * 1024, 'addon')

            (resources / 'runtime-pack-raid-classes.tsv').write_text(
                'first.class\t' + hashlib.sha256(b'a').hexdigest() + '\n'
                + 'second.class\t' + hashlib.sha256(b'b').hexdigest() + '\n')
            (resources / 'runtime-pack-raid-wires.tsv').write_text('')
            second = io.BytesIO()
            with __import__('zipfile').ZipFile(second, 'w') as archive:
                archive.writestr('first.class', b'a')
                archive.writestr('second.class', b'b')
            with mock.patch('raid_condition_oracle.load_pinned_public_jar',
                            return_value=(root / 'raid.jar', second.getvalue())) as cache:
                with self.assertRaisesRegex(ValueError, 'condition inventory/wires differ'):
                    raid_sources(root, resources)
                cache.assert_called_once_with(root, raid_url, raid_sha, 16 * 1024 * 1024, 'raid addon')

            expected = root / 'installer.jar'
            with mock.patch('pack_loader_oracle.load_pinned_public_jar',
                            return_value=(expected, b'dummy')) as cache:
                self.assertEqual(installer_jar(root), expected)
                cache.assert_called_once_with(root, PACK_JAR_URL, PACK_JAR_SHA256,
                                              32 * 1024 * 1024, 'installer')

    def test_symlink_and_invalid_pin_are_rejected_without_network(self):
        with tempfile.TemporaryDirectory() as folder:
            home = Path(folder)
            target = home / 'caches/maestro-verification' / (self.pin() + '.jar')
            target.parent.mkdir(parents=True)
            target.symlink_to(home / 'untrusted')
            with mock.patch('pinned_public_archive.urllib.request.urlopen') as network:
                with self.assertRaisesRegex(ValueError, 'symlink'):
                    self.acquire(home)
                network.assert_not_called()
            with self.assertRaisesRegex(ValueError, 'specification'):
                load_pinned_public_jar(home, 'http://untrusted', '0' * 64, 5, 'addon')
            with self.assertRaisesRegex(ValueError, 'specification'):
                load_pinned_public_jar(home, 'https://example.invalid', 'bad', 5, 'addon')
