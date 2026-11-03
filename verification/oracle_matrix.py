#!/usr/bin/env python3
"""Declared external simulator oracles; preserve independent profiles and order."""
from pathlib import Path
import unittest

NATIVE_STEPS = (
    ("source_oracle.cjs",),
    ("move_attestation_oracle.cjs",),
    ("blocked_hit_oracle.cjs",),
    ("healing_recoil_oracle.cjs",),
    ("recoil_continuation_oracle.cjs",),
    ("pivot_outcome_oracle.cjs",),
    ("modifier_composition_oracle.cjs",),
    ("accuracy_miss_oracle.cjs",),
    ("residual_recovery_oracle.cjs",),
    ("sandstorm_oracle.cjs",),
    ("weather_suppression_oracle.cjs",),
    ("weather_speed_oracle.cjs",),
    ("utility_umbrella_oracle.cjs",),
    ("weather_ability_oracle.cjs",),
    ("roster_ability_oracle.cjs",),
    ("download_entry_oracle.cjs",),
    ("weather_entry_oracle.cjs",),
    ("weather_entry_oracle.cjs", "--setters"),
    ("own_mega_y_oracle.cjs",),
    ("own_z_move_oracle.cjs",),
    ("own_reentry_oracle.cjs",),
    ("cold_weather_oracle.cjs",),
    ("toxic_entry_oracle.cjs",),
    ("status_speed_oracle.cjs",),
    ("psychic_terrain_oracle.cjs",),
)
PACK_STEPS = (
    ("addon_reload_oracle.cjs",),
    ("raid_condition_oracle.cjs",),
    ("pack_item_reload_oracle.cjs",),
    ("runtime_cache_oracle.cjs",),
    ("method_provenance_oracle.cjs",),
    ("source_oracle.cjs",),
)


def commands_for(profile, simulator):
    if profile not in ('native', 'pack') or not str(simulator):
        raise ValueError('Unsupported or empty pinned simulator profile')
    steps = NATIVE_STEPS if profile == 'native' else PACK_STEPS
    for entry in steps:
        name, *flags = entry
        if not name.endswith('.cjs') or '/' in name or any(x != '--setters' for x in flags):
            raise ValueError('Unsafe source-oracle command')
        yield ['node', 'verification/' + name, str(simulator), *flags]


class OracleMatrixGuards(unittest.TestCase):
    def test_profiles_stay_independent_and_complete(self):
        self.assertEqual((len(NATIVE_STEPS), len(PACK_STEPS)), (25, 6))
        self.assertEqual(NATIVE_STEPS.count(('weather_entry_oracle.cjs', '--setters')), 1)
        self.assertEqual(NATIVE_STEPS.count(('weather_entry_oracle.cjs',)), 1)
        self.assertEqual(len(list(commands_for('native', '/disposable-native'))), 25)
        self.assertEqual(len(list(commands_for('pack', '/disposable-pack'))), 6)
        with self.assertRaises(ValueError): list(commands_for('skip', '/fixture'))
        with self.assertRaises(ValueError): list(commands_for('native', ''))
