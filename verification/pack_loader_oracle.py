#!/usr/bin/env python3
"""Execute one hash-pinned public installer in a disposable source-only host.

Only configuration and logging are substituted. The installer class and all
copied assets are the published bytes, not a reimplementation of its behavior.
No Minecraft, user installation, battle payload or private log is accessed.
"""
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import urllib.request
import zipfile
import unittest

PACK_JAR_URL = ('https://mediafilez.forgecdn.net/files/8102/864/'
                'mega_showdown-fabric-1.8.4%2B1.7.3%2B1.21.1.jar')
PACK_JAR_SHA256 = '0cf4568128805816a8d6f3cbab08374b2e0261784c3d6e8c091c602f79bebf51'
LOADER_SHA256 = 'd8575007d2a330098c5365ba28944b7eae36c0d0f7e90e8b3ce8c76ab0769076'
MARKER = b'{"version":"1.0.0"}'
MARKER_SHA256 = '2afa0f3c420ac37f226ceed715865e390c67593793f413018af33d8a79f56b9f'


def metadata_pins(resources):
    expected = 'MSDPatch.json\t' + MARKER_SHA256 + '\n'
    if (resources / 'runtime-pack-metadata.tsv').read_text() != expected:
        raise ValueError('Installer metadata pin differs')
    return {'MSDPatch.json': MARKER}


def cached_library(home, group, artifact, version):
    choices = list((home / 'caches/modules-2/files-2.1' / group / artifact / version).glob('*/*.jar'))
    if len(choices) != 1:
        raise ValueError('Expected one build-resolved installer host library')
    return choices[0]


def installer_jar(home):
    target = home / 'caches/maestro-verification' / (PACK_JAR_SHA256 + '.jar')
    if not target.exists():
        with urllib.request.urlopen(PACK_JAR_URL, timeout=60) as response:
            data = response.read(32 * 1024 * 1024 + 1)
        if hashlib.sha256(data).hexdigest() != PACK_JAR_SHA256:
            raise ValueError('Public installer checksum mismatch')
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    if hashlib.sha256(target.read_bytes()).hexdigest() != PACK_JAR_SHA256:
        raise ValueError('Cached public installer checksum mismatch')
    return target


def run_installer(home, simulator, assets, resources):
    """Use the real installer once, then exercise its already-current fast path."""
    target = installer_jar(home)
    expected = assets | metadata_pins(resources)
    with zipfile.ZipFile(target) as archive:
        if hashlib.sha256(archive.read('com/github/yajatkaul/mega_showdown/utils/LoadShowdown.class')).hexdigest() != LOADER_SHA256:
            raise ValueError('Public installer class differs')
        for name, source in assets.items():
            asset = ('pokemon.js' if name == 'sim/pokemons.js' else
                     ('mods/' if name.startswith('data/mods/') else '') + Path(name).name)
            if archive.read('assets/mega_showdown/showdown/' + asset) != source.replace(b'\n', b'\r\n'):
                raise ValueError('Published installer asset differs from pinned revision')
    # The real load() uses ./showdown. Never change the caller's working directory.
    if simulator.name != 'showdown':
        raise ValueError('Disposable installer source root must be named showdown')
    before = {p.relative_to(simulator).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in simulator.rglob('*') if p.is_file()}
    with tempfile.TemporaryDirectory(prefix='maestro-installer-host-') as folder:
        host = Path(folder)
        sources = {
            'com/github/yajatkaul/mega_showdown/config/MegaShowdownConfig.java':
                'package com.github.yajatkaul.mega_showdown.config; public class MegaShowdownConfig { public static boolean msdPatchAutoUpdate=true; }',
            'com/github/yajatkaul/mega_showdown/MegaShowdown.java':
                'package com.github.yajatkaul.mega_showdown; public class MegaShowdown { public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("source-verification"); }',
            'InstallerHost.java': '''
                import com.github.yajatkaul.mega_showdown.utils.LoadShowdown;
                import java.nio.file.*;
                public class InstallerHost { public static void main(String[] args) throws Exception {
                    LoadShowdown.load();
                    Path file=Path.of("showdown/index.js"); byte[] saved=Files.readAllBytes(file);
                    byte[] sentinel="source-only fast-path sentinel".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    Files.write(file,sentinel); LoadShowdown.load();
                    if(!java.util.Arrays.equals(sentinel,Files.readAllBytes(file))) throw new AssertionError("current installer overwrote source");
                    Files.write(file,saved);
                }}''',
        }
        for name, source in sources.items():
            path = host / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source)
        libraries = [cached_library(home, 'com.google.code.gson', 'gson', '2.10.1'),
                     cached_library(home, 'org.slf4j', 'slf4j-api', '2.0.9')]
        cp = os.pathsep.join(map(str, [host, target] + libraries))
        subprocess.run(['javac', '-cp', cp, '-d', str(host)] + [str(host / n) for n in sources], check=True)
        subprocess.run(['java', '-cp', cp, 'InstallerHost'], cwd=simulator.parent, check=True)
    for name, source in expected.items():
        installed = (simulator / name).read_bytes()
        if installed != (source if name.endswith('.json') else source.replace(b'\n', b'\r\n')):
            raise ValueError('Actual installer output differs')
    after = {p.relative_to(simulator).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in simulator.rglob('*') if p.is_file()}
    changed = {name for name in before.keys() | after.keys() if before.get(name) != after.get(name)}
    if changed != set(expected):
        raise ValueError('Actual installer changed unexpected source files')
    print('Published installer PASS: 13 exact assets, exact version marker and current-version fast path; source-only host')
    return item_fixtures(target, resources)


