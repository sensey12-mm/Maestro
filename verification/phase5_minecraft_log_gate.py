#!/usr/bin/env python3
"""Read-only, redacted Phase 5 Minecraft diagnostic evidence classification.

This scans existing local log lines, never their full contents in output.
It CANNOT establish signed replay permission, CI provenance, Legacy action
equivalence, all four testing teams, or successful Minecraft acceptance.
"""
from pathlib import Path
import re
import sys
import unittest

MAX_LOG = 32 * 1024 * 1024
STARTUP = re.compile(r'\[MaestroAI\] Phase 5 historical operator=(PROVISIONED|UNPROVISIONED|ABSTAIN);authority=NONE(?:\s|$)')
BATTLE_LOG = '[MaestroAI] '
ROLE_REPORT = 'Phase5HistoricalRoleOverlay['
LIVE_REPORT = 'Phase5OnlineShadow['
# Exact source-level toString layout allows only categorical enum labels,
# not replay IDs, UUIDs, private messages or appended mock permissions.
HISTORICAL_FORMAT = re.compile(
    r'^Phase 5 historical Shadow Phase5HistoricalRoleOverlay\['
    r'verdict=[A-Z_]+;current=(true|false);'
    r'roles=\[(?:[A-Z_]+(?:, [A-Z_]+)*)?\];'
    r'presentPokemon=UNKNOWN;historicalJoin=UNBOUND;'
    r'forecast=NONE;authority=NONE\]$')


def inspect_lines(lines):
    startup = 'UNOBSERVED'
    native = live = historical = current_roles = 0
    unsafe = False
    # Never export raw identifiers, replay contents, keys, actor UUIDs
    # or opponent builds; only bounded categorical counters.
    for line in lines:
        if BATTLE_LOG not in line:
            continue
        message = line.split(BATTLE_LOG, 1)[1]
        match = STARTUP.search(line)
        if match:
            startup = match.group(1)
            continue
        if 'Phase 5 online Shadow ' in message:
            if LIVE_REPORT in message:
                if not (';identity=UNKNOWN;' in message
                        and ';historicalBuild=UNBOUND;' in message
                        and ';authority=NONE]' in message):
                    unsafe = True
                elif startup == 'PROVISIONED':
                    live = min(live + 1, 1_000_000)
            continue
        if 'Phase 5 historical Shadow ' in message:
            historical = min(historical + 1, 1_000_000)
            parsed = HISTORICAL_FORMAT.fullmatch(message.strip())
            if parsed is None:
                unsafe = True
                continue
            if parsed.group(1) == 'true' and 'roles=[]' not in message:
                if startup != 'PROVISIONED':
                    unsafe = True
                else:
                    current_roles = min(current_roles + 1, 1_000_000)
            continue
        if message.startswith('Phase 6.24 '):
            # This is only a Java diagnostic receipt. It does NOT establish
            # client delivery, packet ACK, or an authoritative Pokémon identity.
            if 'authority=NONE' in message and 'UNKNOWN' not in message:
                native = min(native + 1, 1_000_000)
    if unsafe:
        verdict = 'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC'
    elif startup != 'PROVISIONED':
        verdict = 'NO_PROVISIONED_ARCHIVE'
    elif not native or not live or not current_roles:
        verdict = 'INCOMPLETE_OBSERVATIONS'
    else:
        verdict = 'LOG_PATTERNS_OBSERVED'
    return dict(verdict=verdict, startup=startup, native_receipts=native,
                live_reports=live, historical_reports=historical,
                current_role_reports=current_roles)


def scan_file(path):
    try:
        if not path.is_absolute() or path.is_symlink() or not path.is_file():
            raise ValueError('file')
        if not 0 < path.stat().st_size <= MAX_LOG:
            raise ValueError('size')
        with path.open('r', encoding='utf-8', errors='strict') as stream:
            return inspect_lines(stream)
    except (OSError, UnicodeError, ValueError, RuntimeError):
        raise ValueError('Private Minecraft evidence unavailable') from None


def receipt(result):
    # Never claim an installed-game acceptance on the strength of a log alone.
    return ('Phase5MinecraftLog[verdict=' + result['verdict']
            + ';operator=' + result['startup']
            + ';nativeReceipts=' + str(result['native_receipts'])
            + ';liveReports=' + str(result['live_reports'])
            + ';historicalReports=' + str(result['historical_reports'])
            + ';currentHistoricalRoleReports=' + str(result['current_role_reports'])
            + ';sourceRun=UNVERIFIED;independentRights=UNVERIFIED'
            + ';fourTeams=UNVERIFIED;LegacyEquivalence=UNVERIFIED'
            + ';MinecraftAcceptance=NOT_ESTABLISHED;authority=NONE]')


