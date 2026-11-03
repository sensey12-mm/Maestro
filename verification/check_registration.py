#!/usr/bin/env python3
"""Fail-closed verification source, origin and execution-registration checks.

This does not pretend that separately listed manual probes run in CI.
"""
import ast
import hashlib
import json
from pathlib import Path
import re
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGIN = '8d6f193fec55554c41b4d16dbfc226d9b88e83ff'
ORIGIN_INVENTORY_SHA256 = '80108f5ab57d4bd17dfeea533a8907d1388c4fc5922de4ca98f1e25b9e09d9f9'
SUFFIX = {'.java', '.cjs', '.py'}
CALL = re.compile(r'\b([A-Z][A-Za-z0-9_]*Regression)\s*\.\s*[A-Za-z_][A-Za-z0-9_]*\s*\(')
HOST_CLASS = re.compile(r'\b([A-Z][A-Za-z0-9_]*Regression)\.class\.getName\(\)')
# Ignore non-executable Java comments, string literals and text blocks.
# A call name in a comment must NEVER satisfy a required registration.
JAVA_NONCODE = re.compile(
    r"//[^\n]*|/\*[\s\S]*?\*/|\"\"\"[\s\S]*?\"\"\"|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'"
)
# Java source also loads selected Node oracles as literals into Graal.
EMBEDDED = re.compile(r"['\"]verification/([a-z][a-z0-9_]*\.cjs)['\"]")


def declared_oracles(matrix, registry):
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
    # Membership alone cannot preserve independent setup, run order, and the
    # native-vs-pack responsibility boundary. Review an explicit registry change.
    expected = registry.get('node_profiles', {})
    require(set(expected) == {'native', 'pack'}, 'Oracle profile registry missing')
    for name, steps in (('native', declared['NATIVE_STEPS']),
                        ('pack', declared['PACK_STEPS'])):
        require([list(x) for x in steps] == expected[name],
                'Pinned Node profile order or independent coverage changed: ' + name)
    require(declared['NATIVE_STEPS'].count(('weather_entry_oracle.cjs',)) == 1
            and declared['NATIVE_STEPS'].count(('weather_entry_oracle.cjs', '--setters')) == 1,
            'Missing independent weather setter checks')
    return {step[0] for rows in declared.values() for step in rows}


