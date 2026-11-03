#!/usr/bin/env python3
"""Offline checks for this repository's narrow ignore/notice/CI contract."""
import hashlib
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APACHE_SHA256 = '0d542e0c8804e39aa7f37eb00da5a762149dc682d7829451287e11b938e94594'
HIDDEN = ('.gradle/cache.bin', 'build/libs/mod.jar', 'out/classes/X.class',
          'run/world/level.dat', 'run/phase7-corpus/phase7_private.json', 'logs/latest.log', 'local.properties',
          '.env', '.env.production', '.idea/workspace.xml', '.vscode/settings.json',
          'verification/__pycache__/check.cpython-311.pyc')
VISIBLE = ('LICENSE', 'README.md', '.env.example', 'gradlew', 'gradlew.bat',
           'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties',
           '.github/workflows/verify.yml', 'verification/check_repository.py',
           'src/main/java/example/Build.java', 'src/main/resources/data.tsv')


def check_ignore(source):
    # Isolated Git repository: no dependency on the user's global ignore rules.
    with tempfile.TemporaryDirectory() as directory:
        repo = Path(directory)
        subprocess.run(['git', 'init', '-q', str(repo)], check=True)
        (repo / '.gitignore').write_text(source)
        paths = HIDDEN + VISIBLE
        result = subprocess.run(['git', '-c', 'core.excludesFile=/dev/null', '-C', str(repo),
                                 'check-ignore', '--no-index', '-z', '--stdin'],
                                input=('\0'.join(paths) + '\0').encode(), capture_output=True)
        if result.returncode not in (0, 1):
            raise ValueError('Cannot check ignore rules')
        ignored = set(result.stdout.decode().strip('\0').split('\0')) - {''}
        if ignored != set(HIDDEN):
            raise ValueError('Local files exposed or required repository inputs ignored')


def check_public_boundary(root):
    """Do not let public verification inputs link to private source records."""
    # Source-bounded technical docs may mention that private evidence exists;
    # no direct private repository URL should leave this public tree.
    private_target = 'sensey12-mm/' + 'Headquarters'
    prefixes = ('github.com/', 'api.github.com/repos/',
                'raw.githubusercontent.com/')
    candidates = [root / 'README.md', root / '.github/workflows/verify.yml']
    candidates.extend(p for p in (root / 'verification').rglob('*')
                      if p.is_file() and p.suffix in {'.md', '.py', '.java', '.cjs',
                                                       '.json', '.tsv', '.gradle'})
    for path in candidates:
        contents = path.read_text(encoding='utf-8').lower()
        if any((prefix + private_target).lower() in contents for prefix in prefixes):
            raise ValueError('Public verification references a private-repository URL')


