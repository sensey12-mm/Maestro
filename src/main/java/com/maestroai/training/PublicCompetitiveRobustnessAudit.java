package com.maestroai.training;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 5.12 — frozen, descriptive, held-out-only hypothesis robustness.
 *
 * A separately pinned protocol and split fingerprint are REQUIRED. The
 * caller-supplied registration is not an independently proven timestamp.
 * Evaluation revalidates complete signed custody and replays through Phase
 * 5.11; this class never fits hypotheses, tunes thresholds, or emits actions.
 */
public final class PublicCompetitiveRobustnessAudit {
    private PublicCompetitiveRobustnessAudit() {}

    public static final int VERSION=1;
    public static final int MIN_REPLAYS=PublicCompetitiveCoverageAudit.MIN_INDEPENDENT_REPLAYS;
    public static final int MIN_HORIZONS=PublicCompetitiveCoverageAudit.MIN_PAIRED_HORIZONS;
    public static final int MIN_ADVANTAGE_BASIS_POINTS=500;
    private static final String PROTOCOL_DESCRIPTION=
            "MAESTROAI-COMPETITIVE-PHASE5.12|v1|minecraft1.21.1|cobblemon1.7.3"
            +"|showdown-gen9ou+gen9ubers|p2a-only|held-out-whole-replays"
            +"|move=OBSERVED_FREQUENCY_VS_FIRST_NATIVE"
            +"|switch=OBSERVED_RETURN_PATTERN_VS_ALWAYS_MOVE"
            +"|uncertainty=PHASE5.11_EXCLUSIVE"
            +"|minimum-replays=3|minimum-horizons=30|minimum-gain-bps=500"
            +"|shift=OU90_UBERS10,OU50_UBERS50,OU10_UBERS90"
            +"|baseline=paired-pre-event|abstain-missing|no-fit|no-live";
    private static final String FINGERPRINT=sha256(PROTOCOL_DESCRIPTION);

    public enum Shift {
        OU_DOMINANT(90), BALANCED(50), UBERS_DOMINANT(10);
        private final int ouWeight;
        Shift(int weight) { ouWeight=weight; }
        public int ouWeight() { return ouWeight; }
        public int ubersWeight() { return 100-ouWeight; }
    }
    public enum Finding {
        ABSTAIN_INSUFFICIENT_STRATA,
        NO_OBSERVED_GAIN,
        OBSERVED_GAIN_BELOW_FROZEN_MARGIN,
        FROZEN_MARGIN_OBSERVED_ONLY
    }

    /** Only the TWO frozen candidates, never arbitrary post-hoc models. */
    public enum Hypothesis {
        OBSERVED_FREQUENCY_VS_FIRST_NATIVE,
        OBSERVED_RETURN_PATTERN_VS_ALWAYS_MOVE
    }

    /**
     * Caller must anchor BOTH digest values outside the current test corpus
     * prior to looking at results. A forged registration cannot be detected
     * solely from its publicly constructible fields.
     */
    public record Registration(int version,String protocolSha256,String testSplitSha256) {
        public Registration {
            if (version!=VERSION || protocolSha256==null || testSplitSha256==null
                    || !protocolSha256.matches("[0-9a-f]{64}")
                    || !testSplitSha256.matches("[0-9a-f]{64}")) throw invalid();
        }
    }
    public static String frozenProtocolSha256() { return FINGERPRINT; }
    public static String frozenProtocolDescription() { return PROTOCOL_DESCRIPTION; }

    public record Stratum(String format, PublicCompetitiveCoverageAudit.Action action,
                          PublicCompetitiveCoverageAudit.Uncertainty uncertainty,
                          int independentReplays,int pairedHorizons,
                          int candidateMinusBaseline,boolean enoughEvidence,
                          boolean frozenMarginMet) {
        public Stratum {
            if (!Set.of("gen9ou","gen9ubers").contains(format)
                    || action==null || uncertainty==null || independentReplays<0
                    || pairedHorizons<0 || Math.abs((long)candidateMinusBaseline)>pairedHorizons
                    || enoughEvidence!=(independentReplays>=MIN_REPLAYS
                        && pairedHorizons>=MIN_HORIZONS)
                    || frozenMarginMet!=(enoughEvidence
                        && (long)candidateMinusBaseline*10000
                        >= (long)MIN_ADVANTAGE_BASIS_POINTS*pairedHorizons))
                throw invalid();
        }
    }

