package com.maestroai.training;

import java.util.*;

/**
 * Offline-only compatibility audit for the Phase 5 public replay interfaces
 * against existing Shadow contracts. NOT a battle-state adapter, optimizer,
 * permission certificate, live policy, or runtime integration point.
 *
 * No raw replay text, player identifiers, replay IDs, hashes, action candidates,
 * legal moves, tactic scores, probabilities or per-battle confidence escape.
 */
public final class PublicCompetitiveShadowContract {
    private PublicCompetitiveShadowContract() {}

    public enum Blocker {
        INPUT_ABSENT_OR_INVALID,
        EXTERNALLY_CONSTRUCTED_RECORDS_NOT_AUTHENTICATED,
        REPLAY_PROVENANCE_MISMATCH,
        FORMAT_OR_VERSION_SCOPE_MISMATCH,
        APPEARANCE_OR_MOVE_EVIDENCE_MISMATCH,
        PUBLIC_SIDE_COUNT_INCONSISTENT,
        UNSUPPORTED_SOURCE_VERSION,
        NO_PUBLIC_APPEARANCE,
        HISTORICAL_REPLAY_NOT_ACTIVE_BATTLE,
        NO_FRESH_ACTOR_AND_TURN_BINDING,
        NO_CURRENT_PUBLIC_BATTLE_STATE,
        NO_OWN_REQUEST_LEGAL_ACTIONS,
        NO_CURRENT_RUNTIME_MECHANICS_RECEIPT,
        SHOWDOWN_RULES_NOT_COBBLEMON_VERIFIED,
        NO_CALIBRATED_OPPONENT_RESPONSE,
        REPLAY_PERMISSION_ATTESTATION_NOT_EXTERNAL_PROOF,
        REPLAY_ID_MAY_IDENTIFY_PUBLIC_PLAYER,
        HELD_OUT_AGGREGATES_NOT_PER_ACTOR
    }

    /** Deliberately cannot carry a candidate action or a permission to promote. */
    public record Audit(boolean sameUpstreamEvidence, boolean offlineSchemaCompatible,
                        boolean runtimeBindingAvailable, boolean shadowTacticalReady,
                        boolean livePromotionAuthorized, Set<Blocker> blockers) {
        public Audit {
            blockers=Set.copyOf(blockers);
            if (runtimeBindingAvailable || shadowTacticalReady || livePromotionAuthorized
                    || !blockers.contains(Blocker.HISTORICAL_REPLAY_NOT_ACTIVE_BATTLE)
                    || !blockers.contains(Blocker.NO_OWN_REQUEST_LEGAL_ACTIONS)
                    || !blockers.contains(Blocker.NO_CURRENT_RUNTIME_MECHANICS_RECEIPT))
                throw new IllegalArgumentException("Offline evidence cannot authorize battle actions");
        }
    }

    public static Audit audit(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies) {
        return audit(normalized,tendencies,null);
    }

    /**
     * Held-out scores are independent corpus-level audit metrics and never
     * evidence for a specific active Pokemon. This only audits shapes/receipts;
     * public record constructors are not cryptographic custody evidence.
     */
    public static Audit audit(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies,
                              PublicHeldOutEvaluation.Report heldOut) {
        EnumSet<Blocker> problems=EnumSet.of(
                Blocker.EXTERNALLY_CONSTRUCTED_RECORDS_NOT_AUTHENTICATED,
                Blocker.HISTORICAL_REPLAY_NOT_ACTIVE_BATTLE,
                Blocker.NO_FRESH_ACTOR_AND_TURN_BINDING,
                Blocker.NO_CURRENT_PUBLIC_BATTLE_STATE,
                Blocker.NO_OWN_REQUEST_LEGAL_ACTIONS,
                Blocker.NO_CURRENT_RUNTIME_MECHANICS_RECEIPT,
                Blocker.SHOWDOWN_RULES_NOT_COBBLEMON_VERIFIED,
                Blocker.NO_CALIBRATED_OPPONENT_RESPONSE,
                Blocker.REPLAY_PERMISSION_ATTESTATION_NOT_EXTERNAL_PROOF,
                Blocker.REPLAY_ID_MAY_IDENTIFY_PUBLIC_PLAYER,
                Blocker.HELD_OUT_AGGREGATES_NOT_PER_ACTOR);

        if (normalized==null || tendencies==null
                || normalized.provenance()==null || tendencies.provenance()==null
                || normalized.rules()==null || tendencies.rules()==null
                || normalized.targetSourceScope()==null || tendencies.targetSourceScope()==null
                || normalized.appearances()==null || tendencies.appearances()==null
                || tendencies.sides()==null) {
            problems.add(Blocker.INPUT_ABSENT_OR_INVALID);
            return result(false,false,problems);
        }
        boolean sourceMatched=validProvenance(normalized.provenance())
                && normalized.provenance().equals(tendencies.provenance());
        if (!sourceMatched) problems.add(Blocker.REPLAY_PROVENANCE_MISMATCH);
        boolean scopeMatched=normalized.rules().equals(tendencies.rules())
                && normalized.targetSourceScope().equals(tendencies.targetSourceScope())
                && validRules(normalized.rules())
                && normalized.provenance().replayId().startsWith("gen9"+normalized.rules().showdownTier()+"-")
                && !normalized.minecraftRuntimeAttested()
                && tendencies.interpretation()
                ==PublicCompetitiveTendencies.Interpretation.OBSERVATION_ONLY_NOT_FORECAST
                && !tendencies.forecastProduced();
        if (!scopeMatched) problems.add(Blocker.FORMAT_OR_VERSION_SCOPE_MISMATCH);
        boolean sourcePinned=normalized.targetSourceScope().equals(CobblemonMechanics.SOURCE);
        if (!sourcePinned) problems.add(Blocker.UNSUPPORTED_SOURCE_VERSION);
        if (normalized.appearances().isEmpty()) problems.add(Blocker.NO_PUBLIC_APPEARANCE);

        if (!sameAppearances(normalized.appearances(),tendencies.appearances()))
            problems.add(Blocker.APPEARANCE_OR_MOVE_EVIDENCE_MISMATCH);
        if (!sameSideCounts(tendencies.appearances(),tendencies.sides()))
            problems.add(Blocker.PUBLIC_SIDE_COUNT_INCONSISTENT);

        // An aggregate benchmark is not keyed to the individual replay or
        // actor/turn; a matching source scope cannot upgrade it to a prior.
        if (heldOut!=null
                && (!heldOut.sourceScope().equals(sourcePinned
                        ? CobblemonMechanics.SOURCE : "OUTSIDE_PINNED_SOURCE_SCOPE")
                    || heldOut.replayOverlapDetected()
                    || heldOut.crossReplayIdentityInferred()
                    || heldOut.trainedParametersShared()
                    || heldOut.forecastProbabilityClaimed())) {
            problems.add(Blocker.FORMAT_OR_VERSION_SCOPE_MISMATCH);
        }
        boolean shape=sourceMatched && scopeMatched && sourcePinned
                && !normalized.appearances().isEmpty()
                && !problems.contains(Blocker.APPEARANCE_OR_MOVE_EVIDENCE_MISMATCH)
                && !problems.contains(Blocker.PUBLIC_SIDE_COUNT_INCONSISTENT)
                && !problems.contains(Blocker.FORMAT_OR_VERSION_SCOPE_MISMATCH);
        return result(sourceMatched,shape,problems);
    }

