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
import urllib.request
import re
import zipfile

DEPENDENCY_SHA256 = 'f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3'
PACK_REVISION = '2a6d0efb8a45652769c4fdecb8c6e26bbd7aabc9'
PACK_BASE = 'https://raw.githubusercontent.com/yajatkaul/CobblemonMegaShowdown/' + PACK_REVISION + '/common/src/main/resources/assets/mega_showdown/showdown/'

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
            'mewtwo': ('pressure', {'psychocut', 'swift', 'bulkup', 'aurasphere', 'nastyplot', 'calmmind', 'recover'}),
            'skarmory': ('sturdy', {'spikes', 'stealthrock', 'roost', 'whirlwind'}),
            'wobbuffet': ('shadowtag', {'splash', 'encore', 'counter', 'mirrorcoat'}),
            'ditto': ('imposter', {'transform'}),
            'porygonz': ('adaptability', {'hyperbeam'}),
            'blissey': ('naturalcure', {'softboiled', 'growl', 'seismictoss'}),
            'mew': ('synchronize', {'transform','knockoff','roost','haze'}),
        }
        for species, (ability, moves) in fixtures.items():
            paths = [n for n in archive.namelist() if n.startswith('data/cobblemon/species/')
                     and n.endswith('/' + species + '.json')]
            if len(paths) != 1: raise ValueError('Ambiguous native fixture species')
            data = json.loads(archive.read(paths[0]))
            native_moves = {move.split(':')[-1] for move in data['moves']}
            if ability not in {a.removeprefix('h:') for a in data['abilities']} or not moves <= native_moves:
                raise ValueError('Fixture ability/learnset is not native')
        zoroark = json.loads(archive.read('data/cobblemon/species/generation5/zoroark.json'))
        hisui = next(f for f in zoroark['forms'] if f['name'] == 'Hisui')
        if not {'nastyplot','shadowball','flamethrower','hypervoice'} <= {m.split(':')[-1] for m in hisui['moves']}:
            raise ValueError('Unverified Hisui learnset')
        if json.loads(archive.read('data/cobblemon/species_features/hisuian.json')) != {'keys':['hisuian'],'type':'flag','isAspect':True,'default':False}:
            raise ValueError('Unverified Hisui property')
        for item in ['custap_berry','focus_sash','choice_scarf','sitrus_berry']:
            if not any(n.endswith('/'+item+'.json') for n in archive.namelist()):
                raise ValueError('Player item unavailable')
    print('Pinned native public inputs PASS: 12 factory hashes, eight base learnsets, Hisui form and one-battle player items')


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


PACK_PATHS = frozenset(['data/abilities.js','data/conditions.js','data/moves.js','index.js',
    'sim/battle-actions.js','sim/battle.js','sim/side.js'] + ['data/mods/cobblemon/' + name
    for name in ('items.js','conditions.js','typechart.js','moves.js','abilities.js')])


def pack_manifest(resources):
    tables = []
    for table, expected in [('runtime-pack-files.tsv', PACK_PATHS),
                            ('runtime-pack-optional.tsv', frozenset(['sim/pokemons.js']))]:
        pins = {}
        for line in (resources / table).read_text().splitlines():
            fields = line.split('\t')
            if len(fields) != 3: raise ValueError('Invalid public source pins')
            name, lf, crlf = fields
            if name in pins or not all(re.fullmatch('[a-f0-9]{64}', p) for p in (lf, crlf)):
                raise ValueError('Invalid or duplicate public source pins')
            pins[name] = (lf, crlf)
        if set(pins) != expected: raise ValueError('Public source inventory differs')
        tables.append(pins)
    return tables[0] | tables[1]


def pack_sources(resources):
    result = {}
    for name, (lf, crlf) in pack_manifest(resources).items():
        asset = 'pokemon.js' if name == 'sim/pokemons.js' else ('mods/' if name.startswith('data/mods/') else '') + Path(name).name
        with urllib.request.urlopen(PACK_BASE + asset, timeout=60) as response:
            data = response.read(8 * 1024 * 1024 + 1)
        checked_bytes(data, lf)
        if b'\r' in data: raise ValueError('Unexpected pinned source line layout')
        checked_bytes(data.replace(b'\n', b'\r\n'), crlf)
        result[name] = data
    return result


