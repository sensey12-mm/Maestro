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
    """Fail closed on unreachable Python/Node runners, not mere call references.

    The critical runner uses a canonical AST shape: comments/formatting may
    change, but a skipped loop iteration, dead branch, early return or removed
    completion receipt requires deliberate review of this independent gate.
    """
    tree = ast.parse(source)
    executor = [n for n in tree.body if isinstance(n, ast.FunctionDef)
                and n.name == 'run_complete_oracle_profile']
    require(len(executor) == 1, 'Source profile completion executor missing')
    # AST comparison excludes location, whitespace and comments.
    reference = ast.parse('''def run_complete_oracle_profile(profile, simulator):
    """Only attest a pinned profile after every subprocess has exited cleanly."""
    limit = {'native': 25, 'pack': 6}.get(profile)
    if limit is None:
        raise ValueError('Incomplete or unknown source oracle profile')
    expected = list(commands_for(profile, simulator))
    if len(expected) != limit:
        raise ValueError('Incomplete or unknown source oracle profile')
    completed = []
    for command in expected:
        bounded_run(command, check=True)
        completed.append(command)
    if completed != expected:
        raise AssertionError('Source-oracle execution was incomplete: ' + profile)
    print('SOURCE_ORACLE_PROFILE_COMPLETE profile=' + profile
          + ' count=' + str(len(completed)))
    return len(completed)''').body[0]
    require(ast.dump(executor[0], include_attributes=False)
            == ast.dump(reference, include_attributes=False),
            'Source-oracle completion control flow changed or bypassed')
    mains = [n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == 'main']
    require(len(mains) == 1, 'Required source oracle main unavailable')
    main = mains[0]
    requires = [
        ('native', 'do_native', 25, 2),
        ('pack', 'do_pack', 6, 1),
    ]
    for profile, flag, count, with_depth in requires:
        branches = [n for n in main.body if isinstance(n, ast.If)
                    and isinstance(n.test, ast.Name) and n.test.id == flag]
        require(len(branches) == 1, 'Source profile direct branch missing: ' + profile)
        branch = branches[0]
        parents = {id(child): parent for parent in ast.walk(branch)
                   for child in ast.iter_child_nodes(parent)}
        expected_test = ("run_complete_oracle_profile('" + profile
                         + "', " + ('folder' if profile == 'native' else 'simulator_root')
                         + ') != ' + str(count))
        guards = [n for n in ast.walk(branch) if isinstance(n, ast.If)
                  and ast.unparse(n.test) == expected_test]
        require(len(guards) == 1 and len(guards[0].body) == 1
                and isinstance(guards[0].body[0], ast.Raise) and not guards[0].orelse,
                'Source profile runtime receipt gate missing: ' + profile)
        # Reject calls gated by additional if/while/for/try, even if a matching
        # call expression survives in an unreachable subtree.
        parent = parents[id(guards[0])]
        depth = 0
        while parent is not branch:
            require(isinstance(parent, ast.With),
                    'Source profile runner hidden by conditional control flow: ' + profile)
            depth += 1
            parent = parents[id(parent)]
        require(depth == with_depth,
                'Source profile mandatory scope or independent native process gate changed')
        require(not any(isinstance(n, (ast.Return, ast.Continue, ast.Break))
                        for n in ast.walk(branch)),
                'Source profile contains an early exit: ' + profile)
        require(not any(isinstance(n, ast.Raise) and n is not guards[0].body[0]
                        for n in ast.walk(branch)),
                'Source profile contains a bypassing raise: ' + profile)
    # The only main() returns are the self-test result and normal completion.
    require(len([n for n in ast.walk(main) if isinstance(n, ast.Return)]) == 2
            and not any(isinstance(n, (ast.Break, ast.Continue))
                        for n in ast.walk(main)),
            'Source profile main gained an early return or control-flow exit')
    assignments = [n for n in main.body if isinstance(n, ast.Assign)
                   and len(n.targets) == 1 and isinstance(n.targets[0], ast.Tuple)
                   and [elt.id for elt in n.targets[0].elts
                        if isinstance(elt, ast.Name)] == ['do_native', 'do_pack']
                   and isinstance(n.value, ast.Call)
                   and isinstance(n.value.func, ast.Name)
                   and n.value.func.id == 'selected_profiles']
    require(len(assignments) == 1
            and len([n for n in ast.walk(main)
                     if isinstance(n, ast.Name) and isinstance(n.ctx, ast.Store)
                     and n.id in {'do_native', 'do_pack'}]) == 2,
            'Source profile selectors reassigned or bypassed')




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



