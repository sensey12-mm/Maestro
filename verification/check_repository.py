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


def check_workflow(source):
    # Deliberately supports only the checked-in block layout, not arbitrary YAML.
    for event in ('push', 'pull_request'):
        match = re.search(r'(?m)^  ' + event + r':\n((?:^    .*\n|^      .*\n)+)', source)
        block = match.group(1) if match else ''
        branch_match = re.search(r'(?m)^    branches: \[([^\]\n]+)\]$', block)
        branches = [name.strip() for name in branch_match.group(1).split(',')] if branch_match else []
        if 'main' not in branches:
            raise ValueError('Missing main branch verification')
        if event == 'push' and 'Maestro-6' not in branches:
            raise ValueError('Missing Maestro-6 Phase 6 CI trigger')
        if event == 'push' and 'Maestro4567' not in branches:
            raise ValueError('Missing Maestro4567 integration CI trigger')
        # Required verify must be scheduled for every main PR/push, including
        # README-only changes. Workflow-level path skips leave required checks pending.
        if re.search(r'(?m)^ +(paths|paths-ignore):', block):
            raise ValueError('Path filters can strand required verification')
    # Stage 2's expensive pinned source oracles must never be blocked by
    # a DIFFERENT commit on the same integration branch. Keep full coverage.
    group = re.search(r'(?m)^  group: (.+)


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
            check_workflow(source.replace('branches: [main, Maestro457, Maestro-6, Maestro4567]', 'branches: [other]'))
    def test_dedicated_integration_branch_cannot_be_skipped(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        broken = source.replace('branches: [main, Maestro457, Maestro-6, Maestro4567]',
                                'branches: [main, Maestro457, Maestro-6]')
        with self.assertRaisesRegex(ValueError, 'Maestro4567'):
            check_workflow(broken)

    def test_integration_ci_isolates_exact_commit_runs(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        check_workflow(source)
        for old in ("github.ref == 'refs/heads/Maestro4567'",
                    "&& github.sha || 'shared'"):
            with self.subTest(concurrency=old), self.assertRaisesRegex(ValueError, 'SHA-isolated'):
                check_workflow(source.replace(old, "'shared'"))

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
    print('Repository safeguards and Gradle notices OK')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
, source)
    if (group is None
            or "github.ref == 'refs/heads/Maestro4567'" not in group.group(1)
            or "&& github.sha || 'shared'" not in group.group(1)):
        raise ValueError('Maestro4567 must use SHA-isolated CI concurrency')
    if 'python3 verification/check_repository.py\\n' not in source or 'python3 verification/check_repository.py --self-test\\n' not in source:
        raise ValueError('Safeguards not executed')


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
            check_workflow(source.replace('branches: [main, Maestro457, Maestro-6, Maestro4567]', 'branches: [other]'))
    def test_dedicated_integration_branch_cannot_be_skipped(self):
        source = (ROOT / '.github/workflows/verify.yml').read_text()
        broken = source.replace('branches: [main, Maestro457, Maestro-6, Maestro4567]',
                                'branches: [main, Maestro457, Maestro-6]')
        with self.assertRaisesRegex(ValueError, 'Maestro4567'):
            check_workflow(broken)

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
    print('Repository safeguards and Gradle notices OK')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
