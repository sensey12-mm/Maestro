#!/usr/bin/env python3
"""Fail-closed verification source, origin and execution-registration checks.

This does not pretend that separately listed manual probes run in CI.
"""
import ast
import json
from pathlib import Path
import re
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGIN = '8d6f193fec55554c41b4d16dbfc226d9b88e83ff'
SUFFIX = {'.java', '.cjs', '.py'}
CALL = re.compile(r'\b([A-Z][A-Za-z0-9_]*Regression)\s*\.\s*[A-Za-z_][A-Za-z0-9_]*\s*\(')
# Ignore non-executable Java comments, string literals and text blocks.
# A call name in a comment must NEVER satisfy a required registration.
JAVA_NONCODE = re.compile(
    r"//[^\n]*|/\*[\s\S]*?\*/|\"\"\"[\s\S]*?\"\"\"|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'"
)
# Java source also loads selected Node oracles as literals into Graal.
EMBEDDED = re.compile(r"['\"]verification/([a-z][a-z0-9_]*\.cjs)['\"]")


def declared_oracles(matrix):
    """Parse static Python declarations without evaluating arbitrary source."""
    declared = {}
    for node in ast.parse(matrix).body:
        if isinstance(node, ast.Assign) and len(node.targets) == 1:
            target = node.targets[0]
            if isinstance(target, ast.Name) and target.id in {'NATIVE_STEPS', 'PACK_STEPS'}:
                require(target.id not in declared, 'Duplicate source profile')
                declared[target.id] = ast.literal_eval(node.value)
    require(set(declared) == {'NATIVE_STEPS', 'PACK_STEPS'}, 'Missing source-oracle profile')
    for profile, steps in declared.items():
        require(isinstance(steps, tuple), 'Invalid declared source profile')
        for step in steps:
            require(isinstance(step, tuple) and len(step) in (1, 2)
                    and re.fullmatch('[a-z_0-9]+[.]cjs', step[0])
                    and (len(step) == 1 or step[1] == '--setters'),
                    'Invalid source-oracle invocation')
    require(len(declared['NATIVE_STEPS']) == 25 and len(declared['PACK_STEPS']) == 6,
            'Native/pack source-oracle invocation count missing')
    require(declared['NATIVE_STEPS'].count(('weather_entry_oracle.cjs',)) == 1
            and declared['NATIVE_STEPS'].count(('weather_entry_oracle.cjs', '--setters')) == 1,
            'Missing independent weather setter checks')
    return {step[0] for rows in declared.values() for step in rows}


def required_oracle_profiles(source):
    tree = ast.parse(source)
    imported = any(isinstance(n, ast.ImportFrom) and n.module == 'oracle_matrix'
                   and 'commands_for' in [a.name for a in n.names] for n in tree.body)
    invoked = [n.args[0].value for n in ast.walk(tree)
               if isinstance(n, ast.Call) and isinstance(n.func, ast.Name)
               and n.func.id == 'commands_for' and n.args
               and isinstance(n.args[0], ast.Constant)]
    require(imported and sorted(invoked) == ['native', 'pack'],
            'Required independent native/pack source runner invocation missing')


def require(ok, why):
    if not ok:
        raise ValueError(why)