    public record Stress(PublicCompetitiveCoverageAudit.Action action,
                         PublicCompetitiveCoverageAudit.Uncertainty uncertainty,
                         Shift shift, int ouPairedHorizons,int ubersPairedHorizons,
                         Finding finding) {
        public Stress {
            if (action==null || uncertainty==null || shift==null || finding==null
                    || ouPairedHorizons<0 || ubersPairedHorizons<0) throw invalid();
        }
        public boolean empiricalMarginMet() {
            return finding==Finding.FROZEN_MARGIN_OBSERVED_ONLY;
        }
    }

    public static final class Report {
        private final String protocolHash;
        private final String splitHash;
        private final List<Stratum> strata;
        private final List<Stress> stresses;
        private final boolean insufficientSupport,formatInstability,pooledMaskingRisk;
        private final int abstainedStressScenarios;
        private Report(String p,String s,List<Stratum> groups,List<Stress> stresses,
                       boolean insufficient,boolean unstable,boolean masking,int abstained) {
            if (!FINGERPRINT.equals(p) || s==null || !s.matches("[0-9a-f]{64}")
                    || groups.size()!=20 || stresses.size()!=30
                    || abstained<0 || abstained>30) throw invalid();
            protocolHash=p;splitHash=s;strata=List.copyOf(groups);
            this.stresses=List.copyOf(stresses);
            insufficientSupport=insufficient;
            formatInstability=unstable;
            pooledMaskingRisk=masking;
            abstainedStressScenarios=abstained;
        }
        public String protocolSha256() { return protocolHash; }
        public String testSplitSha256() { return splitHash; }
        public List<Stratum> strata() { return strata; }
        public List<Stress> stressScenarios() { return stresses; }
        public boolean insufficientSupport() { return insufficientSupport; }
        public boolean formatInstability() { return formatInstability; }
        public boolean pooledMaskingRisk() { return pooledMaskingRisk; }
        public int abstainedStressScenarios() { return abstainedStressScenarios; }
        public boolean observedStabilityAcrossAllRequiredShifts() {
            return !insufficientSupport && !formatInstability
                    && stresses.stream().allMatch(Stress::empiricalMarginMet)
                    && strata.stream().allMatch(Stratum::frozenMarginMet);
        }
        public boolean independentlyPreRegistered() { return false; }
        public boolean calibratedForecastProduced() { return false; }
        public boolean realWorldSkillVerified() { return false; }
        public boolean shadowTacticalReady() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "FrozenCompetitiveRobustness[descriptive-synthetic-only;live=false]";
        }
    }

    public static Report audit(List<PublicHeldOutEvaluation.Replay> split,
                               CompetitiveFormatNormalizer.Target target,
                               PublicHeldOutEvaluation.Report heldOut,
                               PublicSignedCustody.SignedManifest signed,
                               PublicSignedCustody.TrustedPolicy policy,
                               long evaluationTime,Registration pinned) {
        if (pinned==null || !FINGERPRINT.equals(pinned.protocolSha256())
                || split==null || target==null || heldOut==null
                || !pinned.testSplitSha256().equals(heldOut.splitSha256())
                || !target.equals(CompetitiveFormatNormalizer.Target.pinnedSource()))
            throw invalid();
        var coverage=PublicCompetitiveCoverageAudit.audit(
                split,target,heldOut,signed,policy,evaluationTime);
        if (!pinned.testSplitSha256().equals(coverage.splitHash())
                || coverage.groups().size()!=20 || coverage.crossReplayIdentityUsed())
            throw invalid();
        Map<String,Stratum> byGroup=new HashMap<>();
        var results=new ArrayList<Stratum>();
        boolean insufficient=false,unstable=false,masking=coverage.aggregateMaskingRisk();
        for(var g:coverage.groups()) {
            boolean enough=g.independentReplays()>=MIN_REPLAYS
                    && g.pairedHorizonCount()>=MIN_HORIZONS;
            int advantage=g.candidateHits()-g.baselineHits();
            boolean margin=enough && (long)advantage*10000
                    >= (long)MIN_ADVANTAGE_BASIS_POINTS*g.pairedHorizonCount();
            Stratum stratum=new Stratum(g.format(),g.action(),g.uncertainty(),
                    g.independentReplays(),g.pairedHorizonCount(),advantage,enough,margin);
            if(byGroup.put(key(g.format(),g.action(),g.uncertainty()),stratum)!=null)
                throw invalid();
            results.add(stratum);
            if(!enough) insufficient=true;
            if(!margin) masking|=coverage.aggregateMaskingRisk();
        }

        var stresses=new ArrayList<Stress>();
        int abstained=0;
        for(var action:PublicCompetitiveCoverageAudit.Action.values()) {
            for(var uncertainty:PublicCompetitiveCoverageAudit.Uncertainty.values()) {
                var ou=byGroup.get(key("gen9ou",action,uncertainty));
                var ubers=byGroup.get(key("gen9ubers",action,uncertainty));
                if(ou==null || ubers==null) throw invalid();
                boolean favorable=false,unfavorable=false;
                for(var shift:Shift.values()) {
                    Finding outcome=compare(ou,ubers,shift);
                    if(outcome==Finding.ABSTAIN_INSUFFICIENT_STRATA) abstained++;
                    else if(outcome==Finding.FROZEN_MARGIN_OBSERVED_ONLY) favorable=true;
                    else unfavorable=true;
                    stresses.add(new Stress(action,uncertainty,shift,
                            ou.pairedHorizons(),ubers.pairedHorizons(),outcome));
                }
                // Shift-sensitive wins are not robust. A good aggregate also
                // cannot override a weak individual format.
                if(favorable && unfavorable) unstable=true;
                if((ou.frozenMarginMet() && !ubers.frozenMarginMet())
                        || (!ou.frozenMarginMet() && ubers.frozenMarginMet()))
                    unstable=true;
                if((favorable || ou.frozenMarginMet() || ubers.frozenMarginMet())
                        && !(ou.frozenMarginMet() && ubers.frozenMarginMet()))
                    masking=true;
            }
        }
        return new Report(FINGERPRINT,coverage.splitHash(),results,stresses,
                insufficient,unstable,masking,abstained);
    }

    private static Finding compare(Stratum ou,Stratum uber,Shift shift) {
        if(!ou.enoughEvidence() || !uber.enoughEvidence())
            return Finding.ABSTAIN_INSUFFICIENT_STRATA;
        // Compare weighted PAIRED ADVANTAGE, not future-action probability.
        // BigInteger avoids overflow if event counts approach their int limit.
        BigInteger ouN=BigInteger.valueOf(ou.pairedHorizons());
        BigInteger uberN=BigInteger.valueOf(uber.pairedHorizons());
        BigInteger denominator=ouN.multiply(uberN).multiply(BigInteger.valueOf(100));
        BigInteger advantage=BigInteger.valueOf(shift.ouWeight())
                .multiply(BigInteger.valueOf(ou.candidateMinusBaseline()))
                .multiply(uberN).add(BigInteger.valueOf(shift.ubersWeight())
                .multiply(BigInteger.valueOf(uber.candidateMinusBaseline()))
                .multiply(ouN));
        if(advantage.signum()<=0) return Finding.NO_OBSERVED_GAIN;
        if(advantage.multiply(BigInteger.valueOf(10000)).compareTo(
                denominator.multiply(BigInteger.valueOf(MIN_ADVANTAGE_BASIS_POINTS)))<0)
            return Finding.OBSERVED_GAIN_BELOW_FROZEN_MARGIN;
        return Finding.FROZEN_MARGIN_OBSERVED_ONLY;
    }
    private static String key(String f,PublicCompetitiveCoverageAudit.Action a,
                              PublicCompetitiveCoverageAudit.Uncertainty u) {
        return f+"|"+a+"|"+u;
    }
    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 unavailable");
        }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified preregistered competitive evidence");
    }
}