class EvidenceGuards(unittest.TestCase):
    @staticmethod
    def line(s):
        return '[20:11:02] [Server thread/INFO] [STDOUT]: [MaestroAI] ' + s + '\n'

    def reports(self):
        return [
            self.line('Phase 5 historical operator=PROVISIONED;authority=NONE'),
            self.line('Phase 6.24 Phase624Sided[verdict=CURRENT;authority=NONE]'),
            self.line('Phase 5 online Shadow Phase5OnlineShadow[verdict=OBSERVED;identity=UNKNOWN;historicalBuild=UNBOUND;authority=NONE]'),
            self.line('Phase 5 historical Shadow Phase5HistoricalRoleOverlay[verdict=HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC;current=true;roles=[SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE];presentPokemon=UNKNOWN;historicalJoin=UNBOUND;forecast=NONE;authority=NONE]'),
        ]

    def test_fully_observed_is_not_game_acceptance(self):
        result = inspect_lines(self.reports())
        self.assertEqual(result['verdict'], 'LOG_PATTERNS_OBSERVED')
        self.assertIn('MinecraftAcceptance=NOT_ESTABLISHED', receipt(result))
        self.assertIn('independentRights=UNVERIFIED', receipt(result))

    def test_no_archive_always_abstains(self):
        lines = self.reports()
        lines[0] = self.line('Phase 5 historical operator=UNPROVISIONED;authority=NONE')
        self.assertEqual(inspect_lines(lines)['verdict'],
                         'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')
        lines[0] = self.line('Phase 5 historical operator=ABSTAIN;authority=NONE')
        self.assertEqual(inspect_lines(lines)['verdict'],
                         'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')
        self.assertEqual(inspect_lines([lines[0]])['verdict'],
                         'NO_PROVISIONED_ARCHIVE')

    def test_missing_receipts_does_not_pass(self):
        for i in (1, 2, 3):
            lines = self.reports()
            del lines[i]
            self.assertEqual(inspect_lines(lines)['verdict'],
                             'INCOMPLETE_OBSERVATIONS')

    def test_identity_binding_or_authority_is_rejected(self):
        for needle, altered in (
                ('historicalJoin=UNBOUND', 'historicalJoin=BOUND'),
                ('presentPokemon=UNKNOWN', 'presentPokemon=Zoroark'),
                ('forecast=NONE', 'forecast=0.8'),
                (';authority=NONE]', ';authority=LEGACY]')):
            lines = self.reports()
            lines[-1] = lines[-1].replace(needle, altered)
            self.assertEqual(inspect_lines(lines)['verdict'],
                             'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')

    def test_no_full_replay_or_player_name_in_report(self):
        result = inspect_lines(self.reports() +
                               ['Player SECRET_OWNER private sideupdate data\n'])
        out = receipt(result)
        self.assertNotIn('SECRET_OWNER', out)
        self.assertNotIn('sideupdate', out)
        self.assertNotIn('Zoroark', out)

    def test_appended_private_payload_and_fake_approval_rejected(self):
        for payload in (
                ';opponentUUID=SECRET_OWNER',
                ';replay=gen9ou-99887766',
                ';privateSideupdate=|sideupdate|p2|SECRET',
                ' PRIVATE_PLAYER',
        ):
            lines = self.reports()
            lines[-1] = lines[-1].rstrip('\n') + payload + '\n'
            result = inspect_lines(lines)
            self.assertEqual(result['verdict'],
                             'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')
            self.assertNotIn('SECRET', receipt(result))
            self.assertNotIn('gen9ou-', receipt(result))

    def test_forged_historical_legacy_field_rejected(self):
        lines = self.reports()
        lines[-1] = lines[-1].replace(
                ';authority=NONE]', ';authority=NONE;legacy=AUTHORIZED]')
        self.assertEqual(inspect_lines(lines)['verdict'],
                         'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')

    def test_old_logs_cannot_be_misrepresented(self):
        self.assertEqual(inspect_lines([
            self.line('Phase 6.22 Phase622Runtime[verdict=HOOK_UNOBSERVED;nativeReturns=0;authority=NONE]'),
            self.line('Phase 5 online Shadow=UNKNOWN;authority=NONE'),
        ])['verdict'], 'NO_PROVISIONED_ARCHIVE')

    def test_source_end_never_proves_legacy_equivalence(self):
        receipt_string = receipt(inspect_lines(self.reports()))
        self.assertIn('LegacyEquivalence=UNVERIFIED', receipt_string)
        self.assertNotIn('MinecraftAcceptance=PASS', receipt_string)