    private static Audit result(boolean provenance, boolean shape, Set<Blocker> problems) {
        return new Audit(provenance,shape,false,false,false,problems);
    }

    private static boolean validProvenance(CompetitiveFormatNormalizer.Provenance p) {
        return p.replayId()!=null
                && p.replayId().matches("gen9(?:ou|ubers)-[1-9][0-9]{3,17}")
                && p.inputSha256()!=null && p.inputSha256().matches("[0-9a-f]{64}")
                && p.publicEvents()>0 && p.turns()>0
                && "UPSTREAM_OPERATOR_ATTESTATION_ONLY_NOT_INDEPENDENTLY_VERIFIED"
                        .equals(p.rightsVerification());
    }
    private static boolean validRules(CompetitiveFormatNormalizer.Rules r) {
        return r.generation()==9 && "singles".equals(r.gameType())
                && r.showdownTier()!=null && Set.of("ou","ubers").contains(r.showdownTier())
                && !r.completeShowdownRulesKnown()
                && r.cobblemonTierTransfer()==CompetitiveFormatNormalizer.Status.UNKNOWN;
    }

    private static boolean sameAppearances(List<CompetitiveFormatNormalizer.Appearance> normalized,
                                           List<PublicCompetitiveTendencies.Appearance> reported) {
        if (normalized.size()!=reported.size()) return false;
        for (int i=0;i<normalized.size();i++) {
            var a=normalized.get(i);
            var b=reported.get(i);
            if (a==null || b==null || a.slot()==null || b.slot()==null
                    || b.displayedSpecies()==null || a.displayed()==null || a.observedMoves()==null
                    || a.index()!=i+1 || b.index()!=a.index() || !a.slot().equals(b.slot())
                    || !a.displayed().id().equals(b.displayedSpecies())
                    || a.transformed()!=b.transformed()
                    || a.identityLinked() || !a.identityUncertain()
                    || b.identityLinked() || !b.identityUncertain()
                    || b.nativeUses()==null) return false;
            Map<String,CompetitiveFormatNormalizer.Token> observed=new HashMap<>();
            for (var move:b.nativeUses()) {
                if (move==null || move.token()==null || move.publicUses()<1
                        || observed.putIfAbsent(move.token().id(),move.token())!=null)
                    return false;
            }
            if (!a.observedMoves().equals(observed)) return false;
        }
        return true;
    }

    private static boolean sameSideCounts(List<PublicCompetitiveTendencies.Appearance> appearances,
                                          Map<String,PublicCompetitiveTendencies.Side> sides) {
        if (!sides.keySet().equals(Set.of("p1a","p2a"))) return false;
        for (String slot:List.of("p1a","p2a")) {
            var side=sides.get(slot);
            if (side==null || !slot.equals(side.slot()) || side.voluntarySwitchesKnown()) return false;
            int switches=0, drags=0, repeats=0;
            Set<String> seen=new HashSet<>();
            for (var entry:appearances) {
                if (entry==null || !slot.equals(entry.slot())) continue;
                if (entry.entry()==PublicCompetitiveTendencies.EntryKind.SWITCH_CAUSE_UNKNOWN)
                    switches++;
                else if (entry.entry()==PublicCompetitiveTendencies.EntryKind.DRAG_PUBLICLY_FORCED)
                    drags++;
                else return false;
                if (entry.displayedSpecies()==null) return false;
                if (!"unknown".equals(entry.displayedSpecies())
                        && !seen.add(entry.displayedSpecies())) repeats++;
            }
            if (side.switchAnnouncements()!=switches || side.forcedDragAnnouncements()!=drags
                    || side.repeatedDisplayCandidates()!=repeats) return false;
        }
        return true;
    }
}