def required_oracle_profiles(source):
    tree = ast.parse(source)
    imported = any(isinstance(n, ast.ImportFrom) and n.module == 'oracle_matrix'
                   and 'commands_for' in [a.name for a in n.names] for n in tree.body)
    calls = [n for n in ast.walk(tree) if isinstance(n, ast.Call)
             and isinstance(n.func, ast.Name) and n.func.id == 'commands_for']
    executed = []
    for node in ast.walk(tree):
        if not isinstance(node, ast.For) or not isinstance(node.iter, ast.Call):
            continue
        call = node.iter
        if call not in calls or not call.args or not isinstance(call.args[0], ast.Constant):
            continue
        if not isinstance(node.target, ast.Name):
            continue
        target = node.target.id
        # A bare profile reference is not coverage: the command must be passed
        # to bounded_run with check=True within that exact iteration.
        for statement in node.body:
            if (isinstance(statement, ast.Expr)
                    and isinstance(statement.value, ast.Call)
                    and isinstance(statement.value.func, ast.Name)
                    and statement.value.func.id == 'bounded_run'
                    and statement.value.args
                    and isinstance(statement.value.args[0], ast.Name)
                    and statement.value.args[0].id == target
                    and any(kw.arg == 'check' and isinstance(kw.value, ast.Constant)
                            and kw.value.value is True for kw in statement.value.keywords)):
                executed.append(call.args[0].value)
    require(imported and len(calls) == 2 and sorted(executed) == ['native', 'pack'],
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
    canonical = json.dumps(origins, sort_keys=True, separators=(',', ':'), ensure_ascii=True)
    require(hashlib.sha256(canonical.encode('utf-8')).hexdigest() == ORIGIN_INVENTORY_SHA256,
            'Original verification Git identity inventory changed')
    for name, sha in origins.items():
        require(name.startswith('verification/') and '..' not in Path(name).parts
                and re.fullmatch('[0-9a-f]{40}', sha) is not None,
                'Invalid historical path or Git blob identity')
    require(data['new_checked_sources'] == ['verification/check_registration.py',
                                            'verification/oracle_matrix.py',
                                            'verification/pinned_public_archive.py'],
            'New verification sources require explicit registry review')
    additions = data.get('new_registered_files')
    require(isinstance(additions, list) and len(additions) == len(set(additions))
            and set(data['new_checked_sources']) <= set(additions)
            and {'verification/ARCHITECTURE.md', 'verification/INDEX.md',
                 'verification/verification_registry.json'} <= set(additions)
            and all(p.startswith('verification/') and '..' not in Path(p).parts
                    and Path(p).suffix in {'.md', '.py', '.json', '.tsv', '.cjs', '.java', '.gradle'}
                    for p in additions),
            'New verification files must be explicitly registered')
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
    found = set()
    for p in (root / 'verification').rglob('*'):
        if p.is_symlink():
            raise ValueError('Verification tree contains a symlink')
        if p.is_file() and '__pycache__' not in p.parts and p.suffix != '.pyc':
            found.add(p.relative_to(root).as_posix())
    return found


def check_paths(data, found):
    original = set(data['original_paths'])
    migrated = set(data['migrations'])
    require(not (migrated & found), 'Already migrated source unexpectedly returned')
    require(all(move['replacement'] in found for move in data['migrations'].values()),
            'Migrated source replacement missing')
    expected = (original - migrated) | set(data['new_registered_files'])
    require(not (expected - found), 'Baseline verification file missing: ' + repr(sorted(expected - found)[:5]))
    require(not (found - expected), 'Unregistered verification file: ' + repr(sorted(found - expected)[:5]))
    expected_sources = {p for p in expected if Path(p).suffix in SUFFIX}
    actual_sources = {p for p in found if Path(p).suffix in SUFFIX}
    require(actual_sources == expected_sources,
            'Unregistered verification source: ' + repr(sorted(actual_sources ^ expected_sources)))



def check_python_sources(root, source_override=None):
    folder = root / 'verification'
    modules = {p.stem for p in folder.glob('*.py')}
    for file in folder.glob('*.py'):
        source = (source_override if file.name == 'source_oracle.py'
                  and source_override is not None else file.read_text(encoding='utf-8'))
        tree = ast.parse(source, filename=str(file))
        for node in ast.walk(tree):
            if isinstance(node, ast.ImportFrom) and node.module:
                module = node.module.split('.')[0]
                if ((module.endswith('_oracle') or module in {'oracle_matrix', 'check_registration'})
                        and module not in modules):
                    raise ValueError('Broken local Python oracle import in ' + file.name
                                     + ': ' + module)


def check_python_execution(root, workflow, source_override=None):
    """Require every verification Python module to reach a required CI root.

    Static import reachability is a registration check, not proof of executing
    every code path: executable suite tests and native source checks still run.
    """
    folder = root / 'verification'
    scripts = {p.stem for p in folder.glob('*.py')}
    direct = {name for name in re.findall(
        r'(?m)^\s+python3 verification/([a-z_0-9]+)\.py(?:\s|$)', workflow)}
    require(direct <= scripts, 'Public CI references nonexistent Python verification script')
    imports = {}
    for file in folder.glob('*.py'):
        source = (source_override if source_override is not None
                  and file.name == 'source_oracle.py'
                  else file.read_text(encoding='utf-8'))
        tree = ast.parse(source, filename=str(file))
        targets = set()
        # Imports inside self-test classes do not make an otherwise unused
        # production verification path reachable. The source runner must
        # independently import the three external archive consumers.
        for top_level in tree.body:
            if isinstance(top_level, ast.ClassDef):
                continue
            for node in ast.walk(top_level):
                if isinstance(node, ast.ImportFrom) and node.module:
                    imported = node.module.split('.')[0]
                    if imported in scripts:
                        targets.add(imported)
                elif isinstance(node, ast.Import):
                    for entry in node.names:
                        imported = entry.name.split('.')[0]
                        if imported in scripts:
                            targets.add(imported)
        imports[file.stem] = targets
    reached, frontier = set(), list(direct)
    while frontier:
        name = frontier.pop()
        if name in reached:
            continue
        reached.add(name)
        frontier.extend(imports[name] - reached)
    require(reached == scripts,
            'Python verification script unreachable from required CI entry points: '
            + ', '.join(sorted(scripts - reached)))


def check_history_index(root, data, index_override=None):
    report = ((root / 'verification/INDEX.md').read_text(encoding='utf-8')
              if index_override is None else index_override)
    references = re.findall(r'(?m)^- \[[^\]]+\]\(([^)]+\.md)\)$', report)
    historical = {p[len('verification/'):] for p in data['original_paths'] if p.endswith('.md')}
    require(set(references) == historical and len(references) == len(historical),
            'Verification history index contains missing, duplicate or stale paths')


def standalone_order(java):
    """Expand the existing responsibility groups in their actual main-call order.

    This preserves scenario ordering while permitting responsibly reviewed
    additions to the registry, rather than imposing a permanent test count.
    """
    clean = JAVA_NONCODE.sub(' ', java)
    groups = {}
    for match in re.finditer(r'private\s+static\s+void\s+(run\w+Regressions)\(\)\s+throws\s+Exception\s*\{', clean):
        end = clean.find('\n    }', match.end())
        require(end != -1, 'Unclosed standalone regression group: ' + match.group(1))
        groups[match.group(1)] = re.findall(
            r'\b([A-Z]\w+Regression\.(?:run|inventory)\(\);)',
            clean[match.end():end])
    require('public static void main(' in clean, 'Missing standalone regression entrypoint')
    main = clean.split('public static void main(', 1)[1]
    tokens = re.findall(
        r'\b([A-Z]\w+Regression\.(?:run|inventory)\(\);)|(run\w+Regressions)\(\);',
        main)
    order = []
    for call, group in tokens:
        if group:
            require(group in groups and groups[group], 'Referenced standalone group unavailable: ' + group)
            order.extend(groups[group])
        else:
            order.append(call)
    return order



def check_pinned_public_archive_wiring(root, runner, overrides=None):
    """All three public archive consumers must use the same checked transport.

    This is a static registration boundary; the real source profiles and
    synthetic mocked tests exercise each caller's independent evidence checks.
    """
    require('from pinned_public_archive import PinnedPublicArchiveGuards' in runner
            and 'loadTestsFromTestCase(PinnedPublicArchiveGuards)' in runner,
            'Pinned public archive adversarial tests no longer run in source CI')
    expected = {
        'addon_reload_oracle.py':
            "load_pinned_public_jar(home, URL, SHA256, 8 * 1024 * 1024, 'addon')",
        'raid_condition_oracle.py':
            "load_pinned_public_jar(home, URL, SHA256, 16 * 1024 * 1024, 'raid addon')",
        'pack_loader_oracle.py':
            "load_pinned_public_jar(home, PACK_JAR_URL, PACK_JAR_SHA256, 32 * 1024 * 1024, 'installer')",
    }
    for name, expression in expected.items():
        path = root / 'verification' / name
        code = (overrides[name] if overrides is not None and name in overrides
                else path.read_text(encoding='utf-8'))
        parsed = ast.parse(code, filename=str(path))
        imported = any(isinstance(n, ast.ImportFrom)
                       and n.module == 'pinned_public_archive'
                       and any(x.name == 'load_pinned_public_jar' for x in n.names)
                       for n in parsed.body)
        calls = [ast.dump(n, include_attributes=False) for n in ast.walk(parsed)
                 if isinstance(n, ast.Call) and isinstance(n.func, ast.Name)
                 and n.func.id == 'load_pinned_public_jar']
        original = ast.dump(ast.parse(expression, mode='eval').body,
                            include_attributes=False)
        require(imported and calls == [original],
                'Independent public pinned archive consumer wiring changed: ' + name)
        require(not any(isinstance(n, ast.Call) and isinstance(n.func, ast.Attribute)
                        and n.func.attr == 'urlopen' for n in ast.walk(parsed)),
                'Duplicated unaudited public archive downloader reintroduced: ' + name)


def check_runners(data, java, source, gradle, matrix, workflow, native_hook):
    require("mainClass = 'com.maestroai.training.StandaloneRegression'" in gradle
            and "mainClass = 'com.maestroai.training.SourceVerification'" in gradle,
            'Required Java entry point missing from Gradle')
    # The runner is grouped by responsibility; a dead group must not appear
    # registered merely because its method still contains class references.
    actual_order = standalone_order(java.get('StandaloneRegression.java', ''))
    require(actual_order == data['standalone_regression_order'],
            'Original ordered Java regression coverage changed')
    standalone = JAVA_NONCODE.sub(' ', java.get('StandaloneRegression.java', ''))
    main = standalone.split('public static void main(', 1)
    require(len(main) == 2, 'Standalone regression main missing')
    for group in ('runOwnMechanicsRegressions', 'runPublicCompetitiveRegressions',
                  'runBenchmarkRegressions', 'runLiveEvidenceRegressions',
                  'runRuntimeProvenanceRegressions'):
        require(group + '();' in main[1],
                'Standalone regression group disconnected: ' + group)
    regressions = {name[:-5] for name in java if name.endswith('Regression.java')}
    manual_java = set(data['java_manual_not_ci'])
    require(len(manual_java) == len(data['java_manual_not_ci'])
            and manual_java <= regressions,
            'Invalid Java manual-probe registry')
    # Pack-source launchers use ProcessBuilder with Class.getName instead of
    # a direct Java call. Include those children, but require their exit codes
    # to remain checked rather than silently skipping hosted regressions.
    pack = JAVA_NONCODE.sub(' ', java.get('PackSourceRegression.java', ''))
    require(all(needle in pack for needle in (
        'BootProfileRegression.class.getName()',
        'RuntimeCaptureRegression.class.getName()',
        'check(child.waitFor()==0',
        'check(runtime.waitFor()==0',
        'new ProcessBuilder(')),
        'Pinned full-pack Java hosted-regression execution or exit status gate missing')
    calls = {name[:-5]: (set(CALL.findall(clean)) | set(HOST_CLASS.findall(clean)))
             for name, content in java.items()
             for clean in [JAVA_NONCODE.sub(' ', content)]}
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
    invoked = declared_oracles(matrix, data)
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


def check(root=ROOT, found=None, overrides=None, source_override=None, matrix_override=None, workflow_override=None, hook_override=None, index_override=None, consumer_overrides=None):
    data = load(root)
    check_paths(data, paths(root) if found is None else found)
    check_python_sources(root, source_override)
    check_history_index(root, data, index_override)
    folder = root / 'verification/java/com/maestroai/training'
    java = {p.name: p.read_text(encoding='utf-8') for p in folder.glob('*.java')}
    if overrides:
        java.update(overrides)
    source = (root / 'verification/source_oracle.py').read_text(encoding='utf-8')
    if source_override is not None:
        source = source_override
    check_pinned_public_archive_wiring(root, source, consumer_overrides)
    gradle = (root / 'verification/verification.init.gradle').read_text(encoding='utf-8')
    matrix = ((root / 'verification/oracle_matrix.py').read_text(encoding='utf-8')
              if matrix_override is None else matrix_override)
    workflow = ((root / '.github/workflows/verify.yml').read_text(encoding='utf-8')
                if workflow_override is None else workflow_override)
    check_python_execution(root, workflow, source_override)
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
                      'verification/missing_oracle.cjs', 'verification/missing_gate.py',
                      'verification/unreviewed-report.md', 'verification/new_fixture.json'):
            with self.subTest(path=added), self.assertRaisesRegex(ValueError, 'Unregistered'):
                check_paths(data, found | {added})

    def test_removed_standalone_regression_call_is_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/StandaloneRegression.java'
        content = p.read_text(encoding='utf-8')
        marker = 'Phase7BenchmarkRegression.run();'
        self.assertIn(marker, content)
        with self.assertRaisesRegex(ValueError, 'ordered Java regression coverage'):
            check(overrides={p.name: content.replace(marker, '')})
        # Names left as comments do not count as executable calls.
        with self.assertRaisesRegex(ValueError, 'ordered Java regression coverage'):
            check(overrides={p.name: content.replace(marker, '// ' + marker)})

    def test_java_regression_sequence_reordering_is_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/StandaloneRegression.java'
        source = p.read_text(encoding='utf-8')
        a = '        PublicReplayCorpusRegression.run();\n        CompetitiveFormatNormalizerRegression.run();'
        b = '        CompetitiveFormatNormalizerRegression.run();\n        PublicReplayCorpusRegression.run();'
        self.assertIn(a, source)
        with self.assertRaisesRegex(ValueError, 'ordered Java regression coverage'):
            check(overrides={p.name: source.replace(a, b)})

    def test_disconnected_regression_group_rejected(self):
        p = ROOT / 'verification/java/com/maestroai/training/StandaloneRegression.java'
        source = p.read_text(encoding='utf-8')
        marker = '        runBenchmarkRegressions();'
        self.assertIn(marker, source)
        with self.assertRaisesRegex(ValueError, 'ordered Java regression coverage'):
            check(overrides={p.name: source.replace(marker, '        // group disconnected')})

    def test_hosted_java_regressions_and_exit_checks_are_required(self):
        p = ROOT / 'verification/java/com/maestroai/training/PackSourceRegression.java'
        original = p.read_text(encoding='utf-8')
        self.assertIn('RuntimeCaptureRegression.class.getName()', original)
        with self.assertRaisesRegex(ValueError, 'hosted-regression'):
            check(overrides={p.name: original.replace(
                'RuntimeCaptureRegression.class.getName()', 'RuntimeCaptureRegression.class.getSimpleName()')})
        with self.assertRaisesRegex(ValueError, 'exit status gate'):
            check(overrides={p.name: original.replace(
                'check(runtime.waitFor()==0', 'check(true')})

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
        # Replace with a valid, duplicate entry; malformed Python would test
        # the parser instead of a silently skipped independent oracle.
        with self.assertRaisesRegex(ValueError, 'profile order'):
            check(matrix_override=matrix.replace(missing, '("source_oracle.cjs",)'))
        source = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'runner invocation missing'):
            check(source_override=source.replace("commands_for('native', folder)",
                                                  "commands_for('pack', folder)"))
        # A profile remaining in the loop but never invoking the independent
        # subprocess is not a verified oracle, even with a complete manifest.
        self.assertIn('bounded_run(command, check=True)', source)
        with self.assertRaisesRegex(ValueError, 'runner invocation missing'):
            check(source_override=source.replace('bounded_run(command, check=True)', 'pass', 1))

    def test_history_index_preserves_every_original_report(self):
        index = (ROOT / 'verification/INDEX.md').read_text(encoding='utf-8')
        marker = '(PHASE510_DURABLE_TRUST_RECOVERY.md)'
        self.assertIn(marker, index)
        with self.assertRaisesRegex(ValueError, 'history index'):
            check(index_override=index.replace(marker, '(missing-report.md)'))
        with self.assertRaisesRegex(ValueError, 'history index'):
            check(index_override=index + '\n- [duplicate](PHASE510_DURABLE_TRUST_RECOVERY.md)\n')

    def test_pinned_archive_consumer_and_adversarial_registration(self):
        script = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'adversarial tests'):
            check(source_override=script.replace(
                'loadTestsFromTestCase(PinnedPublicArchiveGuards)',
                'loadTestsFromTestCase(OracleMatrixGuards)'))
        path = ROOT / 'verification/addon_reload_oracle.py'
        original = path.read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'consumer wiring changed'):
            check(consumer_overrides={path.name: original.replace(
                'load_pinned_public_jar(home, URL, SHA256, 8 * 1024 * 1024',
                'load_pinned_public_jar(home, URL, SHA256, 80 * 1024 * 1024')})
        with self.assertRaisesRegex(ValueError, 'consumer wiring changed'):
            check(consumer_overrides={path.name: original.replace(
                'load_pinned_public_jar(home, URL, SHA256, 8 * 1024 * 1024',
                'some_other_downloader(home, URL, SHA256, 8 * 1024 * 1024')})

    def test_python_execution_disconnection_is_rejected(self):
        source = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        marker = 'from addon_reload_oracle import addon_sources'
        self.assertIn(marker, source)
        with self.assertRaisesRegex(ValueError, 'Python verification script unreachable'):
            check(source_override=source.replace(marker, 'from urllib.request import urlopen'))
        workflow = (ROOT / '.github/workflows/verify.yml').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Python verification script unreachable'):
            check(workflow_override=workflow.replace(
                'python3 verification/check_publish_artifact.py --self-test',
                'echo skip-verification-script').replace(
                'python3 verification/check_publish_artifact.py build/verified-artifact/',
                'echo skip-publication-verifier'))

    def test_python_import_breakage_rejected(self):
        source = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Broken local Python oracle import'):
            check(source_override=source + '\nfrom missing_oracle import unsafe\n')
        with self.assertRaises(SyntaxError):
            check(source_override=source + '\ndef invalid(:\n')

    def test_oracle_order_and_independent_profile_scope_are_locked(self):
        matrix = (ROOT / 'verification/oracle_matrix.py').read_text(encoding='utf-8')
        left = '    ("source_oracle.cjs",),\n    ("move_attestation_oracle.cjs",),'
        right = '    ("move_attestation_oracle.cjs",),\n    ("source_oracle.cjs",),'
        self.assertIn(left, matrix)
        with self.assertRaisesRegex(ValueError, 'profile order'):
            check(matrix_override=matrix.replace(left, right))
        source = '    ("method_provenance_oracle.cjs",)'
        self.assertIn(source, matrix)
        with self.assertRaisesRegex(ValueError, 'profile order'):
            check(matrix_override=matrix.replace(source, '    ("source_oracle.cjs",)'))

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

    def test_original_git_blob_inventory_is_cryptographically_locked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'verification').mkdir()
            manifest = json.loads((ROOT / 'verification/verification_registry.json').read_text(encoding='utf-8'))
            one_original = next(iter(manifest['original_paths']))
            manifest['original_paths'][one_original] = '0' * 40
            (root / 'verification/verification_registry.json').write_text(
                json.dumps(manifest), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Git identity inventory'):
                load(root)

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
