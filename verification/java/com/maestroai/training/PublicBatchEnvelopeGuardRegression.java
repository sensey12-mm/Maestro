package com.maestroai.training;

import java.util.*;

/** Phase 6.15: complete direct update batches and split/private isolation. */
final class PublicBatchEnvelopeGuardRegression {
    private PublicBatchEnvelopeGuardRegression(){}
    private static int checks;
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError("Phase 6.15 "+why);
        checks++;
    }
    private static List<String> original(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> append(List<String> base,String... chunks){
        var r=new ArrayList<>(base);Collections.addAll(r,chunks);return List.copyOf(r);
    }
    private static void reject(List<String> frames,String why){
        Object battle=new Object();
        var b=new ReadOnlyShadowObservationBridge();
        var r=b.observe(battle,frames,true);
        check(r.state()==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && b.withdrawn() && b.publicLease(battle)==null,
                why+" fail-closed");
        check(!r.toString().contains("PRIVATE"),why+" no private diagnostics");
    }
    static void run(){
        Object battle=new Object();
        var history=original();
        var multi=append(history,
                "update\n|-status|p1a: Own|brn\n"
                +"|-damage|p1a: Own|70/100 brn\n"
                +"|-crit|p1a: Own\n"
                +"|-supereffective|p1a: Own\n"
                +"|-heal|p1a: Own|85/100 brn",
                "update\n|-curestatus|p1a: Own|brn\n"
                +"|-damage|p1a: Own|60/100\n"
                +"|upkeep");
        var b=new ReadOnlyShadowObservationBridge();
        var result=b.observe(battle,multi,true);
        check(result.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && result.publicTurn()==1 && result.publicEventCount()==12
                && result.revision()==1 && b.publicLease(battle)!=null,
                "multi-mechanics and common battle annotations accepted");
        var before=b.publicLease(battle);
        check(b.observe(battle,multi,true).revision()==1,
                "idempotent identical batched history");
        var later=append(multi,"update\n|turn|2\n"
                +"|move|p1a: Own|Tackle|p2a: Opp\n"
                +"|-miss|p1a: Own|p2a: Opp\n"
                +"|-fail|p1a: Own");
        var updated=b.observe(battle,later,true);
        check(updated.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && updated.publicTurn()==2 && updated.publicEventCount()==16
                && updated.revision()==2 && !before.current(),
                "turn, missed action and failure annotation retain ordering and freshness");
        check(b.publicLease(battle)!=null && !b.publicLease(battle).toString().contains("Own"),
                "public lease only exports non-private metadata");
        var annotated=PublicBatchEnvelopeGuard.inspect(multi);
        check(!annotated.narrowOnly() && annotated.allEvents()==12
                && annotated.directMechanics().size()==9
                && annotated.mechanicFrames().size()==9,
                "annotation doesn't manufacture an extra mechanics event");
        var plain=PublicBatchEnvelopeGuard.inspect(history);
        check(plain.narrowOnly() && plain.allEvents()==history.size(),
                "original Phase 6.7/6.8 single-event witness remains eligible");
        var groupedNarrow=append(history,
                "update\n|turn|2\n|move|p1a: Own|Tackle|p2a: Opp");
        check(!PublicBatchEnvelopeGuard.inspect(groupedNarrow).narrowOnly(),
                "grouped narrow actions are NOT a Phase 6.7 fixture");

        reject(append(history.subList(0,3),"update\n|-crit|p1a: Own"),
                "unbound critical-hit annotation without a preceding move");
        reject(append(history.subList(0,3),"update\n|-miss|p1a: Own|p2a: Opp"),
                "unbound miss annotation");
        reject(append(history,"update\n|upkeep\n|turn|2\n|-fail|p1a: Own"),
                "failure before a move in the current turn");
        reject(append(history,"update\n|-crit|p1a: Own\n|request|PRIVATE_REQUEST"),
                "unknown/private event after a good annotation");
        reject(append(history,"update\n|-crit|p1a: Own\n|-damage|p1a: Own|60/100\n"
                +"|-weather|RainDance"),"unmodeled public event never silently skipped");
        reject(append(history,"update\n|-status|p1a: Own|brn\n|-status|p1a: Own|psn"),
                "invalid status reentry inside one batch");
        reject(append(history,"update\n|-damage|p1a: Own|10/100\n|-heal|p1a: Own|5/100"),
                "HP movement contradiction inside batch");
        reject(append(history,"update\n|turn|2\n|turn|4"),
                "skipped public turn inside one batch");
        reject(append(history,"update\n|turn|2\n|move|p2a: PRIVATE_ALIAS|Tackle|p1a: Own"),
                "spoofed actor within multi event batch");
        reject(append(history,"update\n|move|p1a: Own|Tackle|p2a: Opp\n"
                +"|-crit|p1a: PRIVATE_ALIAS"),"annotation uses invented actor alias");
        reject(append(history,"update\n|move|p1a: Own|Tackle|p2a: Opp\n"
                +"|-fail|p1a: Own|PRIVATE|NESTED"),"unsupported annotation shape");
        reject(append(history,"update\n|move|p1a: Own|Tackle|p2a: Opp\n"
                +"|-crit|p1a: Own\n"),"trailing partial event");
        reject(append(history,"update\n|move|p1a: Own|Tackle|p2a: Opp\n"
                +"sideupdate\n|request|PRIVATE_TEAM"),"nested private routing");
        reject(append(history,"sideupdate\n|request|PRIVATE_TEAM"),
                "sideupdate in separate packet");
        reject(append(history,"update\n|split|p1\n"
                +"|-damage|p1a: Own|SECRET/100\n"
                +"|-damage|p1a: Own|30/100"),"split route, including second public half");
        reject(append(history,"update\n|split|p2\n"
                +"|-damage|p1a: Own|SECRET/100"),
                "incomplete split route");
        reject(append(history,"update\n|-crit|p1a: Own\n"
                +"|split|p1\n|-damage|p1a: Own|SECRET/100\n"
                +"|-damage|p1a: Own|30/100"),
                "split hidden inside otherwise ordinary annotation batch");
        reject(append(history,"update\n|-crit|p1a: Own\n"
                +"|-damage|p1a: Own|80/100\r\n"),"carriage returns fail closed");
        reject(append(history,"update\n|move|p1a: Own|Tackle|p2a: Opp\n"
                +"|split|p3"),"unsupported source split target");
        // Verify the old independent source-provenance parser's routing
        // contract: the first split payload is PRIVATE and second PUBLIC.
        // Merely extracting that public branch never permits our bridge to
        // attest delivery or completeness.
        var split=append(history,"update\n|split|p1\n"
                +"|-damage|p1a: Own|PRIVATE_HP\n"
                +"|-damage|p1a: Own|70/100");
        var filtered=PublicProtocolFilter.publicEvents(split);
        var audit=PublicEnvelopeProvenance.reconcile(split,filtered);
        check(audit.complete()
                && audit.origins().getLast().route()
                    ==PublicEnvelopeProvenance.Route.SPLIT_PUBLIC
                && !filtered.toString().contains("PRIVATE_HP"),
                "independent split provenance preserves PRIVATE first/PUBLIC second");
        var splitBridge=new ReadOnlyShadowObservationBridge();
        check(splitBridge.observe(new Object(),split,true).state()
                ==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && splitBridge.withdrawn(),
                "extracted public half never equals attested live source delivery");

        var issue=new ReadOnlyShadowObservationBridge();
        Object tracked=new Object();
        issue.observe(tracked,multi,true);
        var fork=append(history,"update\n|-status|p1a: Own|brn\n"
                +"|-damage|p1a: Own|71/100 brn\n"
                +"|-crit|p1a: Own\n"
                +"|-supereffective|p1a: Own\n"
                +"|-heal|p1a: Own|85/100 brn",
                "update\n|-curestatus|p1a: Own|brn\n"
                +"|-damage|p1a: Own|60/100\n"
                +"|upkeep");
        check(issue.observe(tracked,fork,true).state()
                ==ReadOnlyShadowObservationBridge.State.REPLAY_OR_REWRITE
                && issue.withdrawn(),
                "edit of accepted multi-batch bytes poisons public lease");
        // Illusion in a multi-event packet permits structural observation
        // only, never readiness of the hidden real party identity.
        var illusion=new ReadOnlyShadowObservationBridge();
        Object illBattle=new Object();
        var ill=append(history,"update\n|replace|p2a: Zoroark|Zoroark, L50\n"
                +"|turn|2\n|move|p2a: Zoroark|Scratch|p1a: Own");
        check(illusion.observe(illBattle,ill,true).state()
                ==ReadOnlyShadowObservationBridge.State.APPEARANCE_AMBIGUOUS
                && illusion.publicLease(illBattle)==null,
                "Illusion invalidates cross-stream readiness in a multi-event batch");

        System.out.println("Phase 6.15 batched-public envelope PASS: "+checks+" checks");
    }
}
