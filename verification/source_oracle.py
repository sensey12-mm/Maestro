#!/usr/bin/env python3
"""Run source checks with pinned external bytes and temporary extraction only."""
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import stat
import subprocess
import sys
import tempfile
import unittest
import zipfile

DEPENDENCY_SHA256 = 'f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3'
SIMULATOR_SHA256 = '1c398031f28fefcb2ce6b44a5ca289061ed4c5d0dccefaae0f7c070b3f7e8d10'


def checked_bytes(data, expected):
    if hashlib.sha256(data).hexdigest() != expected:
        raise ValueError('External source checksum mismatch')
    return data


def safe_entries(archive, limit=256 * 1024 * 1024):
    entries = archive.infolist()
    if len(entries) > 20000 or sum(e.file_size for e in entries) > limit:
        raise ValueError('Source archive exceeds bounds')
    paths = set()
    for entry in entries:
        name = entry.filename
        normalized = PurePosixPath(name).as_posix().rstrip('/')
        if (not name or name.startswith('/') or '\\' in name or ':' in name
                or '\0' in name or any(p in ('..', '.') for p in name.split('/'))
                or normalized in paths or stat.S_ISLNK(entry.external_attr >> 16)):
            raise ValueError('Unsafe or duplicate source path')
        paths.add(normalized)
    return entries


def cache_dependency(home):
    choices = list((home / 'caches/modules-2/files-2.1/curse.maven/cobblemon-687131/7553235').glob('*/*.jar'))
    if len(choices) != 1:
        raise ValueError('Expected one exact cached Cobblemon dependency after build')
    return choices[0]


def verify_public_inputs(dependency, resources):
    """Independent native bytes and obtainable fixture facts, never private sets."""
    with zipfile.ZipFile(io.BytesIO(dependency)) as archive:
        pins = dict(line.split('\t') for line in
                    (resources / 'runtime-native-factories.tsv').read_text().splitlines())
        expected = {('bagItem' if '/bag_items/' in n else 'heldItem') + '/' + Path(n).stem: n
                    for n in archive.namelist()
                    if n.startswith(('data/cobblemon/bag_items/', 'data/cobblemon/held_items/'))
                    and n.endswith('.js')}
        if len(expected) != 12 or set(pins) != set(expected):
            raise ValueError('Native factory inventory differs')
        for key, name in expected.items(): checked_bytes(archive.read(name), pins[key])
        fixtures = {
            'weavile': ('pressure', {'shadowclaw', 'swordsdance'}),
            'mewtwo': ('pressure', {'psychocut', 'bulkup', 'aurasphere', 'nastyplot'}),
            'skarmory': ('sturdy', {'spikes', 'stealthrock'}),
            'wobbuffet': ('shadowtag', {'splash', 'counter', 'mirrorcoat'}),
            'ditto': ('imposter', {'transform'}),
            'porygonz': ('adaptability', {'hyperbeam'}),
            'blissey': ('naturalcure', {'softboiled', 'growl', 'seismictoss'}),
        }
        for species, (ability, moves) in fixtures.items():
            paths = [n for n in archive.namelist() if n.startswith('data/cobblemon/species/')
                     and n.endswith('/' + species + '.json')]
            if len(paths) != 1: raise ValueError('Ambiguous native fixture species')
            data = json.loads(archive.read(paths[0]))
            native_moves = {move.split(':')[-1] for move in data['moves']}
            if ability not in {a.removeprefix('h:') for a in data['abilities']} or not moves <= native_moves:
                raise ValueError('Fixture ability/learnset is not native')
    print('Pinned native public inputs PASS: 12 factory byte hashes and seven natural fixture learnsets/abilities')


class SourceGuards(unittest.TestCase):
    def archive(self, names, symlink=False):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as output:
            for name in names:
                entry = zipfile.ZipInfo(name)
                if symlink: entry.external_attr = (stat.S_IFLNK | 0o777) << 16
                output.writestr(entry, b'synthetic')
        return zipfile.ZipFile(io.BytesIO(stream.getvalue()))

    def test_valid(self):
        with self.archive(['sim/', 'sim/public.js']) as archive:
            self.assertEqual(len(safe_entries(archive)), 2)

    def test_traversal(self):
        with self.archive(['../outside.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_absolute(self):
        with self.archive(['/outside.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_backslash(self):
        with self.archive(['sim\\outside.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_drive(self):
        with self.archive(['C:/outside.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_dot_alias(self):
        with self.archive(['sim/./a.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_normalized_duplicate(self):
        with self.archive(['sim/a.js', 'sim//a.js']) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_symlink(self):
        with self.archive(['link'], True) as archive, self.assertRaises(ValueError): safe_entries(archive)

    def test_size_bound(self):
        with self.archive(['a.js']) as archive, self.assertRaises(ValueError): safe_entries(archive, limit=1)

    def test_wrong_source(self):
        with self.assertRaises(ValueError): checked_bytes(b'synthetic', '0' * 64)

    def test_right_source(self):
        self.assertEqual(checked_bytes(b'synthetic', hashlib.sha256(b'synthetic').hexdigest()), b'synthetic')

    def test_missing_dependency(self):
        with tempfile.TemporaryDirectory() as folder, self.assertRaises(ValueError): cache_dependency(Path(folder))

    def test_ambiguous_dependency(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder) / 'caches/modules-2/files-2.1/curse.maven/cobblemon-687131/7553235'
            for name in ('a', 'b'):
                (root / name).mkdir(parents=True); (root / name / 'library.jar').write_bytes(b'fake')
            with self.assertRaises(ValueError): cache_dependency(Path(folder))


def main():
    if sys.argv[1:] == ['--self-test']:
        from runtime_hook_oracle import HookOracleRegression
        suite = unittest.TestSuite((
            unittest.defaultTestLoader.loadTestsFromTestCase(SourceGuards),
            unittest.defaultTestLoader.loadTestsFromTestCase(HookOracleRegression),
        ))
        return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1
    if sys.argv[1:]: raise ValueError('Unknown oracle argument')
    home = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')))
    dependency = checked_bytes(cache_dependency(home).read_bytes(), DEPENDENCY_SHA256)
    from runtime_hook_oracle import verify_pinned_hooks
    verify_pinned_hooks(dependency)
    verify_public_inputs(dependency, Path("src/main/resources/com/maestroai/training"))
    with zipfile.ZipFile(io.BytesIO(dependency)) as archive:
        simulator = checked_bytes(archive.read('data/cobblemon/showdown.zip'), SIMULATOR_SHA256)
    with tempfile.TemporaryDirectory(prefix='maestroai-source-') as folder:
        with zipfile.ZipFile(io.BytesIO(simulator)) as archive:
            safe_entries(archive)
            archive.extractall(folder)
        subprocess.run(['node', 'verification/source_oracle.cjs', folder], check=True)
        subprocess.run(['bash', 'gradlew', '--no-daemon', '-I', 'verification/verification.init.gradle',
                        'sourceVerification', '-PverificationSource=' + folder, '--stacktrace'], check=True)
    print('Pinned external source checks PASS; temporary source tree removed; no Minecraft acceptance claimed')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
