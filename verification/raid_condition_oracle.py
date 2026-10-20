"""External, whole-JAR-pinned Raid Dens public condition fixtures only.

The upstream expressions stay in temporary verification input, never our JAR.
"""
import hashlib
import io
import urllib.request
import zipfile

URL = ('https://cursemaven.com/curse/maven/cobblemonraiddens-1349231/'
       '8358837/cobblemonraiddens-1349231-8358837.jar')
SHA256 = '0a09f8ae544412e2f0f599a38b033928ec0a553ad53506c10bca3db734877c7b'
PREFIX = 'data/cobblemonraiddens/cobblemonraiddens/showdown/conditions/'
IDS = {'cheerattack', 'cheerdefense', 'raidboss', 'shield'}


def raid_sources(home, resources):
    target = home / 'caches/maestro-verification' / (SHA256 + '.jar')
    if not target.exists():
        with urllib.request.urlopen(URL, timeout=60) as response:
            data = response.read(16 * 1024 * 1024 + 1)
        if hashlib.sha256(data).hexdigest() != SHA256:
            raise ValueError('Public raid addon checksum mismatch')
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    data = target.read_bytes()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise ValueError('Cached public raid addon checksum mismatch')
    result, wires = {}, {}
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        class_pins = dict(line.split('\t') for line in
                          (resources / 'runtime-pack-raid-classes.tsv').read_text().splitlines())
        if len(class_pins) != 2 or any(hashlib.sha256(archive.read(name)).hexdigest() != digest
                                     for name, digest in class_pins.items()):
            raise ValueError('Public raid membership API provenance differs')
        for name in archive.namelist():
            if not name.startswith(PREFIX) or not name.endswith('.js'):
                continue
            identifier = name[len(PREFIX):-3]
            if identifier not in IDS or identifier in result:
                raise ValueError('Unexpected public raid condition')
            text = archive.read(name).decode('utf-8')
            # Original StatusEffectsReloadImpl uses BufferedReader.lines joined
            # with LF. StatusEffectsReloadListener forwards it WITHOUT flattening.
            wire = '\n'.join(text.replace('\r\n', '\n').replace('\r', '\n').splitlines())
            result[identifier] = text
            wires['condition/' + identifier] = hashlib.sha256(wire.encode()).hexdigest()
    expected = dict(line.split('\t') for line in
                    (resources / 'runtime-pack-raid-wires.tsv').read_text().splitlines())
    if result.keys() != IDS or wires != expected:
        raise ValueError('Public raid condition inventory/wires differ')
    print('Pinned raid addon PASS: original complete JAR, four public conditions and native LF transport')
    return result


def pack_native_sources(home, resources):
    from pack_loader_oracle import installer_jar, PACK_JAR_SHA256
    result, hashes = {}, {}
    sources = [(installer_jar(home), PACK_JAR_SHA256),
               (home / 'caches/maestro-verification' / (SHA256 + '.jar'), SHA256)]
    for path, expected in sources:
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError('Native pack source checksum mismatch')
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            for name in archive.namelist():
                if not name.startswith('data/cobblemon/bag_items/') or not name.endswith('.js'):
                    continue
                identifier = name.split('/')[-1][:-3]
                if identifier in result:
                    raise ValueError('Duplicate native pack initializer')
                text = archive.read(name).decode('utf-8')
                # Original BagItems.reload uses readText: preserve CRLF here.
                result[identifier] = text
                hashes['bagItem/' + identifier] = hashlib.sha256(text.encode()).hexdigest()
    expected = dict(line.split('\t') for line in
                    (resources / 'runtime-pack-native-factories.tsv').read_text().splitlines())
    if len(result) != 5 or hashes != expected:
        raise ValueError('Native pack initializer inventory/bytes differ')
    print('Pinned native pack inputs PASS: five original bag factories from two verified artifacts')
    return result