def require_direct_java_regression_flow(java):
    """A source reference is not execution: groups must be direct linear calls."""
    clean = JAVA_NONCODE.sub(' ', java)
    groups = ('runOwnMechanicsRegressions', 'runPublicCompetitiveRegressions',
              'runBenchmarkRegressions', 'runLiveEvidenceRegressions',
              'runRuntimeProvenanceRegressions')
    for group in groups:
        pattern = (r'private\s+static\s+void\s+' + group
                   + r'\(\)\s+throws\s+Exception\s*\{([\s\S]*?)\n    \}')
        matches = re.findall(pattern, clean)
        require(len(matches) == 1 and re.fullmatch(
            r'(?:\s*[A-Z]\w+Regression\.(?:run|inventory)\(\);)+'
            r'\s*completedRegressionGroups\+\+;\s*', matches[0]) is not None,
            'Java regression group is not direct, linear, and completion-counted: ' + group)
    require('private static int completedRegressionGroups;' in clean,
            'Java regression group completion counter missing')
    completion = re.findall(
        r'private\s+static\s+void\s+requireRegisteredGroupsComplete\(\)\s*'
        r'\{([\s\S]*?)\n    \}', clean)
    require(len(completion) == 1 and
            re.search(r'if\s*\(completedRegressionGroups\s*!=\s*5\)\s*'
                      r'throw\s+new\s+AssertionError\(', completion[0]) is not None,
            'Java completed-regression-group runtime assertion missing')
    main = re.findall(
        r'public\s+static\s+void\s+main\([^)]*\)\s*throws\s+Exception\s*'
        r'\{([\s\S]*?)\n    \}', clean)
    require(len(main) == 1,
            'Java standalone regression main absent or changed')
    body = main[0]
    require(not re.search(r'\b(?:if|for|while|do|switch|try|catch|finally|'
                          r'return|break|continue|throw|System\.exit)\b', body),
            'Java regression main contains a conditional or early-exit bypass')
    expected = list(groups) + ['requireRegisteredGroupsComplete']
    observed = re.findall(
        r'\b(run\w+Regressions|requireRegisteredGroupsComplete)\(\);', body)
    require(observed == expected and
            body.index('requireRegisteredGroupsComplete();')
            > body.index('runRuntimeProvenanceRegressions();'),
            'Java regression completion is not the direct final group gate')


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


def check_source_java_execution(root, java, gradle):
    """Check performance edits cannot silently bypass independent Graal checks.

    Static order/call registration complements full native + pack CI execution.
    Timings and JVM heap bounds are verification-only, not production settings.
    """
    name = 'SourceVerification.java'
    content = java[name]
    expected = (
        'BlockedHitRegression', 'HealingRecoilRegression',
        'RecoilContinuationRegression', 'PivotOutcomeRegression',
        'AccuracyMissRegression', 'ResidualRecoveryRegression',
        'SandstormRegression', 'WeatherSuppressionRegression',
        'WeatherSpeedRegression', 'UtilityUmbrellaRegression',
        'WeatherAbilityRegression', 'ToxicEntryRegression',
        'RosterAbilityRegression', 'DownloadEntryRegression',
        'WeatherEntryRegression', 'ColdWeatherRegression',
        'WeatherSetterRegression', 'OwnMegaYRegression',
        'OwnZMoveRegression', 'OwnReentryRegression',
        'OwnXerneasRegression', 'GeomancyContinuityRegression',
        'OwnFirstWebRegression', 'StatusSpeedRegression',
        'PsychicTerrainRegression', 'ModifierCompositionRegression',
    )
    observed = tuple(re.findall(r'(?m)^\s*(\w+Regression)\.differential\(engine\);\s*$', content))
    require(observed == expected,
            'Graal native differential coverage/order altered by CI performance edit')
    # Existing differential calls run directly inside the native Graal try body,
    # never in a newly inserted if(false)/loop/early-return subtree. Source
    # registration alone would not detect that bypass.
    clean = JAVA_NONCODE.sub(' ', content)
    invocations = list(re.finditer(
        r'(?m)^\s*\w+Regression\.differential\(engine\);', clean))
    depths = [clean[:m.start()].count('{') - clean[:m.start()].count('}')
              for m in invocations]
    require(len(invocations) == len(expected) and all(depth == 3 for depth in depths),
            'Graal native differential control flow hidden in nested branch')
    native_scope = clean.find('try (var engine =')
    require(native_scope >= 0 and native_scope < invocations[0].start()
            and not re.search(r'\b(?:return|continue|break|System\.exit|Runtime\.getRuntime)\b',
                              clean[native_scope:invocations[-1].end()]),
            'Graal native differential control flow has an early exit')
    for marker in (
        'DispatchIntegrationRegression.run(Path.of(args[1]));',
        'PackSourceRegression.run(simulator, Path.of(args[1]), args.length > 3);',
        'MoveAttestationRegression.run(engine,probe,publicBattle,modules,anchor,expected);',
        'OwnXerneasRegression.attestation(engine);',
        'NativeRegistryRegression.prepare(engine,probe,publicBattle,modules,resources);',
        'profile("native-differentials", ciStart);',
        'profile("native-move-attestation", ciStart);',
        'profile("native-complete", ciStart);',
        'profile("pack-complete", ciStart);',
    ):
        require(content.count(marker) == 1,
                'Graal attestation/pack gate or timing marker removed: ' + marker)
    require(gradle.count("p.tasks.register('sourceVerification', JavaExec)") == 1
            and "minHeapSize = '512m'" in gradle
            and "maxHeapSize = '3g'" in gradle
            and "def sourceJava = System.getenv('MAESTRO_SOURCE_JAVA')" in gradle
            and "if (sourceJava) executable = sourceJava" in gradle
            and 'CI_SOURCE_JVM vendor=' in content
            and "mainClass = 'com.maestroai.training.SourceVerification'" in gradle
            and "if (p.hasProperty('verificationPack')) args 'pack'" in gradle
            and "if (p.hasProperty('verificationLayoutOnly')) args 'layout-only'" in gradle,
            'Source-only performance tuning or native/pack argument isolation removed')


