"""Obtain only the published, whole-JAR-pinned public addon factory fixtures.

No addon expression is shipped in the mod or synthesized from production hashes.
"""
import hashlib
import io
import urllib.request
import zipfile

URL = 'https://mediafilez.forgecdn.net/files/7961/235/zamega-fabric-1.7.3.jar'
SHA256 = '0d134591a9db9834f5f2bffdce4bd4cd0976a1b7879aa7f52ce064c38199cfaa'
PREFIX = 'data/zamega/mega_showdown/showdown/'
GROUPS = {'scripts': 'script', 'held_items': 'heldItem', 'abilities': 'ability',
          'conditions': 'condition', 'moves': 'move'}


def addon_sources(home, resources):
    target = home / 'caches/maestro-verification' / (SHA256 + '.jar')
    if not target.exists():
        with urllib.request.urlopen(URL, timeout=60) as response:
            data = response.read(8 * 1024 * 1024 + 1)
        if hashlib.sha256(data).hexdigest() != SHA256:
            raise ValueError('Public addon checksum mismatch')
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    data = target.read_bytes()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise ValueError('Cached public addon checksum mismatch')
    result, wires = {}, {}
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for name in archive.namelist():
            if not name.startswith(PREFIX) or not name.endswith('.js'):
                continue
            group, filename = name[len(PREFIX):].split('/')
            key = GROUPS[group] + '/' + filename[:-3]
            if key in result:
                raise ValueError('Duplicate public factory')
            text = archive.read(name).decode('utf-8')
            # BufferedReader.lines() handles CR, LF and CRLF and drops the
            # terminal delimiter. Preserve every other character exactly.
            wire = ' '.join(text.replace('\r\n', '\n').replace('\r', '\n').splitlines())
            result[key] = text
            wires[key] = hashlib.sha256(wire.encode()).hexdigest()
    expected = dict(line.split('\t') for line in
                    (resources / 'runtime-pack-definition-wires.tsv').read_text().splitlines())
    if len(result) != 58 or wires != expected:
        raise ValueError('Public addon factory inventory/wires differ')
    print('Pinned public addon PASS: whole original JAR, 58 original public factories and exact native wire hashes')
    return result
