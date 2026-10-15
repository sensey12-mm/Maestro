#!/usr/bin/env python3
"""Check the built mod's packaging using only repository-local inputs and stdlib."""
import hashlib
import io
import json
from pathlib import Path
import struct
import sys
import unittest
import warnings
import zipfile


def inspect(data, classes, resources, version, notice):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or any(n.startswith('/') or '\\' in n or '..' in n.split('/') for n in names):
            raise ValueError('Duplicate or unsafe ZIP entry')
        files = {n for n in names if not n.endswith('/')}
        if any(not any(f.startswith(n) for f in files) for n in names if n.endswith('/')):
            raise ValueError('Unexpected empty directory')
        actual = {n for n in files if n.endswith('.class')}
        if not classes or actual != classes:
            raise ValueError('Missing or extra compiled class')
        for name in classes:
            content = archive.read(name)
            if len(content) < 8 or content[:4] != b'\xca\xfe\xba\xbe' or struct.unpack('>H', content[6:8])[0] != 65:
                raise ValueError('Class is not Java 21')
        allowed = classes | set(resources) | {'META-INF/MANIFEST.MF', 'LICENSE_maestroai', 'maestroai-refmap.json'}
        required = set(resources) | {'META-INF/MANIFEST.MF', 'LICENSE_maestroai', 'maestroai-refmap.json'}
        if files - allowed or not required <= files:
            raise ValueError('Extra or missing packaged resource')
        if archive.read('LICENSE_maestroai') != notice:
            raise ValueError('Original notice missing or altered')
        for name, content in resources.items():
            actual_content = archive.read(name)
            if name == 'fabric.mod.json':
                expected = json.loads(content); expected['version'] = version
                if json.loads(actual_content) != expected:
                    raise ValueError('Unexpected mod metadata')
            elif name == 'maestroai.mixins.json':
                expected = json.loads(content); expected['refmap'] = 'maestroai-refmap.json'
                if json.loads(actual_content) != expected:
                    raise ValueError('Unexpected mixin metadata')
            elif actual_content != content:
                raise ValueError('Packaged resource bytes changed')
        if json.loads(archive.read('maestroai-refmap.json')) != {'mappings': {}, 'data': {}}:
            raise ValueError('Unexpected generated reference map')
        return len(classes), len(files)


class PackagingRegression(unittest.TestCase):
    def fixture(self, **changes):
        resources = {'fabric.mod.json': b'{"id":"maestroai","version":"${version}"}',
                     'maestroai.mixins.json': b'{"required":true}', 'synthetic.tsv': b'a\t1\n'}
        files = {'com/maestroai/Synthetic.class': b'\xca\xfe\xba\xbe\0\0\0A',
                 'fabric.mod.json': b'{"id":"maestroai","version":"1"}',
                 'maestroai.mixins.json': b'{"required":true,"refmap":"maestroai-refmap.json"}',
                 'maestroai-refmap.json': b'{"mappings":{},"data":{}}',
                 'META-INF/MANIFEST.MF': b'Manifest-Version: 1.0\n\n',
                 'LICENSE_maestroai': b'synthetic notice', 'synthetic.tsv': resources['synthetic.tsv']}
        files.update(changes)
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as archive:
            for name, content in files.items(): archive.writestr(name, content)
        return stream.getvalue(), {'com/maestroai/Synthetic.class'}, resources, '1', b'synthetic notice'

    def test_valid(self): self.assertEqual(inspect(*self.fixture()), (1, 7))
    def test_old_java(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'com/maestroai/Synthetic.class': b'\xca\xfe\xba\xbe\0\0\0='}))
    def test_extra_payload(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'unexpected.txt': b'not allowed'}))
    def test_traversal(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'../unexpected.txt': b'not allowed'}))
    def test_changed_resource(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'synthetic.tsv': b'altered'}))
    def test_changed_notice(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'LICENSE_maestroai': b'altered'}))
    def test_duplicate_entry(self):
        args = list(self.fixture()); stream = io.BytesIO(args[0])
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(stream, 'a') as archive: archive.writestr('synthetic.tsv', b'altered')
        args[0] = stream.getvalue()
        with self.assertRaises(ValueError): inspect(*args)
    def test_unexpected_reference_map(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'maestroai-refmap.json': b'{"mappings":{"unexpected":"value"},"data":{}}'}))
    def test_changed_metadata(self):
        with self.assertRaises(ValueError): inspect(*self.fixture(**{'fabric.mod.json': b'{"id":"unexpected","version":"1"}'}))
    def without(self, name):
        args = list(self.fixture()); stream = io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(args[0])) as original, zipfile.ZipFile(stream, 'w') as output:
            for entry in original.namelist():
                if entry != name: output.writestr(entry, original.read(entry))
        args[0] = stream.getvalue()
        return args

    def test_missing_manifest(self):
        with self.assertRaises(ValueError): inspect(*self.without('META-INF/MANIFEST.MF'))
    def test_missing_notice(self):
        with self.assertRaises(ValueError): inspect(*self.without('LICENSE_maestroai'))



def main():
    if sys.argv[1:] == ['--self-test']:
        result = unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(PackagingRegression))
        return 0 if result.wasSuccessful() else 1
    root = Path.cwd()
    classes = {p.relative_to(root / 'build/classes/java/main').as_posix()
               for p in (root / 'build/classes/java/main').rglob('*.class')}
    resources = {p.relative_to(root / 'src/main/resources').as_posix(): p.read_bytes()
                 for p in (root / 'src/main/resources').rglob('*') if p.is_file()}
    version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('version='))
    data = Path(sys.argv[1]).read_bytes()
    class_count, entries = inspect(data, classes, resources, version, (root / 'LICENSE').read_bytes())
    print(json.dumps({'jar_sha256': hashlib.sha256(data).hexdigest(), 'java21_classes': class_count,
                      'jar_files': entries, 'standalone_tests_packaged': False}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