def load(root):
    data = json.loads((root / 'verification/verification_registry.json').read_text(encoding='utf-8'))
    require(data.get('schema') == 2 and data.get('origin_commit') == ORIGIN,
            'Verification inventory schema or source baseline altered')
    origins = data['original_paths']
    require(len(origins) == 256, 'Original 256-file inventory cannot be silently truncated')
    for name, sha in origins.items():
        require(name.startswith('verification/') and '..' not in Path(name).parts
                and re.fullmatch('[0-9a-f]{40}', sha) is not None,
                'Invalid historical path or Git blob identity')
    require(data['new_checked_sources'] == ['verification/check_registration.py',
                                            'verification/oracle_matrix.py'],
            'New verification sources require explicit registry review')
    migrations = data.get('migrations')
    require(isinstance(migrations, dict)
            and set(migrations) == {'verification/check_native_split_hooks.py'},
            'Unreviewed source-file migration')
    for origin, move in migrations.items():
        require(origin in origins and move.get('original_blob') == origins[origin],
                'Migrated original Git blob identity changed')
        require(move.get('replacement') == 'verification/runtime_hook_oracle.py'
                and len(move.get('rationale', '')) > 40,
                'Migration justification or replacement missing')
        require(move.get('coverage_command') == 'python3 verification/runtime_hook_oracle.py --verify-pinned'
                and move.get('negative_command') == 'python3 verification/runtime_hook_oracle.py --self-test',
                'Migration CI coverage mapping changed')
    return data


def paths(root):
    return {p.relative_to(root).as_posix() for p in (root / 'verification').rglob('*') if p.is_file()}


def check_paths(data, found):
    original = set(data['original_paths'])
    migrated = set(data['migrations'])
    missing = original - found - migrated
    require(not missing, 'Baseline verification file missing: ' + repr(sorted(missing)[:5]))
    require(not (migrated & found), 'Already migrated source unexpectedly returned')
    require(all(move['replacement'] in found for move in data['migrations'].values()),
            'Migrated source replacement missing')
    expected_sources = {p for p in (original - migrated) | set(data['new_checked_sources'])
                        if Path(p).suffix in SUFFIX}
    actual_sources = {p for p in found if Path(p).suffix in SUFFIX}
    require(actual_sources == expected_sources,
            'Unregistered verification source: ' + repr(sorted(actual_sources ^ expected_sources)))


def check_runners(data, java, source, gradle, matrix, workflow, native_hook):
    require("mainClass = 'com.maestroai.training.StandaloneRegression'" in gradle
            and "mainClass = 'com.maestroai.training.SourceVerification'" in gradle,
            'Required Java entry point missing from Gradle')
    regressions = {name[:-5] for name in java if name.endswith('Regression.java')}
    manual_java = set(data['java_manual_not_ci'])
    require(len(manual_java) == len(data['java_manual_not_ci'])
            and manual_java <= regressions,
            'Invalid Java manual-probe registry')
    calls = {name[:-5]: set(CALL.findall(JAVA_NONCODE.sub(' ', content)))
             for name, content in java.items()}
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
    required_oracle_profiles(source)
    invoked = declared_oracles(matrix)
    # Only quoted Java string tokens represent resource loading. A comment
    # mentioning a file must never satisfy an oracle registration.
    embedded = {filename for name in reached if name + '.java' in java
                for token in JAVA_NONCODE.findall(java[name + '.java'])
                if token.startswith(('"', "'"))
                for filename in EMBEDDED.findall(token)}
    # Java sometimes imports the same oracle also launched directly by
    # the independent Node runner. Sharing a source is valid; losing an
    # explicit Java-only entry path is not.
    expected_embedded = set(data['node_embedded_java'])
    require(len(expected_embedded) == len(data['node_embedded_java'])
            and expected_embedded <= embedded and not (expected_embedded & invoked),
            'Java-embedded Node oracle registration changed: '
            + repr(sorted(expected_embedded - embedded)))
    require(invoked <= node and embedded <= node,
            'Python/Java runner references missing Node oracle')
    require(node == invoked | expected_embedded,
            'Unregistered Node oracle: ' + repr(sorted(node - invoked - expected_embedded)))
    require('sourceVerification' in source and 'selected_profiles' in source
            and 'bounded_run' in source,
            'Native/pack execution linkage missing')
    for moved in data['migrations'].values():
        require(all(workflow.count(moved[key] + '\n') == 1
                    for key in ('coverage_command', 'negative_command')),
                'Moved ABI preflight missing from mandatory CI job')
    require('def preflight_exact_pinned_dependency():' in native_hook
            and 'verified = checked_bytes(jar.read_bytes(), DEPENDENCY_SHA256)' in native_hook
            and 'verify_pinned_hooks(verified)' in native_hook,
            'Native preflight lost exact dependency integrity check')


