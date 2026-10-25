package com.maestroai.training;

import java.util.*;

/**
 * Versioned, deterministic OFFLINE custody gate over operator-supplied review decisions.
 *
 * The ledger is not a remote authority, signed document, trusted clock, or
 * independent permission verifier. A trusted caller must supply an up-to-date
 * revision floor, evaluate with an independently trusted time, and maintain
 * the full append-only decision history outside this package.
 */
public final class PublicReplayCustody {
    private PublicReplayCustody() {}
    public static final int VERSION=1;
    public static final long MAX_APPROVAL_SECONDS=366L*24*60*60;
    public enum Status { PENDING, APPROVED, REVOKED }

    /** No names, chat, credentials, raw replay content, or reviewer notes. */
    public record Decision(String replayId, String format, String inputSha256,
                           Status status, long decidedAtEpochSecond,
                           long expiresAtEpochSecond, String reviewDigest) {
        public Decision {
            if (replayId==null || format==null || inputSha256==null || status==null
                    || reviewDigest==null
                    || !Set.of("gen9ou","gen9ubers").contains(format)
                    || !replayId.matches("gen9(?:ou|ubers)-[1-9][0-9]{3,17}")
                    || !replayId.startsWith(format+"-")
                    || !inputSha256.matches("[a-f0-9]{64}")
                    || !reviewDigest.matches("[a-f0-9]{64}")
                    || decidedAtEpochSecond<=0
                    || (status==Status.APPROVED
                        && (expiresAtEpochSecond<=decidedAtEpochSecond
                        || expiresAtEpochSecond-decidedAtEpochSecond>MAX_APPROVAL_SECONDS))
                    || (status!=Status.APPROVED && expiresAtEpochSecond!=0))
                throw invalid();
        }
    }

    /**
     * An immutable append-only snapshot. Its revision equals its number of
     * decisions so a caller cannot simply bump a revision with no new event.
     * A REVOKED decision is terminal for that replay ID within this history.
     */
    public record Ledger(int schemaVersion,long revision,List<Decision> decisions) {
        public Ledger {
            if (schemaVersion!=VERSION || decisions==null
                    || decisions.isEmpty() || decisions.size()>10000
                    || revision!=decisions.size()) throw invalid();
            decisions=List.copyOf(decisions);
            var known=new HashMap<String,Decision>();
            long lastTime=0;
            for (Decision decision:decisions) {
                if (decision==null || decision.decidedAtEpochSecond()<=lastTime)
                    throw invalid();
                lastTime=decision.decidedAtEpochSecond();
                Decision previous=known.get(decision.replayId());
                if (previous!=null) {
                    if (!previous.format().equals(decision.format())
                            || !previous.inputSha256().equals(decision.inputSha256())
                            || previous.status()==Status.REVOKED
                            || previous.status()==decision.status()
                            || previous.status()==Status.APPROVED && decision.status()==Status.PENDING)
                        throw invalid();
                }
                known.put(decision.replayId(),decision);
            }
        }
    }

    /**
     * Opaque in-process reference; consumers only see a yes/no at the requested
     * revision and evaluation time. It is not a durable authorization token.
     */
    static final class Reference {
        final String replayId,format,inputSha256;
        Reference(PublicReplayCorpus.Source source,PublicReplayCorpus.Evidence evidence) {
            replayId=source.replayId();
            format=source.format();
            inputSha256=evidence.inputSha256();
        }
    }

    static Reference require(Ledger ledger,PublicReplayCorpus.Source source,
                             PublicReplayCorpus.Evidence evidence,
                             long evaluatedAtEpochSecond,long requiredRevision) {
        if (source==null || evidence==null
                || !source.replayId().equals(evidence.replayId())
                || !source.format().equals(evidence.format())
                || !source.publiclyListed()
                || source.rights()!=PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION)
            throw invalid();
        Reference ref=new Reference(source,evidence);
        require(ledger,ref,evaluatedAtEpochSecond,requiredRevision);
        return ref;
    }

    static void require(Ledger ledger,Reference reference,
                        long evaluatedAtEpochSecond,long requiredRevision) {
        if (ledger==null || reference==null || requiredRevision<1
                || evaluatedAtEpochSecond<=0
                || ledger.schemaVersion()!=VERSION
                || ledger.revision()<requiredRevision) throw invalid();
        Decision selected=null;
        for (Decision decision:ledger.decisions()) {
            if (decision.decidedAtEpochSecond()>evaluatedAtEpochSecond)
                throw invalid(); // No future-dated decisions in a purported current ledger.
            if (decision.replayId().equals(reference.replayId)) selected=decision;
        }
        if (selected==null || !selected.format().equals(reference.format)
                || !selected.inputSha256().equals(reference.inputSha256)
                || selected.status()!=Status.APPROVED
                || evaluatedAtEpochSecond>=selected.expiresAtEpochSecond())
            throw invalid();
    }

    static boolean eligible(Ledger ledger,List<Reference> refs,
                            long evaluatedAtEpochSecond,long requiredRevision) {
        if (refs==null || refs.isEmpty()) return false;
        try {
            for (Reference ref:refs)
                require(ledger,ref,evaluatedAtEpochSecond,requiredRevision);
            return true;
        } catch (IllegalArgumentException rejected) {
            return false;
        }
    }

    private static IllegalArgumentException invalid() {
        // No replay ID, secret data, permission notes or hash echoed in errors.
        return new IllegalArgumentException("Unverified offline custody evidence");
    }
}