def check_workflow(source):
    # Deliberately supports only the checked-in block layout, not arbitrary YAML.
    if ('permissions:\n  contents: read\n' not in source
            or source.count('persist-credentials: false') != 3
            or source.count('actions/checkout@11d5960a326750d5838078e36cf38b85af677262') != 3
            or 'secrets.' in source or 'pull_request_target:' in source):
        raise ValueError('Public CI may not escalate permissions, retain credentials or use secrets')
    for event in ('push', 'pull_request'):
        match = re.search(r'(?m)^  ' + event + r':\n((?:^    .*\n|^      .*\n)+)', source)
        block = match.group(1) if match else ''
        branch_match = re.search(r'(?m)^    branches: \[([^\]\n]+)\]$', block)
        branches = [name.strip() for name in branch_match.group(1).split(',')] if branch_match else []
        if 'main' not in branches:
            raise ValueError('Missing main branch verification')
        if event == 'pull_request' and 'Maestro-2.0' not in branches:
            raise ValueError('Missing Maestro-2.0 review PR verification')
        if any(name in branches for name in ('Maestro-6', 'Maestro457')):
            raise ValueError('Obsolete deleted development-branch CI trigger')
        if event == 'push' and 'Maestro4567' not in branches:
            raise ValueError('Missing Maestro4567 integration CI trigger')
        if event == 'push' and 'Maestro-2.0' not in branches:
            raise ValueError('Missing Maestro-2.0 verification CI trigger')
        # Required verify must be scheduled for every main PR/push, including
        # README-only changes. Workflow-level path skips leave required checks pending.
        if re.search(r'(?m)^ +(paths|paths-ignore):', block):
            raise ValueError('Path filters can strand required verification')
    # A newer integration commit must retire obsolete heavy verification.
    # Exact commit isolation belongs to immutable GitHub run / artifact SHAs,
    # enforced below; serializing/cancelling branch supersession is safe.
    if ("group: verify-${{ github.event.pull_request.number || github.ref }}" not in source
            or "cancel-in-progress: ${{ github.event_name == 'pull_request' || github.ref == 'refs/heads/Maestro4567' || github.ref == 'refs/heads/Maestro-2.0' }}" not in source
            or "&& github.sha || 'shared'" in source):
        raise ValueError('Maestro4567 requires branch-scoped superseded-run cancellation')
    if 'python3 verification/check_repository.py\n' not in source or 'python3 verification/check_repository.py --self-test\n' not in source:
        raise ValueError('Safeguards not executed')
    if 'python3 verification/phase5_verify_download.py --self-test\n' not in source:
        raise ValueError('Phase 5 installed artifact self-test missing')
    if 'python3 verification/phase5_minecraft_log_gate.py --self-test\n' not in source:
        raise ValueError('Phase 5 installed Minecraft log gate self-test missing')
    if (re.search(r'(?m)^\s*continue-on-error\s*:\s*true\s*$', source)
            or re.search(r'(?m)^\s*if\s*:\s*(?:always\(\)|failure\(\))', source)):
        raise ValueError('Verified JAR cannot bypass failed simulator job')
    # Two complete pinned-source profiles can run concurrently, but a
    # release requires BOTH successful jobs and a verified intermediate JAR.
    native_start = source.find('  verify:\n')
    pack_start = source.find('  pack_source:\n')
    publish_start = source.find('  publish:\n')
    if not (0 <= native_start < pack_start < publish_start):
        raise ValueError('Verified JAR requires separate native, pack and publish jobs')
    native = source[native_start:pack_start]
    pack = source[pack_start:publish_start]
    publish = source[publish_start:]
    pin = 'actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02'
    download = 'actions/download-artifact@d3f86a106a0bac45b974a628896c90dbdf5c8093'
    required_native = ('timeout --signal=TERM --kill-after=30s 95m '
                       'python3 verification/source_oracle.py --native\n')
    required_pack = ('timeout --signal=TERM --kill-after=30s 48m '
                     'python3 verification/source_oracle.py --pack\n')
    for job_name, section in (('verify', native), ('pack_source', pack)):
        if (section.count('python3 verification/check_registration.py --self-test\n') != 1
                or section.count('python3 verification/check_registration.py\n') != 1):
            raise ValueError('Verification registration safeguard missing from ' + job_name)
    if (native.count('python3 verification/runtime_hook_oracle.py --self-test\n') != 1
            or native.count('python3 verification/runtime_hook_oracle.py --verify-pinned\n') != 1
            or 'python3 verification/check_native_split_hooks.py' in source):
        raise ValueError('Native ABI preflight or selector negative tests missing')
    for name, section in (('verify', native), ('pack_source', pack)):
        if section.count('for oracle in verification/*.cjs; do node --check "$oracle"; done\n') != 1:
            raise ValueError('Mandatory independent Node syntax and path gate missing in ' + name)
    # CI may accelerate JavaScript only in its source-verification children.
    # The production JAR build and standalone checks must retain Temurin 21.
    graal = (
        '      - name: Provision GraalVM 21 only for embedded JavaScript verification\n'
        '        uses: actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3\n'
        '        with:\n'
        '          distribution: graalvm-community\n'
        "          java-version: '21'\n"
    )
    isolated = (
        '          export MAESTRO_SOURCE_JAVA="$JAVA_HOME/bin/java"\n'
        '          export JAVA_HOME="$MAESTRO_BUILD_JAVA_HOME"\n'
        '          export PATH="$JAVA_HOME/bin:$PATH"\n'
        '          "$MAESTRO_SOURCE_JAVA" -version\n'
    )
    for name, job in (('native', native), ('pack', pack)):
        first_build = ('      - name: Build and run standalone regressions'
                       if name == 'native' else
                       '      - name: Prepare pinned Cobblemon dependency and Graal verification classes')
        source_start = ('      - name: Verify pinned native simulator source and relocated engine'
                        if name == 'native' else
                        '      - name: Verify pinned pack-source and relocated engine')
        if (job.count(graal) != 1 or job.count(isolated) != 1
                or job.count('echo "MAESTRO_BUILD_JAVA_HOME=$JAVA_HOME" >> "$GITHUB_ENV"') != 1
                or not (job.find(first_build) < job.find(graal)
                        < job.find(source_start))
                or 'distribution: temurin\n          java-version: \'21\'\n' not in job):
            raise ValueError('Verification-only GraalVM JDK must preserve Temurin build and full source execution')
    if 'distribution: graalvm-community' in publish or 'MAESTRO_SOURCE_JAVA' in publish:
        raise ValueError('Release job must not use the source-only GraalVM launcher')

    if (required_native not in native or required_pack not in pack
            or 'python3 verification/source_oracle.py --self-test\n' not in native
            or 'python3 verification/source_oracle.py --self-test\n' not in pack
            or 'source_oracle.py --pack' in native
            or 'source_oracle.py --native' in pack
            or 'timeout-minutes: 65\n' not in pack
            or 'remapJar verificationClasses --stacktrace\n' not in pack
            or 'python3 verification/check_repository.py\n' not in pack):
        raise ValueError('Verified JAR requires both full bounded source profiles')
    if (native.count(pin)!=1 or 'name: maestroai-candidate-${{ github.sha }}' not in native
            or '      - name: Upload intermediate JAR candidate (not release-approved)' not in native
            or native.count('if-no-files-found: error') != 1
            or native.count('path: build/verified-artifact/') != 1
            or 'cp "build/libs/maestroai-$VERSION.jar" build/verified-artifact/' not in native
            or 'sha256sum "maestroai-$VERSION.jar"' not in native
            or native.find(required_native) > native.find('Upload intermediate JAR candidate')
            or 'maestroai-verified-' in native or 'maestroai-verified-' in pack
            or 'actions/upload-artifact@' in pack):
        raise ValueError('Verified JAR candidate must be source-checked and non-release')
    if (not publish.startswith('  publish:\n    needs: [verify, pack_source]\n')
            or 'if: always()' in publish
            or publish.count(download)!=1 or publish.count(pin)!=1
            or 'name: maestroai-candidate-${{ github.sha }}' not in publish
            or 'name: maestroai-verified-${{ github.sha }}' not in publish
            or 'python3 verification/check_publish_artifact.py --self-test' not in publish
            or 'python3 verification/check_publish_artifact.py build/verified-artifact/' not in publish
            or publish.find('name: maestroai-candidate-${{ github.sha }}')
                > publish.find('python3 verification/check_publish_artifact.py')
            or publish.find('python3 verification/check_publish_artifact.py build/verified-artifact/')
                > publish.find('      - name: Upload verified Minecraft JAR')
            or 'if-no-files-found: error' not in publish
            or 'path: build/verified-artifact/' not in publish
            or re.search(r'(?m)^\s*run-id\s*:', publish)
            or re.search(r'(?m)^\s*repository\s*:', publish)
            or 'if: ' in publish
            or publish.count('      - name: Upload verified Minecraft JAR') != 1):
        raise ValueError('Verified JAR publication must depend on both full simulator jobs')