def check(root=ROOT, found=None, overrides=None, source_override=None, matrix_override=None, workflow_override=None, hook_override=None):
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
    matrix = ((root / 'verification/oracle_matrix.py').read_text(encoding='utf-8')
              if matrix_override is None else matrix_override)
    workflow = ((root / '.github/workflows/verify.yml').read_text(encoding='utf-8')
                if workflow_override is None else workflow_override)
    native_hook = ((root / 'verification/runtime_hook_oracle.py').read_text(encoding='utf-8')
                   if hook_override is None else hook_override)
    check_runners(data, java, source, gradle, matrix, workflow, native_hook)
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
        marker = 'Phase7BenchmarkRegression.run();'
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'not reachable'):
            check(overrides={p.name: content.replace(marker, '')})
        # Names left as comments do not count as executable calls.
        with self.assertRaisesRegex(ValueError, 'not reachable'):
            check(overrides={p.name: content.replace(marker, '// ' + marker)})

    def test_removed_indirect_phase7_regression_call_is_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/Phase7SelfPlayRegression.java'
        content = p.read_text(encoding='utf-8')
        marker = 'Phase7MechanicsAdapterRegression.run('
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'not reachable'):
            check(overrides={p.name: content.replace(marker, 'UnregisteredFixture.run(')})

    def test_removed_java_embedded_oracle_link_is_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/OwnXerneasRegression.java'
        content = p.read_text(encoding='utf-8')
        marker = 'verification/own_xerneas_oracle.cjs'
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'Java-embedded Node oracle'):
            check(overrides={p.name: content.replace(marker, 'verification/fake_xerneas_oracle.cjs')})
        # The deleted import cannot be resurrected by mentioning it in a comment.
        with self.assertRaisesRegex(ValueError, 'Java-embedded Node oracle'):
            check(overrides={p.name: content.replace(marker, 'verification/fake_xerneas_oracle.cjs')
                  + '\n// ' + marker + '\n'})

    def test_removed_node_oracle_call_is_rejected(self):
        matrix = (ROOT / 'verification/oracle_matrix.py').read_text(encoding='utf-8')
        missing = '("accuracy_miss_oracle.cjs",)'
        self.assertIn(missing, matrix)
        with self.assertRaisesRegex(ValueError, 'invocation count'):
            check(matrix_override=matrix.replace(missing, ''))
        source = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'runner invocation missing'):
            check(source_override=source.replace("commands_for('native', folder)",
                                                  "commands_for('pack', folder)"))

    def test_native_preflight_migration_is_mapped_and_enforced(self):
        data, found = load(ROOT), paths(ROOT)
        old = 'verification/check_native_split_hooks.py'
        self.assertEqual(data['migrations'][old]['original_blob'], data['original_paths'][old])
        self.assertNotIn(old, found)
        with self.assertRaisesRegex(ValueError, 'unexpectedly returned'):
            check_paths(data, found | {old})
        with self.assertRaisesRegex(ValueError, 'replacement missing'):
            check_paths(data, found - {'verification/runtime_hook_oracle.py'})
        source = (ROOT / '.github/workflows/verify.yml').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'preflight missing'):
            check(workflow_override=source.replace(
                'python3 verification/runtime_hook_oracle.py --verify-pinned', 'echo skip-preflight'))
        hook = (ROOT / 'verification/runtime_hook_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'dependency integrity'):
            check(hook_override=hook.replace(
                'verified = checked_bytes(jar.read_bytes(), DEPENDENCY_SHA256)',
                'verified = jar.read_bytes()'))

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
          + str(len(data['node_embedded_java'])) + ' Java-embedded source oracles')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
