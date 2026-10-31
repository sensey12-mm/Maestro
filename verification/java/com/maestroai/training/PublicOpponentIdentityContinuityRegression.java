package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.29 synthetic-only public opponent identity safety regression. */
final class PublicOpponentIdentityContinuityRegression {
    private PublicOpponentIdentityContinuityRegression(){}
    private static int checks;
    private static final String SECRET="PRIVATE_UUID_NEVER_EXPORT_629";
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.29: "+why);
        checks++;
    }
    private static String split(String target,String hidden,String shown){
        return "update\n|split|"+target+"\n"+hidden+"\n"+shown;
    }
    private static List<String> add(List<String> origin,String... chunks){
        var items=new ArrayList<>(origin);
        for(String chunk:chunks)items.add(chunk);
        return List.copyOf(items);
    }
    private static PublicOpponentIdentityContinuity.Report inspect(
            PublicOpponentIdentityContinuity.Session identity,
            PublicSplitRoutingGate.Session source,Object battle,List<String> history){
        return identity.inspect(battle,source.inspect(battle,history,true),history);
    }
    private static void noAuthority(PublicOpponentIdentityContinuity.Report r){
        check(!r.opponentSideAuthenticated()
                && !r.nativeActorIdentityAuthenticated()
                && !r.persistentPokemonIdentityConfirmed()
                && !r.samePokemonAcrossSwitchesProven()
                && !r.illusionOrTransformResolved()
                && !r.historicalReplayAppearanceJoined()
                && !r.legalActionSetProven()
                && !r.phase5OnlineAuthorized()
                && !r.privatePayloadExportable()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized(),
                "public appearance NEVER authenticates actor, historical build or gameplay");
        String redacted=r.toString();
        check(!redacted.contains(SECRET) && !redacted.contains("p1a:")
                && !redacted.contains("p2a:") && !redacted.contains("Zoroark")
                && !redacted.contains("Genesect")
                && !redacted.contains("java.lang.Object@")
                && redacted.contains("identity=UNBOUND")
                && redacted.contains("opponentActor=UNKNOWN")
                && redacted.contains("authority=NONE"),
                "only numeric/categorical metadata, no original identity or private bytes");
    }
    static void run() throws Exception {
        Object battle=new Object();
        var baseline=List.of(
                split("p1","|switch|p1a: Own|Genesect, L100|80/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: Opp|Greninja, L100|50/100",
                        "|switch|p2a: Opp|Greninja, L100|100/100"),
                "update\n|turn|1");
        var publicSource=new PublicSplitRoutingGate.Session();
        var session=new PublicOpponentIdentityContinuity.Session();
        var opening=inspect(session,publicSource,battle,baseline);
        check(opening.current()
                && opening.verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                && opening.currentPublicAppearanceObserved()
                && opening.publicP1Entries()==1 && opening.publicP2Entries()==1
                && opening.reusedActivePositions()==0
                && opening.publicTurn()==1 && opening.originalHistoryChunks()==3,
                "genuine initial source exposes p1/p2 active slots only");
        check(opening.uncertainty().contains(
                PublicOpponentIdentityContinuity.Uncertainty.PUBLIC_DISPLAY_ONLY)
                && !opening.persistentPokemonIdentityConfirmed()
                && opening.shadowOnlyUncertainty()
                    ==ShadowCompetitiveRuntimeBridge.IdentityEvidence.PUBLIC_DISPLAY_ONLY,
                "initial display cannot become persistent party identity");
        noAuthority(opening);
        var poll=publicSource.inspect(battle,baseline,true);
        check(poll.current() && poll.revision()==opening.sourceRevision(),
                "original Phase 6.16 source idempotent poll remains unchanged");
        var repeated=session.inspect(battle,poll,baseline);
        check(repeated.verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.REPLAY_OR_REGRESSION
                && session.withdrawn() && !opening.current(),
                "replayed same source receipt permanently withdraws consumer");

        var source2=new PublicSplitRoutingGate.Session();
        var observer2=new PublicOpponentIdentityContinuity.Session();
        var current=inspect(observer2,source2,battle,baseline);
        var switched=add(baseline,split("p2",
                "|switch|p2a: Opp|Zoroark, L100|75/100",
                "|switch|p2a: Opp|Zoroark, L100|100/100"));
        var reentry=inspect(observer2,source2,battle,switched);
        check(reentry.current() && !current.current()
                && reentry.verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.IDENTITY_AMBIGUOUS
                && reentry.reusedActivePositions()==1
                && reentry.publicP2Entries()==2
                && reentry.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY),
                "same p2a position reused does NOT prove same Pokémon returned");
        noAuthority(reentry);

        var revealed=add(switched,"update\n|replace|p2a: Opp|Zoroark, L100|100/100");
        var reveal=inspect(observer2,source2,battle,revealed);
        check(reveal.current() && !reentry.current()
                && reveal.replaceReveals()==1
                && reveal.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.ILLUSION_REVEAL_UNRESOLVED)
                && reveal.shadowOnlyUncertainty()
                    ==ShadowCompetitiveRuntimeBridge.IdentityEvidence
                        .ILLUSION_OR_TRANSFORM_UNRESOLVED,
                "Illusion public replace DOES NOT resolve historical actor identity");
        noAuthority(reveal);
        var transformHistory=add(revealed,
                "update\n|-transform|p2a: Opp|p1a: Own");
        var transformed=inspect(observer2,source2,battle,transformHistory);
        check(transformed.current() && transformed.transformEvents()==1
                && transformed.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.TRANSFORM_UNRESOLVED)
                && !reveal.current(),"Transform target never becomes stable Pokémon identity");
        noAuthority(transformed);
        var forms=add(transformHistory,
                "update\n|detailschange|p2a: Opp|Zoroark-Hisui, L100|100/100",
                "update\n|-formechange|p2a: Opp|Zoroark-Hisui",
                "update\n|-end|p2a: Opp|Illusion");
        var changed=inspect(observer2,source2,battle,forms);
        check(changed.current() && changed.formChangeEvents()==2
                && changed.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.FORM_CHANGE_UNRESOLVED)
                && changed.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.ILLUSION_REVEAL_UNRESOLVED),
                "permanent/temporary forms and Illusion end remain identity-unknown");
        noAuthority(changed);

        var drag=add(forms,split("p2",
                "|drag|p2a: Opp|Ditto, L100|50/100",
                "|drag|p2a: Opp|Ditto, L100|100/100"));
        var dragged=inspect(observer2,source2,battle,drag);
        check(dragged.current() && dragged.reusedActivePositions()==2
                && dragged.publicP2Entries()==3
                && dragged.verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.IDENTITY_AMBIGUOUS,
                "forced switching cannot connect distinct slot appearances");
        var multi=add(drag,split("p2",
                "|switch|p2b: Another|Blissey, L100|40/100",
                "|switch|p2b: Another|Blissey, L100|100/100"));
        var doubles=inspect(observer2,source2,battle,multi);
        check(doubles.current() && doubles.publicP2Entries()==4
                && doubles.uncertainty().contains(
                    PublicOpponentIdentityContinuity.Uncertainty.MULTI_ACTIVE_SLOT_UNLINKED),
                "two active positions cannot be collapsed into one identity");
        check(!observer2.battleEnded(new Object()) && doubles.current(),
                "late unrelated battle end cannot revoke the bound session");
        check(observer2.battleEnded(battle),
                "original physical battle end withdraws current continuity");
        check(!doubles.current() && !dragged.current()
                && observer2.inspect(battle,source2.inspect(battle,multi,true),multi)
                    .verdict()==PublicOpponentIdentityContinuity.Verdict.WITHDRAWN,
                "explicit lifecycle withdrawal terminal");

        // Both private and public tampering fail even with equal original size,
        // turn, event counts and displayed species. Original Phase 6.16 source
        // hashes are the custody anchor, not caller-created frame metadata.
        var matched=new PublicSplitRoutingGate.Session();
        var matchReceipt=matched.inspect(battle,baseline,true);
        var fake=new ArrayList<>(baseline);
        fake.set(1,split("p2","|switch|p2a: Opp|Greninja, L100|50/100",
                "|switch|p2a: Opp|Zoroark, L100|100/100"));
        var forged=new PublicOpponentIdentityContinuity.Session();
        var forgedReport=forged.inspect(battle,matchReceipt,fake);
        check(forgedReport.verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.SOURCE_HISTORY_MISMATCH
                && forged.withdrawn(),"forged public nickname/form cannot rehash source receipt");
        noAuthority(forgedReport);
        var privateChanged=new ArrayList<>(baseline);
        privateChanged.set(0,split("p1",
                "|switch|p1a: Own|Genesect, L100|70/100",
                "|switch|p1a: Own|Genesect, L100|100/100"));
        var privateForged=new PublicOpponentIdentityContinuity.Session();
        check(privateForged.inspect(battle,matchReceipt,privateChanged).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.SOURCE_HISTORY_MISMATCH
                && privateForged.withdrawn(),
                "private split-half alteration invalidates exact source custody");
        var battleChanged=new PublicOpponentIdentityContinuity.Session();
        check(battleChanged.inspect(new Object(),matchReceipt,baseline).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.WRONG_BATTLE
                && battleChanged.withdrawn(),
                "foreign physical battle cannot reuse authentic source");
        var missingAfterSource=new PublicOpponentIdentityContinuity.Session();
        var missingAfterOwner=new PublicSplitRoutingGate.Session();
        var sourceBeforeMissing=inspect(missingAfterSource,missingAfterOwner,
                battle,baseline);
        check(sourceBeforeMissing.current()
                && missingAfterSource.inspect(battle,null,baseline).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.MISSING_OR_STALE_SOURCE
                && missingAfterSource.withdrawn() && !sourceBeforeMissing.current(),
                "losing previously accepted source evidence revokes current receipt");
        var absent=new PublicOpponentIdentityContinuity.Session();
        check(absent.inspect(battle,null,baseline).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.MISSING_OR_STALE_SOURCE
                && !absent.withdrawn(),
                "missing original report abstains but never fabricates identity");
        var zero=new PublicOpponentIdentityContinuity.Session();
        var zeroSource=new PublicSplitRoutingGate.Session();
        var noEntry=List.of("update\n|turn|1");
        check(zero.inspect(battle,zeroSource.inspect(battle,noEntry,true),noEntry)
                    .verdict()==PublicOpponentIdentityContinuity.Verdict.UNSUPPORTED_PUBLIC_SEQUENCE
                && zero.withdrawn(),
                "turn-only stream cannot invent an opponent appearance");

        var oldReceiptSource=new PublicSplitRoutingGate.Session();
        var oldReceipt=oldReceiptSource.inspect(battle,baseline,true);
        var updated=add(baseline,"update\n|move|p2a: Opp|Scratch|p1a: Own");
        var staleSession=new PublicOpponentIdentityContinuity.Session();
        var old=staleSession.inspect(battle,oldReceipt,baseline);
        oldReceiptSource.inspect(battle,updated,true);
        check(!old.current()
                && staleSession.inspect(battle,oldReceipt,baseline).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.MISSING_OR_STALE_SOURCE
                && staleSession.withdrawn(),
                "advanced source revision expires prior continuity report");
        var invalidVersion=new PublicSplitRoutingGate.Session();
        var wrongPin=invalidVersion.inspect(battle,baseline,false);
        var wrongPinObserver=new PublicOpponentIdentityContinuity.Session();
        check(wrongPinObserver.inspect(battle,wrongPin,baseline).verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.MISSING_OR_STALE_SOURCE
                && wrongPinObserver.withdrawn(),
                "Cobblemon version-pin mismatch cannot become identity evidence");

        // Phase 6.28 remains preparation-only. A Phase 6.29 report cannot
        // bless an actor to bypass Phase 5's offline-only consumer contract.
        var offline=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(offline.purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                "existing Phase 5 offline-only purpose unchanged");
        String ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int chooserStart=ai.indexOf("public ShowdownActionResponse choose(");
        int chooserEnd=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        check(chooserStart>=0 && chooserEnd>chooserStart,
                "source-level live chooser boundary located");
        String chooser=ai.substring(chooserStart,chooserEnd);
        check(chooser.contains("LivePromotionGate.choose(")
                && chooser.contains("null, null, false);")
                && !chooser.contains("PublicOpponentIdentityContinuity.")
                && !chooser.contains("ShadowCompetitiveRuntimeBridge.")
                && !chooser.contains("PublicCompetitiveLiveShadowConsumer.")
                && !ai.contains("PublicShadowBuildHandoff.prepare("),
                "actual live chooser cannot consume Phase 5/6 diagnostics");
        System.out.println("Phase 6.29 public opponent continuity PASS: "
                +checks+" synthetic/source checks; Minecraft UNTESTED");
    }
}