def check_wrapper(root):
    with zipfile.ZipFile(root / 'gradle/wrapper/gradle-wrapper.jar') as wrapper:
        if hashlib.sha256(wrapper.read('META-INF/LICENSE')).hexdigest() != APACHE_SHA256:
            raise ValueError('Gradle complete Apache license changed or missing')
    for path in ('gradlew', 'gradlew.bat'):
        header = (root / path).read_text()[:1500]
        if 'Copyright' not in header or 'SPDX-License-Identifier: Apache-2.0' not in header or 'https://www.apache.org/licenses/LICENSE-2.0' not in header:
            raise ValueError('Gradle launcher attribution missing')


class SafeguardRegression(unittest.TestCase):
    def test_actual_ignore_rules(self): check_ignore((ROOT / '.gitignore').read_text())
    def test_unprotected_local_secrets_rejected(self):
        with self.assertRaises(ValueError): check_ignore((ROOT / '.gitignore').read_text().replace('/.env.*', ''))
    def test_overbroad_jar_rule_rejected(self):
        with self.assertRaises(ValueError): check_ignore((ROOT / '.gitignore').read_text() + '\n*.jar\n')
    def test_overbroad_source_rule_rejected(self):
        with self.assertRaises(ValueError): check_ignore((ROOT / '.gitignore').read_text() + '\nsrc/\n')
    def test_both_events_cover_docs_notices_and_other_paths(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for event in ('push', 'pull_request'):
            for filter_name in ('paths', 'paths-ignore'):
                for path in ('src/**', 'README.md', 'LICENSE', '**/*.md', '!LICENSE'):
                    broken = source.replace('  ' + event + ':\n', '  ' + event + ':\n'
                                            + "    " + filter_name + ":\n      - '" + path + "'\n", 1)
                    with self.subTest(event=event, filter=filter_name, path=path), self.assertRaises(ValueError):
                        check_workflow(broken)
    def test_missing_or_wrong_branch_trigger_rejected(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        for event in ('push', 'pull_request'):
            for replacement in ('  unsupported_event:', '  ' + event + ':\n    paths: []'):
                with self.subTest(event=event), self.assertRaises(ValueError):
                    check_workflow(source.replace('  ' + event + ':', replacement, 1))
        with self.assertRaises(ValueError):
            check_workflow(source.replace('branches: [main, Maestro4567, Maestro-2.0]', 'branches: [other]'))
    def test_dedicated_integration_branch_cannot_be_skipped(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        broken = source.replace('branches: [main, Maestro4567, Maestro-2.0]',
                                'branches: [main, Maestro-2.0]')
        with self.assertRaisesRegex(ValueError, 'Maestro4567'):
            check_workflow(broken)

    def test_review_pr_branch_and_obsolete_branches_are_protected(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        with self.assertRaisesRegex(ValueError, 'review PR'):
            check_workflow(source.replace('    branches: [main, Maestro-2.0]',
                                          '    branches: [main]', 1))
        with self.assertRaisesRegex(ValueError, 'Obsolete'):
            check_workflow(source.replace('branches: [main, Maestro4567, Maestro-2.0]',
                                          'branches: [main, Maestro457, Maestro4567, Maestro-2.0]', 1))

    def test_superseded_maestro4567_ci_is_cancelled_but_exact_commit_published(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for before, after in (
                ("group: verify-${{ github.event.pull_request.number || github.ref }}",
                 "group: verify-${{ github.event.pull_request.number || github.sha }}"),
                ("github.ref == 'refs/heads/Maestro4567'",
                 "github.ref == 'refs/heads/not-the-integration-branch'"),
                ("cancel-in-progress:", "do-not-cancel:"),
                ("group: verify-${{ github.event.pull_request.number || github.ref }}",
                 "group: verify-shared")):
            with self.subTest(before=before), self.assertRaisesRegex(
                    ValueError, 'branch-scoped'):
                check_workflow(source.replace(before, after, 1))
        # Supersession must never turn release artifacts into branch-latest
        # downloads, or permit publish when one simulator job failed.
        for before, after in (
                ("maestroai-candidate-${{ github.sha }}", "maestroai-candidate-latest"),
                ("    needs: [verify, pack_source]", "    needs: [verify]")):
            with self.subTest(before=before), self.assertRaises(ValueError):
                check_workflow(source.replace(before, after, 1))

    def test_installed_artifact_self_test_required(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        with self.assertRaisesRegex(ValueError, 'artifact self-test'):
            check_workflow(source.replace(
                'python3 verification/phase5_verify_download.py --self-test\n', '', 1))

    def test_minecraft_log_evidence_gate_is_required(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        with self.assertRaisesRegex(ValueError, 'Minecraft log gate'):
            check_workflow(source.replace(
                'python3 verification/phase5_minecraft_log_gate.py --self-test\n', '', 1))

    def test_failed_jobs_and_other_run_artifacts_cannot_publish(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for extra in ('    continue-on-error: true\n',
                      '    if: always()\n',
                      '    if: failure()\n'):
            broken = source.replace('  publish:\n', '  publish:\n' + extra, 1)
            with self.subTest(extra=extra), self.assertRaisesRegex(ValueError, 'Verified JAR'):
                check_workflow(broken)
        for extra in ('          run-id: 123456\n',
                      '          repository: attacker/other\n'):
            marker = '          name: maestroai-candidate-${{ github.sha }}\n'
            publish_at = source.index('  publish:\n')
            broken = (source[:publish_at] +
                      source[publish_at:].replace(marker, marker + extra, 1))
            with self.subTest(extra=extra), self.assertRaisesRegex(ValueError, 'Verified JAR'):
                check_workflow(broken)

    def test_verification_only_graal_launcher_cannot_replace_build_or_release_gate(self):
        original = (ROOT / '.github/workflows/verify.yml').read_text()
        for old, new in (
                ('distribution: graalvm-community', 'distribution: temurin'),
                ('export JAVA_HOME="$MAESTRO_BUILD_JAVA_HOME"', 'export JAVA_HOME="$HOME"'),
                ('export MAESTRO_SOURCE_JAVA="$JAVA_HOME/bin/java"',
                 'export MAESTRO_SOURCE_JAVA="$HOME/unknown-java"'),
                ('echo "MAESTRO_BUILD_JAVA_HOME=$JAVA_HOME" >> "$GITHUB_ENV"',
                 'echo "MAESTRO_BUILD_JAVA_HOME=/tmp" >> "$GITHUB_ENV"')):
            self.assertIn(old, original)
            with self.subTest(change=old), self.assertRaisesRegex(
                    ValueError, 'Verification-only GraalVM JDK'):
                check_workflow(original.replace(old, new, 1))
        with self.assertRaisesRegex(ValueError, 'publication must depend'):
            check_workflow(original.replace(
                '    needs: [verify, pack_source]', '    needs: [verify]', 1))

    def test_exact_commit_jar_artifact_is_gated_by_simulator(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for old,new in (
                ('    needs: [verify, pack_source]', '    needs: [verify]'),
                ('  pack_source:', '  skipped_pack:'),
                ('python3 verification/source_oracle.py --native',
                 'python3 verification/source_oracle.py --skip'),
                ('python3 verification/source_oracle.py --pack',
                 'python3 verification/source_oracle.py --skip'),
                ('timeout --signal=TERM --kill-after=30s 95m', 'python3'),
                ('timeout --signal=TERM --kill-after=30s 48m', 'python3'),
                ('actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02',
                 'actions/upload-artifact@v4'),
                ('actions/download-artifact@d3f86a106a0bac45b974a628896c90dbdf5c8093',
                 'actions/download-artifact@v4'),
                ('name: maestroai-verified-${{ github.sha }}', 'name: unpinned'),
                ('name: maestroai-candidate-${{ github.sha }}', 'name: unpinned'),
                ('if-no-files-found: error','if-no-files-found: warn'),
                ('path: build/verified-artifact/', 'path: build/libs/'),
                ('cp "build/libs/maestroai-$VERSION.jar" build/verified-artifact/',
                 'echo missing-jar'),
                ('sha256sum "maestroai-$VERSION.jar"', 'echo missing-hash'),
                ('python3 verification/check_publish_artifact.py build/verified-artifact/',
                 'echo skip-verifier')):
            with self.subTest(old=old), self.assertRaisesRegex(ValueError,'Verified JAR'):
                check_workflow(source.replace(old,new,1))
        with self.assertRaisesRegex(ValueError,'Verified JAR'):
            check_workflow(source.replace(
                '  publish:\n    needs: [verify, pack_source]\n',
                '  publish:\n    if: always()\n    needs: [verify, pack_source]\n',1))
        with self.assertRaisesRegex(ValueError,'Verified JAR'):
            check_workflow(source.replace(
                '      - name: Upload verified Minecraft JAR',
                '      - name: Upload verified Minecraft JAR',1).replace(
                '      - name: Verify candidate integrity after both simulator profiles pass',
                '      - name: Skip candidate verification',1).replace(
                'python3 verification/check_publish_artifact.py build/verified-artifact/',
                'echo skip',1))

    def test_registration_checker_is_mandatory_in_both_jobs(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for command in ('python3 verification/check_registration.py --self-test\n',
                        'python3 verification/check_registration.py\n'):
            for start_marker, end_marker in (('  verify:\n', '  pack_source:\n'),
                                              ('  pack_source:\n', '  publish:\n')):
                start = source.index(start_marker)
                end = source.index(end_marker, start)
                changed = source[:start] + source[start:end].replace(command, '', 1) + source[end:]
                with self.subTest(command=command, job=start_marker), self.assertRaisesRegex(ValueError, 'registration'):
                    check_workflow(changed)
        with self.assertRaisesRegex(ValueError, 'Maestro-2.0'):
            check_workflow(source.replace('Maestro4567, Maestro-2.0]', 'Maestro4567]', 1))

    def test_native_mixin_preflight_and_negative_abi_tests_required(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for command in ('python3 verification/runtime_hook_oracle.py --self-test\n',
                        'python3 verification/runtime_hook_oracle.py --verify-pinned\n'):
            with self.subTest(command=command), self.assertRaisesRegex(ValueError, 'Native ABI'):
                check_workflow(source.replace(command, '', 1))

    def test_all_source_oracle_syntax_is_mandatory(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        command = 'for oracle in verification/*.cjs; do node --check "$oracle"; done\n'
        for start, end in (('  verify:\n', '  pack_source:\n'),
                           ('  pack_source:\n', '  publish:\n')):
            begin, stop = source.index(start), source.index(end)
            broken = source[:begin] + source[begin:stop].replace(command, '', 1) + source[stop:]
            with self.subTest(job=start), self.assertRaisesRegex(ValueError, 'Node syntax'):
                check_workflow(broken)

    def test_public_ci_permissions_and_private_data_boundary(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for candidate in (
            source.replace('contents: read', 'contents: write', 1),
            source.replace('persist-credentials: false', 'persist-credentials: true', 1),
            source + '\n# ${{ secrets.NOT_ALLOWED }}\n',
            source + '\npull_request_target:\n',
        ):
            with self.subTest(candidate=candidate[-50:]), self.assertRaisesRegex(ValueError, 'Public CI'):
                check_workflow(candidate)
        check_public_boundary(ROOT)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'verification').mkdir()
            (root / '.github/workflows').mkdir(parents=True)
            (root / 'README.md').write_text('public source only', encoding='utf-8')
            (root / '.github/workflows/verify.yml').write_text('public workflow', encoding='utf-8')
            (root / 'verification/README.md').write_text(
                'https://github.com/sensey12-mm/' + 'Headquarters', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'private-repository'):
                check_public_boundary(root)

    def test_benchmark_docs_keep_synthetic_fixture_and_exclude_handoff(self):
        check_benchmark_docs(ROOT)
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            folder = root / 'verification/benchmarks'
            folder.mkdir(parents=True)
            (root / 'README.md').write_text('# Public Maestro\\n', encoding='utf-8')
            (folder / 'README.md').write_text('# Offline benchmark\\n', encoding='utf-8')
            (folder / 'phase7_1_synthetic.json').write_text('{}', encoding='utf-8')
            check_benchmark_docs(root)
            handoff = folder / 'PHASE7_INTEGRATION_HANDOFF.md'
            handoff.write_text('should not be here', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'handoff reintroduced'):
                check_benchmark_docs(root)
            handoff.unlink()
            (folder / 'README.md').write_text('[broken](PHASE7_INTEGRATION_HANDOFF.md)', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'markdown references'):
                check_benchmark_docs(root)

    def test_wrapper_notice_retained(self): check_wrapper(ROOT)
    def test_missing_wrapper_notice_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root / 'gradle/wrapper').mkdir(parents=True)
            with zipfile.ZipFile(root / 'gradle/wrapper/gradle-wrapper.jar', 'w') as wrapper:
                wrapper.writestr('META-INF/LICENSE', 'incomplete')
            with self.assertRaises(ValueError): check_wrapper(root)


def check_benchmark_docs(root):
    """Keep runnable benchmark inputs public but private coordination off this branch."""
    folder = root / 'verification/benchmarks'
    if not (folder / 'phase7_1_synthetic.json').is_file():
        raise ValueError('Default synthetic Phase 7 benchmark fixture missing')
    if not (folder / 'README.md').is_file():
        raise ValueError('Public Phase 7 benchmark instructions missing')
    if (folder / 'PHASE7_INTEGRATION_HANDOFF.md').exists():
        raise ValueError('Phase 7 integration handoff reintroduced into public verification')
    for doc in (root / 'README.md', folder / 'README.md'):
        if 'PHASE7_INTEGRATION_HANDOFF.md' in doc.read_text(encoding='utf-8'):
            raise ValueError('Public markdown references removed Phase 7 handoff')


def main():
    if sys.argv[1:] == ['--self-test']:
        return not unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(SafeguardRegression)).wasSuccessful()
    check_ignore((ROOT / '.gitignore').read_text())
    check_workflow((ROOT / '.github/workflows/verify.yml').read_text())
    check_wrapper(ROOT)
    check_benchmark_docs(ROOT)
    check_public_boundary(ROOT)
    print('Repository safeguards and Gradle notices OK')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