def item_fixtures(target, resources):
    """Original public post-boot scripts; verify the separate native reload API."""
    with zipfile.ZipFile(target) as archive:
        code=archive.read('com/github/yajatkaul/mega_showdown/fabric/datapack/showdown/HeldItems.class')
        if hashlib.sha256(code).hexdigest()!='1d52078cae0964292f2f872507aa39112c08ee6a4afe1d08ad4aacd150cdcf5b':
            raise ValueError('Public item reload implementation differs')
        prefix='data/mega_showdown/mega_showdown/showdown/held_items/'
        scripts={Path(n).stem:archive.read(n).decode('utf-8') for n in archive.namelist()
                 if n.startswith(prefix) and n.endswith('.js')}
    if set(scripts)!={'eviolite','legendplate','pikaniumz','pikashuniumz'}:
        raise ValueError('Public post-boot item factory inventory differs')
    actual={k:hashlib.sha256(' '.join(v.splitlines()).encode()).hexdigest() for k,v in scripts.items()}
    expected=dict(line.split('\t') for line in (resources/'runtime-pack-item-wires.tsv').read_text().splitlines())
    if actual!=expected: raise ValueError('Native BufferedReader/newline wire pins differ')
    return scripts



class InstallerGuards(unittest.TestCase):
    def test_generated_metadata_has_exact_pin(self):
        self.assertEqual(hashlib.sha256(MARKER).hexdigest(), MARKER_SHA256)
        self.assertEqual(metadata_pins(Path('src/main/resources/com/maestroai/training')), {'MSDPatch.json': MARKER})

    def test_metadata_pin_cannot_broaden_or_change(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for text in ['', 'MSDPatch.json\t' + '0'*64 + '\n',
                         'private.json\t' + MARKER_SHA256 + '\n',
                         'MSDPatch.json\t' + MARKER_SHA256 + '\nextra.json\t' + MARKER_SHA256 + '\n']:
                (root / 'runtime-pack-metadata.tsv').write_text(text)
                with self.assertRaises(ValueError): metadata_pins(root)

    def test_cached_installer_is_hash_checked_before_execution(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            target = root / 'caches/maestro-verification' / (PACK_JAR_SHA256 + '.jar')
            target.parent.mkdir(parents=True)
            target.write_bytes(b'not a trusted installer')
            with self.assertRaises(ValueError): installer_jar(root)