def check_runners(data, java, source, gradle, matrix, workflow, native_hook):
    require("mainClass = 'com.maestroai.training.StandaloneRegression'" in gradle
            and "mainClass = 'com.maestroai.training.SourceVerification'" in gradle,
            'Required Java entry point missing from Gradle')
    # The runner is grouped by responsibility; a dead group must not appear
    # registered merely because its method still contains class references.
    require_direct_java_regression_flow(java.get('StandaloneRegression.java', ''))
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
    require("with parallel_native_source_gradle(native_gradle):" in source
            and "def parallel_native_source_gradle(command, limit=5400):" in source
            and "process = subprocess.Popen(command, start_new_session=True)" in source
            and "status = process.wait(timeout=limit)" in source
            and "raise subprocess.CalledProcessError(status, command)" in source
            and "os.killpg(process.pid, signal.SIGTERM)" in source
            and "os.killpg(process.pid, signal.SIGKILL)" in source
            and "signal.signal(signal.SIGTERM, interrupted)" in source
            and "signal.signal(signal.SIGTERM, original_termination)" in source,
            'Native overlapping verification must await both statuses and cancel failed children')
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
    check_source_java_execution(root, java, gradle)
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

    def test_required_python_source_oracles_reject_control_flow_bypasses(self):
        source = (ROOT / 'verification/source_oracle.py').read_text()
        loop = 'for command in expected:\n        bounded_run(command, check=True)'
        self.assertIn(loop, source)
        for name, mutant in (
                ('continue before execution',
                 source.replace(loop, 'for command in expected:\n        continue\n        bounded_run(command, check=True)')),
                ('early return before execution',
                 source.replace(loop, 'for command in expected:\n        return 25\n        bounded_run(command, check=True)')),
                ('dead oracle execution',
                 source.replace(loop, 'for command in expected:\n        if False:\n            bounded_run(command, check=True)')),
                ('false native branch',
                 source.replace('if do_native:', 'if False and do_native:')),
                ('false pack branch',
                 source.replace('if do_pack:', 'if False and do_pack:')),
                ('premature main return',
                 source.replace('if do_native:', 'return 0\n    if do_native:')),
                ('dead receipt gate',
                 source.replace("if run_complete_oracle_profile('native', folder) != 25:",
                                "if False and run_complete_oracle_profile('native', folder) != 25:")),
                ('silent completion assertion removal',
                 source.replace('if completed != expected:', 'if False:')),
                ('silent profile short circuit',
                 source.replace("limit = {'native': 25, 'pack': 6}.get(profile)",
                                'return 25\n    limit = {\'native\': 25, \'pack\': 6}.get(profile)')),
        ):
            with self.subTest(case=name):
                self.assertNotEqual(mutant, source)
                with self.assertRaisesRegex(ValueError, 'profile|completion|control flow'):
                    check(source_override=mutant)

    def test_required_java_groups_reject_dead_and_early_exit_paths(self):
        path = ROOT / 'verification/java/com/maestroai/training/StandaloneRegression.java'
        source = path.read_text()
        for name, before, after in (
                ('dead standalone group',
                 '        runBenchmarkRegressions();',
                 '        if (false) { runBenchmarkRegressions(); }'),
                ('early return from main',
                 '        runBenchmarkRegressions();',
                 '        return;\n        runBenchmarkRegressions();'),
                ('dead single regression',
                 '        Phase7BenchmarkRegression.run();',
                 '        if (false) { Phase7BenchmarkRegression.run(); }'),
                ('early return inside group',
                 '        Phase7BenchmarkRegression.run();',
                 '        return;\n        Phase7BenchmarkRegression.run();'),
                ('fake group completion without execution',
                 '        Phase7BenchmarkRegression.run();',
                 '        // Phase7BenchmarkRegression.run();'),
                ('missing group receipt',
                 '        completedRegressionGroups++;',
                 '        // completedRegressionGroups++;'),
                ('missing final receipt',
                 '        requireRegisteredGroupsComplete();',
                 '        // requireRegisteredGroupsComplete();'),
        ):
            with self.subTest(case=name):
                self.assertIn(before, source)
                with self.assertRaisesRegex(ValueError, 'Java regression|ordered Java'):
                    check(overrides={path.name: source.replace(before, after, 1)})

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
        with self.assertRaisesRegex(ValueError, 'Java regression|ordered Java regression coverage'):
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

    def test_native_overlap_fail_closed_invocation_is_registered(self):
        source = (ROOT / 'verification/source_oracle.py').read_text()
        for before, after in (
                ('with parallel_native_source_gradle(native_gradle):', 'if True:'),
                ('status = process.wait(timeout=limit)', 'status = 0'),
                ('os.killpg(process.pid, signal.SIGTERM)', 'pass'),
                ('signal.signal(signal.SIGTERM, interrupted)', 'pass'),
                ('raise subprocess.CalledProcessError(status, command)', 'pass')):
            with self.subTest(fragment=before):
                with self.assertRaisesRegex(ValueError, 'Source profile|overlapping verification'):
                    check(source_override=source.replace(before, after))

    def test_removed_node_oracle_call_is_rejected(self):
        matrix = (ROOT / 'verification/oracle_matrix.py').read_text(encoding='utf-8')
        missing = '("accuracy_miss_oracle.cjs",)'
        self.assertIn(missing, matrix)
        # Replace with a valid, duplicate entry; malformed Python would test
        # the parser instead of a silently skipped independent oracle.
        with self.assertRaisesRegex(ValueError, 'profile order'):
            check(matrix_override=matrix.replace(missing, '("source_oracle.cjs",)'))
        source = (ROOT / 'verification/source_oracle.py').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Source profile'):
            check(source_override=source.replace("run_complete_oracle_profile('native', folder)",
                                                  "run_complete_oracle_profile('pack', folder)"))
        # A profile reference alone is not execution: the executor must run
        # every subprocess and check its successful completion receipt.
        self.assertIn('bounded_run(command, check=True)', source)
        with self.assertRaisesRegex(ValueError, 'completion control flow'):
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

    def test_graal_native_suite_and_performance_boundary_are_required(self):
        path = ROOT / 'verification/java/com/maestroai/training/SourceVerification.java'
        original = path.read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Graal native differential coverage/order'):
            check(overrides={path.name: original.replace(
                'ModifierCompositionRegression.differential(engine);',
                'ModifierCompositionRegression.runNothing(engine);')})
        with self.assertRaisesRegex(ValueError, 'Graal attestation/pack gate'):
            check(overrides={path.name: original.replace(
                'MoveAttestationRegression.run(engine,probe,publicBattle,modules,anchor,expected);',
                'System.out.println("skipped");')})
        with self.assertRaisesRegex(ValueError, 'Graal attestation/pack gate'):
            check(overrides={path.name: original.replace(
                'profile("native-move-attestation", ciStart);',
                'System.out.println("unprofiled");')})
        # The source-only heap change may not leak into other Gradle Java tasks
        # or weaken pack layout / independent native invocation.
        gradle = (ROOT / 'verification/verification.init.gradle').read_text()
        with self.assertRaisesRegex(ValueError, 'Source-only performance'):
            check_with_gradle = gradle.replace("maxHeapSize = '3g'", "maxHeapSize = '256m'")
            check_source_java_execution(ROOT, {path.name: original}, check_with_gradle)
        with self.assertRaisesRegex(ValueError, 'Source-only performance'):
            check_with_gradle = gradle.replace(
                "if (sourceJava) executable = sourceJava", "if (sourceJava) println(sourceJava)")
            check_source_java_execution(ROOT, {path.name: original}, check_with_gradle)

    def test_native_graal_differentials_reject_dead_branches_and_early_exit(self):
        path = ROOT / 'verification/java/com/maestroai/training/SourceVerification.java'
        original = path.read_text()
        marker = '            ModifierCompositionRegression.differential(engine);'
        self.assertIn(marker, original)
        for name, replacement in (
                ('dead nested branch', '            if (false) {\n' + marker + '\n            }'),
                ('unconditional early return', '            return;\n' + marker),
                ('premature process exit', '            System.exit(0);\n' + marker),
        ):
            with self.subTest(case=name):
                with self.assertRaisesRegex(ValueError, 'Graal native differential control flow'):
                    check(overrides={path.name: original.replace(marker, replacement)})

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