def main(args):
    if args == ['--self-test']:
        tests = unittest.defaultTestLoader.loadTestsFromTestCase(EvidenceGuards)
        return 0 if unittest.TextTestRunner().run(tests).wasSuccessful() else 1
    if len(args) != 1:
        print('Usage: python verification/phase5_minecraft_log_gate.py ABSOLUTE_MINECRAFT_LOG',
              file=sys.stderr)
        return 2
    try:
        print(receipt(scan_file(Path(args[0]))))
        return 0
    except ValueError:
        print('Phase5MinecraftLog[verdict=ABSTAIN;authority=NONE]', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
)


def inspect_lines(lines):
    startup = 'UNOBSERVED'
    native = live = historical = current_roles = 0
    unsafe = False
    # Never export raw identifiers, replay contents, keys, actor UUIDs
    # or opponent builds; only bounded categorical counters.
    for line in lines:
        if BATTLE_LOG not in line:
            continue
        message = line.split(BATTLE_LOG, 1)[1]
        match = STARTUP.search(line)
        if match:
            startup = match.group(1)
            continue
        if 'Phase 5 online Shadow ' in message:
            if LIVE_REPORT in message:
                if not (';identity=UNKNOWN;' in message
                        and ';historicalBuild=UNBOUND;' in message
                        and ';authority=NONE]' in message):
                    unsafe = True
                elif startup == 'PROVISIONED':
                    live = min(live + 1, 1_000_000)
            continue
        if 'Phase 5 historical Shadow ' in message:
            historical = min(historical + 1, 1_000_000)
            if ROLE_REPORT not in message or not all(x in message for x in (
                    ';presentPokemon=UNKNOWN;', ';historicalJoin=UNBOUND;',
                    ';forecast=NONE;', ';authority=NONE]')):
                unsafe = True
                continue
            if ';current=true;' in message and 'roles=[]' not in message:
                if startup != 'PROVISIONED':
                    unsafe = True
                else:
                    current_roles = min(current_roles + 1, 1_000_000)
            continue
        if message.startswith('Phase 6.24 '):
            # This is only a Java diagnostic receipt. It does NOT establish
            # client delivery, packet ACK, or an authoritative Pokémon identity.
            if 'authority=NONE' in message and 'UNKNOWN' not in message:
                native = min(native + 1, 1_000_000)
    if unsafe:
        verdict = 'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC'
    elif startup != 'PROVISIONED':
        verdict = 'NO_PROVISIONED_ARCHIVE'
    elif not native or not live or not current_roles:
        verdict = 'INCOMPLETE_OBSERVATIONS'
    else:
        verdict = 'LOG_PATTERNS_OBSERVED'
    return dict(verdict=verdict, startup=startup, native_receipts=native,
                live_reports=live, historical_reports=historical,
                current_role_reports=current_roles)


def scan_file(path):
    try:
        if not path.is_absolute() or path.is_symlink() or not path.is_file():
            raise ValueError('file')
        if not 0 < path.stat().st_size <= MAX_LOG:
            raise ValueError('size')
        with path.open('r', encoding='utf-8', errors='strict') as stream:
            return inspect_lines(stream)
    except (OSError, UnicodeError, ValueError, RuntimeError):
        raise ValueError('Private Minecraft evidence unavailable') from None


def receipt(result):
    # Never claim an installed-game acceptance on the strength of a log alone.
    return ('Phase5MinecraftLog[verdict=' + result['verdict']
            + ';operator=' + result['startup']
            + ';nativeReceipts=' + str(result['native_receipts'])
            + ';liveReports=' + str(result['live_reports'])
            + ';historicalReports=' + str(result['historical_reports'])
            + ';currentHistoricalRoleReports=' + str(result['current_role_reports'])
            + ';sourceRun=UNVERIFIED;independentRights=UNVERIFIED'
            + ';fourTeams=UNVERIFIED;LegacyEquivalence=UNVERIFIED'
            + ';MinecraftAcceptance=NOT_ESTABLISHED;authority=NONE]')


