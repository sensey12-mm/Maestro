#!/usr/bin/env python3
"""Fail-closed verification source, origin and execution-registration checks.

This does not pretend that separately listed manual probes run in CI.
"""
import json
from pathlib import Path
import re
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGIN = '8d6f193fec55554c41b4d16dbfc226d9b88e83ff'
SUFFIX = {'.java', '.cjs', '.py'}
CALL = re.compile(r'\b([A-Z][A-Za-z0-9_]*Regression)\s*\.\s*[A-Za-z_][A-Za-z0-9_]*\s*\(')
NODE = re.compile(r"['\"]verification/([a-z][a-z0-9_]*\.cjs)['\"]")


def require(ok, why):
    if not ok:
        raise ValueError(why)


def load(root):
    data = json.loads((root / 'verification/verification_registry.json').read_text(encoding='utf-8'))
    require(data.get('schema') == 1 and data.get('origin_commit') == ORIGIN,
            'Verification inventory schema or source baseline altered')
    origins = data['original_paths']
    require(len(origins) == 256, 'Original 256-file inventory cannot be silently truncated')
    for name, sha in origins.items():
        require(name.startswith('verification/') and '..' not in Path(name).parts
                and re.fullmatch('[0-9a-f]{40}', sha) is not None,
                'Invalid historical path or Git blob identity')
    require(data['new_checked_sources'] == ['verification/check_registration.py'],
            'New verification sources require explicit registry review')
    return data


def paths(root):
    return {p.relative_to(root).as_posix() for p in (root / 'verification').rglob('*') if p.is_file()}


def check_paths(data, found):
    original = set(data['original_paths'])
    missing = original - found
    require(not missing, 'Baseline verification file missing: ' + repr(sorted(missing)[:5]))
    expected_sources = {p for p in original | set(data['new_checked_sources'])
                        if Path(p).suffix in SUFFIX}
    actual_sources = {p for p in found if Path(p).suffix in SUFFIX}
    require(actual_sources == expected_sources,
            'Unregistered verification source: ' + repr(sorted(actual_sources ^ expected_sources)))


def check_runners(data, java, source, gradle):
    require("mainClass = 'com.maestroai.training.StandaloneRegression'" in gradle
            and "mainClass = 'com.maestroai.training.SourceVerification'" in gradle,
            'Required Java entry point missing from Gradle')
    regressions = {name[:-5] for name in java if name.endswith('Regression.java')}
    manual_java = set(data['java_manual_not_ci'])
    require(len(manual_java) == len(data['java_manual_not_ci'])
            and manual_java <= regressions,
            'Invalid Java manual-probe registry')
    calls = {name[:-5]: set(CALL.findall(content)) for name, content in java.items()}
    reached, todo = set(), ['StandaloneRegression', 'SourceVerification']
    while todo:
        name = todo.pop()
        if name in reached:
            continue
        require(name in calls, 'Required Java runner source missing: ' + name)
        reached.add(name)
        todo.extend((calls[name] & regressions) - reached)
    missing_java = regressions - reached - manual_java
    require(not missing_java, 'Java regressions not reachable from CI runner: '
            + ', '.join(sorted(missing_java)))
    require(not (reached & manual_java),
            'Java probe became CI-reachable; classify the new coverage: '
            + ', '.join(sorted(reached & manual_java)))

    node = {Path(p).name for p in data['original_paths'] if p.endswith('.cjs')}
    manual_node = set(data['node_manual_not_ci'])
    invoked = set(NODE.findall(source))
    require(len(manual_node) == len(data['node_manual_not_ci'])
            and manual_node <= node and not (manual_node & invoked),
            'Invalid Node manual-probe registry')
    require(invoked <= node, 'Node runner references missing source oracle')
    require(node == invoked | manual_node,
            'Unregistered Node oracle: ' + repr(sorted(node - invoked - manual_node)))
    require('sourceVerification' in source and 'selected_profiles' in source
            and 'bounded_run' in source,
            'Native/pack execution linkage missing')


def check(root=ROOT, found=None, overrides=None, source_override=None):
    data = load(root)
    check_paths(data, paths(root) if found is None else found)
    folder = root / 'verification/java/com/maestroai/training'
    java = {p.name: p.read_text(encoding='utf-8') for p in folder.glob('*.java')}
    if overrides:
        java.update(overrides)
    source = (root / 'verification/source_oracle.py').read_text(encoding='utf-8')
    if source_override is not None:
        source = source_override
    gradle = (root / 'verification/verification.init.gradle').read_text(encoding='utf-8')
    check_runners(data, java, source, gradle)
    return data


class RegistryRegression(unittest.TestCase):
    def test_live_inventory_and_reachability(self):
        check()

    def test_original_java_and_documentation_cannot_vanish(self):
        data = load(ROOT)
        found = paths(ROOT)
        for removed in ('verification/verification.init.gradle',
                        'verification/java/com/maestroai/training/Phase7MechanicsAdapterRegression.java',
                        'verification/PHASE635_FINAL_PHASE6_CLOSURE_AND_ACCEPTANCE.md'):
            with self.subTest(path=removed), self.assertRaisesRegex(ValueError, 'missing'):
                check_paths(data, found - {removed})

    def test_new_unregistered_tests_and_scripts_are_rejected(self):
        data, found = load(ROOT), paths(ROOT)
        for added in ('verification/java/com/maestroai/training/NewRegression.java',
                      'verification/missing_oracle.cjs', 'verification/missing_gate.py'):
            with self.subTest(path=added), self.assertRaisesRegex(ValueError, 'Unregistered'):
                check_paths(data, found | {added})

    def test_removed_standalone_regression_call_is_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/StandaloneRegression.java'
        content = p.read_text(encoding='utf-8')
        marker = 'Phase7MechanicsAdapterRegression.run();'
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'not reachable'):
            check(overrides={p.name: content.replace(marker, '')})

    def test_removed_node_oracle_call_is_rejected(self):
        content = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        marker = "'verification/accuracy_miss_oracle.cjs'"
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'Unregistered Node oracle'):
            check(source_override=content.replace(marker, "''"))

    def test_history_is_not_a_file_count_target(self):
        self.assertEqual(load(ROOT)['origin_commit'], ORIGIN)


def main():
    if sys.argv[1:] == ['--self-test']:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(RegistryRegression)
        return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1
    if sys.argv[1:]:
        raise ValueError('Usage: check_registration.py [--self-test]')
    data = check()
    print('Verification registrations OK: 256 original paths, '
          + str(len(data['java_manual_not_ci'])) + ' explicit manual Java probes, '
          + str(len(data['node_manual_not_ci'])) + ' explicit manual Node oracles')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
