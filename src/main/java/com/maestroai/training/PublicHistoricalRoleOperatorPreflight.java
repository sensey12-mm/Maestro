package com.maestroai.training;

import java.time.Instant;
import java.util.Properties;
import java.util.function.LongSupplier;

/**
 * Read-only Phase 5 operator archive preflight.
 *
 * This reports only the local, source-level readiness of externally provisioned
 * historical role diagnostics. It does not attest real replay-use permission,
 * a remote clock, a Minecraft callback, a battle outcome or Shadow authority.
 * No archive, keyring, replay contents or local paths are printed.
 */
public final class PublicHistoricalRoleOperatorPreflight {
    private PublicHistoricalRoleOperatorPreflight(){}
    public enum Verdict {
        OBSERVER_DISABLED,
        ARCHIVE_UNPROVISIONED,
        CUSTODY_OR_ARCHIVE_REJECTED,
        SOURCE_READY_FOR_MINECRAFT_TEST
    }
    public record Report(Verdict verdict, boolean minecraftAccepted,
                         boolean liveBattleEvidenceSeen,
                         boolean independentReplayRightsAttested,
                         boolean shadowTacticalAuthority) {
        public Report {
            if(verdict==null || minecraftAccepted || liveBattleEvidenceSeen
                    || independentReplayRightsAttested || shadowTacticalAuthority)
                throw new IllegalArgumentException("Unsupported Phase 5 preflight authority");
        }
        public boolean locallyProvisioned(){
            return verdict==Verdict.SOURCE_READY_FOR_MINECRAFT_TEST;
        }
        @Override public String toString(){
            return "Phase5HistoricalPreflight[verdict="+verdict
                    +";minecraft=UNTESTED;replayRights=OPERATOR_REVIEW_REQUIRED"
                    +";historicalIdentity=UNBOUND;authority=NONE]";
        }
    }

    /** Does not mutate world state, install into a chooser or cache trust tokens. */
    public static Report inspect(Properties settings,LongSupplier operatorClock){
        if(settings==null || operatorClock==null)
            throw new IllegalArgumentException("Phase 5 preflight arguments unavailable");
        if(!"true".equals(settings.getProperty("maestroai.phase5HistoricalRolesObserve")))
            return new Report(Verdict.OBSERVER_DISABLED,false,false,false,false);
        try{
            var configured=PublicHistoricalRoleOperatorActivation.fromProperties(
                    settings,operatorClock);
            if(configured==null)
                return new Report(Verdict.ARCHIVE_UNPROVISIONED,false,false,false,false);
            // Fresh check immediately after prepare; no cached startup success.
            if(configured.trusted().latest()==null
                    || configured.archive().currentOpponentIdentified()
                    || configured.archive().shadowTacticalAuthorized()
                    || configured.archive().phase5OnlineBuildAuthorized())
                return new Report(Verdict.CUSTODY_OR_ARCHIVE_REJECTED,
                        false,false,false,false);
            return new Report(Verdict.SOURCE_READY_FOR_MINECRAFT_TEST,
                    false,false,false,false);
        }catch(RuntimeException rejected){
            // Error text may contain local paths, so never propagate it to
            // stdout or the game diagnostic channel.
            return new Report(Verdict.CUSTODY_OR_ARCHIVE_REJECTED,
                    false,false,false,false);
        }
    }
    /** Uses the same JVM properties as the installed operator path. */
    public static void main(String[] arguments){
        if(arguments.length!=0){
            System.out.println("Phase5HistoricalPreflight[verdict=CUSTODY_OR_ARCHIVE_REJECTED;authority=NONE]");
            System.exit(2);
            return;
        }
        var result=inspect(System.getProperties(),()->Instant.now().getEpochSecond());
        System.out.println(result);
        if(!result.locallyProvisioned())System.exit(2);
    }
}