class EvidenceGuards(unittest.TestCase):
    @staticmethod
    def line(s):
        return '[20:11:02] [Server thread/INFO] [STDOUT]: [MaestroAI] ' + s + '\n'

    def reports(self):
        return [
            self.line('Phase 5 historical operator=PROVISIONED;authority=NONE'),
            self.line('Phase 6.24 Phase624Sided[verdict=CURRENT;authority=NONE]'),
            self.line('Phase 5 online Shadow Phase5OnlineShadow[verdict=OBSERVED;identity=UNKNOWN;historicalBuild=UNBOUND;authority=NONE]'),
            self.line('Phase 5 historical Shadow Phase5HistoricalRoleOverlay[verdict=HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC;current=true;roles=[SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE];presentPokemon=UNKNOWN;historicalJoin=UNBOUND;forecast=NONE;authority=NONE]'),
        ]

    def test_fully_observed_is_not_game_acceptance(self):
        result = inspect_lines(self.reports())
        self.assertEqual(result['verdict'], 'LOG_PATTERNS_OBSERVED')
        self.assertIn('MinecraftAcceptance=NOT_ESTABLISHED', receipt(result))
        self.assertIn('independentRights=UNVERIFIED', receipt(result))

    def test_no_archive_always_abstains(self):
        lines = self.reports()
        lines[0] = self.line('Phase 5 historical operator=UNPROVISIONED;authority=NONE')
        self.assertEqual(inspect_lines(lines)['verdict'],
                         'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')
        lines[0] = self.line('Phase 5 historical operator=ABSTAIN;authority=NONE')
        self.assertEqual(inspect_lines(lines)['verdict'],
                         'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')
        self.assertEqual(inspect_lines([lines[0]])['verdict'],
                         'NO_PROVISIONED_ARCHIVE')

    def test_missing_receipts_does_not_pass(self):
        for i in (1, 2, 3):
            lines = self.reports()
            del lines[i]
            self.assertEqual(inspect_lines(lines)['verdict'],
                             'INCOMPLETE_OBSERVATIONS')

    def test_identity_binding_or_authority_is_rejected(self):
        for needle, altered in (
                ('historicalJoin=UNBOUND', 'historicalJoin=BOUND'),
                ('presentPokemon=UNKNOWN', 'presentPokemon=Zoroark'),
                ('forecast=NONE', 'forecast=0.8'),
                (';authority=NONE]', ';authority=LEGACY]')):
            lines = self.reports()
            lines[-1] = lines[-1].replace(needle, altered)
            self.assertEqual(inspect_lines(lines)['verdict'],
                             'CONTRADICTORY_OR_UNSAFE_DIAGNOSTIC')

    def test_no_full_replay_or_player_name_in_report(self):
        result = inspect_lines(self.reports() +
                               ['Player SECRET_OWNER private sideupdate data\n'])
        out = receipt(result)
        self.assertNotIn('SECRET_OWNER', out)
        self.assertNotIn('sideupdate', out)
        self.assertNotIn('Zoroark', out)

    def test_old_logs_cannot_be_misrepresented(self):
        self.assertEqual(inspect_lines([
            self.line('Phase 6.22 Phase622Runtime[verdict=HOOK_UNOBSERVED;nativeReturns=0;authority=NONE]'),
            self.line('Phase 5 online Shadow=UNKNOWN;authority=NONE'),
        ])['verdict'], 'NO_PROVISIONED_ARCHIVE')

    def test_source_end_never_proves_legacy_equivalence(self):
        receipt_string = receipt(inspect_lines(self.reports()))
        self.assertIn('LegacyEquivalence=UNVERIFIED', receipt_string)
        self.assertNotIn('MinecraftAcceptance=PASS', receipt_string)


def main(args):
    if args == ['--self-test']:
        tests = unittest.defaultTestLoader.loadTestsFromTestCase(EvidenceGuards)
        return 0 if unittest.TextTestRunner().run(tests).wasSuccessful() else 1
    if len(args) != 1:
        print('Usage: python verification/phase5_minecraft_log_gate.py ABSOLUTE_MINECRAFT_LOG',
              file=sys.stderr)
        return 2
    try:
        print(receipt(scan_file(Path(args[0]))))
        return 0
    except ValueError:
        print('Phase5MinecraftLog[verdict=ABSTAIN;authority=NONE]', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