class PackSourceGuards(unittest.TestCase):
    def test_exact_manifest(self):
        self.assertEqual(len(pack_manifest(Path('src/main/resources/com/maestroai/training'))), 13)

    def test_missing_added_duplicate_or_traversal_pin(self):
        resources = Path('src/main/resources/com/maestroai/training')
        original = (resources / 'runtime-pack-files.tsv').read_text()
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            (folder / 'runtime-pack-optional.tsv').write_text((resources / 'runtime-pack-optional.tsv').read_text())
            for text in [original.split('\n', 1)[1], original + original.splitlines()[0] + '\n',
                         original + '../private.js\t' + '0'*64 + '\t' + '1'*64 + '\n', original.replace('data/abilities.js','data/custom.js')]:
                (folder / 'runtime-pack-files.tsv').write_text(text)
                with self.assertRaises(ValueError): pack_manifest(folder)

    def test_layout_hash_is_not_arbitrary_normalization(self):
        raw = b"const x = 'safe';\n"
        pin = hashlib.sha256(raw.replace(b'\n',b'\r\n')).hexdigest()
        checked_bytes(raw.replace(b'\n',b'\r\n'), pin)
        for changed in [raw, raw + b'\n', raw.replace(b'safe',b'other'), raw.replace(b' ',b'  ')]:
            with self.assertRaises(ValueError): checked_bytes(changed, pin)


def main():
    if sys.argv[1:] == ['--self-test']:
        from runtime_hook_oracle import HookOracleRegression
        from pack_loader_oracle import InstallerGuards
        suite = unittest.TestSuite((
            unittest.defaultTestLoader.loadTestsFromTestCase(SourceGuards),
            unittest.defaultTestLoader.loadTestsFromTestCase(PackSourceGuards),
            unittest.defaultTestLoader.loadTestsFromTestCase(HookOracleRegression),
            unittest.defaultTestLoader.loadTestsFromTestCase(InstallerGuards),
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
        subprocess.run(['node', 'verification/move_attestation_oracle.cjs', folder], check=True)
        subprocess.run(['bash', 'gradlew', '--no-daemon', '-I', 'verification/verification.init.gradle',
                        'sourceVerification', '-PverificationSource=' + folder, '-PverificationDependency=' + str(cache_dependency(home)), '--stacktrace'], check=True)
    assets = pack_sources(Path('src/main/resources/com/maestroai/training'))
    with tempfile.TemporaryDirectory(prefix='maestroai-pack-source-') as folder:
        simulator_root = Path(folder) / 'showdown'
        simulator_root.mkdir()
        with zipfile.ZipFile(io.BytesIO(simulator)) as archive:
            safe_entries(archive); archive.extractall(simulator_root)
        from pack_loader_oracle import run_installer
        scripts=run_installer(home, simulator_root, assets, Path('src/main/resources/com/maestroai/training'))
        (Path(folder)/'pack-item-fixtures.json').write_text(json.dumps(scripts))
        from addon_reload_oracle import addon_sources
        addon=addon_sources(home, Path('src/main/resources/com/maestroai/training'))
        (Path(folder)/'za-definition-fixtures.json').write_text(json.dumps(addon))
        from raid_condition_oracle import raid_sources, pack_native_sources
        raid=raid_sources(home, Path('src/main/resources/com/maestroai/training'))
        (Path(folder)/'raid-condition-fixtures.json').write_text(json.dumps(raid))
        (Path(folder)/'pack-native-fixtures.json').write_text(json.dumps(pack_native_sources(home, Path('src/main/resources/com/maestroai/training'))))
        subprocess.run(['node','verification/addon_reload_oracle.cjs',str(simulator_root)],check=True)
        subprocess.run(['node','verification/raid_condition_oracle.cjs',str(simulator_root)],check=True)
        subprocess.run(['node','verification/pack_item_reload_oracle.cjs',str(simulator_root)],check=True)
        subprocess.run(['node', 'verification/runtime_cache_oracle.cjs', str(simulator_root)], check=True)
        subprocess.run(['node', 'verification/method_provenance_oracle.cjs', str(simulator_root)], check=True)
        subprocess.run(['node', 'verification/source_oracle.cjs', str(simulator_root)], check=True)
        subprocess.run(['bash', 'gradlew', '--no-daemon', '-I', 'verification/verification.init.gradle',
                        'sourceVerification', '-PverificationSource=' + str(simulator_root),
                        '-PverificationDependency=' + str(cache_dependency(home)), '-PverificationPack=true', '--stacktrace'], check=True)
        for name, data in assets.items(): (simulator_root / name).write_bytes(data)
        subprocess.run(['bash', 'gradlew', '--no-daemon', '-I', 'verification/verification.init.gradle',
                        'sourceVerification', '-PverificationSource=' + str(simulator_root),
                        '-PverificationDependency=' + str(cache_dependency(home)), '-PverificationPack=true',
                        '-PverificationLayoutOnly=true', '--stacktrace'], check=True)
    print('Pinned external source checks PASS; temporary source tree removed; no Minecraft acceptance claimed')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
