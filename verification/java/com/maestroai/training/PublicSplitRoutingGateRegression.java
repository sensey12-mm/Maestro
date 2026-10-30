package com.maestroai.training;

import java.util.*;

/** Phase 6.16 source-pinned split-routing privacy and custody regressions. */
final class PublicSplitRoutingGateRegression {
    private PublicSplitRoutingGateRegression(){}
    private static int checks;
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError("Phase 6.16 "+reason);
        checks++;
    }
    private static final String SECRET="PRIVATE_OPPONENT_UUID_AND_HP_123";
    private static List<String> original(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> add(List<String> prev,String... next){
        var x=new ArrayList<>(prev);
        Collections.addAll(x,next);
        return List.copyOf(x);
    }
    private static String split(String side,String privateLine,String publicLine){
        return "update\n|split|"+side+"\n"+privateLine+"\n"+publicLine;
    }
    private static final String PRIVATE_DAMAGE="|-damage|p1a: Own|56/100";
    private static final String PUBLIC_DAMAGE="|-damage|p1a: Own|60/100";
    private static PublicSplitRoutingGate.Report inspect(PublicSplitRoutingGate.Session s,
                                                          Object battle,List<String> history){
        return s.inspect(battle,history,true);
    }
    private static void deny(List<String> source,String name){
        var s=new PublicSplitRoutingGate.Session();
        var r=inspect(s,new Object(),source);
        check(r.verdict()==PublicSplitRoutingGate.Verdict.MISSING_OR_MALFORMED
                && !r.current() && s.withdrawn(),name+" fails closed");
        check(!r.toString().contains(SECRET)
                && !r.toString().contains("p1a")
                && !r.toString().contains("Own"),
                name+" never echoes actor/private source");
    }
    static void run(){
        Object battle=new Object();
        var original=original();
        var audit=new PublicSplitRoutingGate.Session();
        var direct=inspect(audit,battle,original);
        check(direct.verdict()==PublicSplitRoutingGate.Verdict.DIRECT_ONLY
                && direct.current() && direct.splitPairs()==0
                && direct.directEvents()==4 && direct.chunkCount()==4,
                "ordinary public frames remain source-scoped diagnostics only");
        check(!direct.publicReadinessLeaseAuthorized()
                && !direct.liveSourceDeliveryAuthenticated()
                && !direct.runtimeActorAuthenticated()
                && !direct.shadowSearchAuthorized()
                && !direct.tacticalDecisionAuthorized()
                && !direct.livePromotionAuthorized(),
                "no state promotes public custody to live authority");

        var one=add(original,split("p1",PRIVATE_DAMAGE,PUBLIC_DAMAGE));
        var r1=inspect(audit,battle,one);
        check(r1.verdict()==PublicSplitRoutingGate.Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                && r1.current() && r1.revision()==2
                && r1.splitPairs()==1 && r1.directEvents()==4 && r1.chunkCount()==5,
                "complete private-first, public-second pair source framing");
        check(r1.splitPublicExtractionStructurallyMatched()
                && !r1.liveSourceDeliveryAuthenticated()
                && !r1.publicReadinessLeaseAuthorized()
                && !r1.privatePayloadExportable()
                && !r1.completeMechanicsKnown(),
                "split match is structure ONLY, never custody or public-readiness proof");
        check(!r1.toString().contains(PRIVATE_DAMAGE)
                && !r1.toString().contains(PUBLIC_DAMAGE)
                && !r1.toString().contains(SECRET),
                "private and public originals never printed by reports");
        check(inspect(audit,battle,one).revision()==2
                && direct.current()==false,
                "identical poll is idempotent and superseded receipt expires");

        var two=add(one,split("p2",
                "|-heal|p2a: Opp|42/100",
                "|-heal|p2a: Opp|45/100"));
        var r2=inspect(audit,battle,two);
        check(r2.verdict()==PublicSplitRoutingGate.Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                && r2.splitPairs()==2 && r2.revision()==3
                && !r1.current(),"multiple complete routed events are append-only");
        var another=new PublicSplitRoutingGate.Session();
        check(inspect(another,battle,two).splitPairs()==2,
                "same complete source deterministically maps to same counts");
        check(!another.inspect(new Object(),two,true).current()
                && another.withdrawn(),
                "one session cannot switch physical battles");
        var wrongVersion=new PublicSplitRoutingGate.Session();
        var deniedPin=wrongVersion.inspect(battle,one,false);
        check(deniedPin.verdict()==PublicSplitRoutingGate.Verdict.BAD_SOURCE_PIN
                && wrongVersion.withdrawn(),
                "version drift disallows source assertions");
        check(wrongVersion.inspect(battle,one,true).verdict()
                ==PublicSplitRoutingGate.Verdict.WITHDRAWN,
                "source pin restoration cannot revive session");

        // 6.23: battle-opening native history contains two split switches,
        // then direct -ability/-boost before turn 1. A 6.16 parser that
        // rejects these already-allowlisted public events poisoned all real
        // runtime sources, even though independent provenance was available.
        // Synthetic identities only. No private owner logs become fixtures.
        var nativeOpening=List.of(
                split("p1","|switch|p1a: Own|Genesect, L100|80/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: Opp|Xerneas, L100|80/100",
                        "|switch|p2a: Opp|Xerneas, L100|100/100"),
                "update\n|-ability|p2a: Opp|Fairy Aura",
                "update\n|-boost|p2a: Opp|atk|1",
                "update\n|turn|1");
        var openSession=new PublicSplitRoutingGate.Session();
        var openReport=inspect(openSession,battle,nativeOpening);
        check(openReport.verdict()
                    ==PublicSplitRoutingGate.Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                && openReport.current() && openReport.splitPairs()==2
                && openReport.directEvents()==3 && openReport.chunkCount()==5
                && openReport.publicTurn()==1,
                "runtime-shaped turn-zero switch/ability/boost history is recognized");
        check(PublicProtocolFilter.publicEvents(nativeOpening).size()==5
                && PublicEnvelopeProvenance.reconcile(
                        nativeOpening,PublicProtocolFilter.publicEvents(nativeOpening)).complete(),
                "independent public filter agrees on exact opening event order");
        check(!openReport.liveSourceDeliveryAuthenticated()
                && !openReport.runtimeActorAuthenticated()
                && !openReport.shadowSearchAuthorized()
                && !openReport.tacticalDecisionAuthorized()
                && !openReport.livePromotionAuthorized(),
                "runtime-shaped opening does not certify provenance or tactics");
        var openNext=add(nativeOpening,
                "update\n|move|p1a: Own|Iron Head|p2a: Opp");
        check(inspect(openSession,battle,openNext).revision()==2,
                "normal append advances a source-bound revision, not proof");
        var openedRevoked=openSession.inspect(new Object(),openNext,true);
        check(openedRevoked.verdict()==PublicSplitRoutingGate.Verdict.CROSS_BATTLE
                && !openReport.current() && openSession.withdrawn(),
                "cross-battle callback after opening irreversibly withdraws");
        deny(add(nativeOpening,"update\n|-ability|p1a: Own|{"+SECRET+"}"),
                "ability value pretending to contain an opaque private object");
        deny(add(nativeOpening,"update\n|-boost|p2a: Opp|atk|99"),
                "out-of-range boosted stat level");
        deny(add(nativeOpening,"update\n|-boost|p2a: Opp|atk|1|[private] "+SECRET),
                "unrecognized boost annotation");
        deny(add(nativeOpening,"sideupdate\np1\n|request|"+SECRET),
                "native private request never recast as a public event");
        deny(add(nativeOpening,"update\n|-unboost|p1a: Own|atk|1"),
                "other unreviewed direct types remain unsupported");


        // Phase 6.26: the pinned native interpreter stores private sideupdate
        // requests/errors alongside public update chunks. Synthetic only:
        // source-shaped messages without ANY real user battle payloads.
        var sevenChunks=List.of(
                "sideupdate\np1\n|request|{\"active\":[]}",
                split("p1","|switch|p1a: Alpha|Genesect, L100|80/100",
                        "|switch|p1a: Alpha|Genesect, L100|100/100"),
                split("p2","|switch|p2a: Beta|Xerneas, L100|70/100",
                        "|switch|p2a: Beta|Xerneas, L100|100/100"),
                "update\n|-ability|p2a: Beta|Download|boost",
                "update\n|-boost|p2a: Beta|spa|1",
                "update\n|turn|1",
                "sideupdate\np2\n|request|{\"active\":[{\"moves\":[]}]}");
        var mixedSession=new PublicSplitRoutingGate.Session();
        var mixed=inspect(mixedSession,battle,sevenChunks);
        check(mixed.current() && mixed.verdict()
                    ==PublicSplitRoutingGate.Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                && mixed.splitPairs()==2 && mixed.directEvents()==3
                && mixed.chunkCount()==7 && mixed.publicTurn()==1,
                "real-shaped seven-chunk opening skips only private-side frames");
        var mixedPublic=PublicProtocolFilter.publicEvents(sevenChunks);
        var mixedProvenance=PublicEnvelopeProvenance.reconcile(
                sevenChunks,mixedPublic);
        check(mixedPublic.size()==5 && mixedProvenance.complete()
                && mixedProvenance.origins().size()==5
                && mixedProvenance.origins().get(0).route()
                    ==PublicEnvelopeProvenance.Route.SPLIT_PUBLIC
                && mixedProvenance.origins().get(1).route()
                    ==PublicEnvelopeProvenance.Route.SPLIT_PUBLIC
                && mixedProvenance.origins().get(2).route()
                    ==PublicEnvelopeProvenance.Route.DIRECT_UPDATE,
                "two independent public consumers ignore sideupdate bodies");
        check(!mixed.toString().contains("request")
                && !mixed.toString().contains("active")
                && !mixed.toString().contains(SECRET)
                && !mixed.privatePayloadExportable()
                && !mixed.liveSourceDeliveryAuthenticated()
                && !mixed.runtimeActorAuthenticated()
                && !mixed.publicReadinessLeaseAuthorized()
                && !mixed.shadowSearchAuthorized()
                && !mixed.tacticalDecisionAuthorized()
                && !mixed.livePromotionAuthorized(),
                "accepted private frame stays private and has no tactical authority");
        var expanded=add(sevenChunks,
                "update\n|-activate|p1a: Alpha|move: Substitute",
                "update\n|-sidestart|p1: Alpha|move: Stealth Rock",
                "update\n|-unboost|p2a: Beta|def|1",
                "update\n|-end|p1a: Alpha|move: Substitute|[silent]");
        var expandedReport=inspect(mixedSession,battle,expanded);
        check(expandedReport.current() && expandedReport.revision()==2
                && expandedReport.splitPairs()==2
                && expandedReport.directEvents()==7
                && expandedReport.chunkCount()==11
                && PublicEnvelopeProvenance.reconcile(expanded,
                        PublicProtocolFilter.publicEvents(expanded)).complete(),
                "narrow activation, side condition, unboost and end formats accepted");
        check(!expandedReport.liveSourceDeliveryAuthenticated()
                && !expandedReport.publicReadinessLeaseAuthorized()
                && !expandedReport.shadowSearchAuthorized()
                && !expandedReport.livePromotionAuthorized(),
                "newly recognized direct kinds never grant delivery or gameplay control");
        var privateAltered=new ArrayList<>(expanded);
        privateAltered.set(0,"sideupdate\np1\n|request|{\"active\":[1]}");
        var poisoned=inspect(mixedSession,battle,privateAltered);
        check(poisoned.verdict()==PublicSplitRoutingGate.Verdict.HISTORY_REWRITTEN
                && mixedSession.withdrawn() && !expandedReport.current(),
                "private-frame alteration still revokes salted history custody");
        deny(add(nativeOpening,"update\n|-ability|p2a: Opp|Download|boost|[silent]"),
                "literal ability mode cannot blend with arbitrary annotations");
        deny(add(nativeOpening,"update\n|-ability|p2a: Opp|Download|custom"),
                "unreviewed untagged ability mode rejected");
        deny(add(nativeOpening,"update\n|-activate|p1a: Own|{"+SECRET+"}"),
                "private object not admissible as public effect");
        deny(add(nativeOpening,"update\n|-sidestart|p3: Unlisted|move: Spikes"),
                "unsupported third side cannot be introduced as a public event");
        deny(add(nativeOpening,"update\n|-sidestart|p1: Own|{"+SECRET+"}"),
                "private source object not admissible as a side condition");
        deny(add(nativeOpening,"update\n|-unboost|p1a: Own|atk|99"),
                "stat magnitude out of supported bounds rejected");
        deny(add(nativeOpening,"update\n|-end|p1a: Own|move: Substitute|[private] "+SECRET),
                "unrecognized end annotation refused");
        deny(add(nativeOpening,"sideupdate\np3\n|request|{}"),
                "wrong native side target refused");
        deny(add(nativeOpening,"sideupdate\np1\n|move|p1a: Own|Secret"),
                "public-looking event inside private sideupdate refused");
        deny(add(nativeOpening,"sideupdate\np1\n|request|{}\nupdate\n|turn|2"),
                "embedded frame inside opaque private envelope refused");
        deny(add(nativeOpening,"sideupdate\np1\n|request|"),
                "empty private request refused");
        deny(add(nativeOpening,"sideupdate\np1\n|error|"),
                "empty private error refused");
        deny(add(nativeOpening,"update\n|request|{}"),
                "private request cannot masquerade as a public update");
        var benignPrivateError=add(nativeOpening,
                "sideupdate\np1\n|error|[Invalid choice] unsupported action");
        var errorReceipt=inspect(new PublicSplitRoutingGate.Session(),
                battle,benignPrivateError);
        check(errorReceipt.current() && errorReceipt.chunkCount()==6
                && errorReceipt.directEvents()==3
                && !errorReceipt.privatePayloadExportable(),
                "synthetic native private error skipped without exposing text");
        var sensitiveRequest=add(nativeOpening,
                "sideupdate\np1\n|request|{\"secret\":\""+SECRET+"\"}");
        var sensitiveReceipt=inspect(new PublicSplitRoutingGate.Session(),
                battle,sensitiveRequest);
        check(sensitiveReceipt.current()
                && !sensitiveReceipt.toString().contains(SECRET)
                && PublicProtocolFilter.publicEvents(sensitiveRequest).size()==5
                && PublicEnvelopeProvenance.reconcile(sensitiveRequest,
                        PublicProtocolFilter.publicEvents(sensitiveRequest)).complete(),
                "opaque valid private JSON never leaks into public events");

        // The independently implemented filter sees ONLY second/public split
        // lines. Phase 6.9 must *still* reject the entire private-bearing input.
        var filtered=PublicProtocolFilter.publicEvents(one);
        var independent=PublicEnvelopeProvenance.reconcile(one,filtered);
        check(independent.complete() && filtered.contains(PUBLIC_DAMAGE)
                && !filtered.contains(PRIVATE_DAMAGE)
                && independent.origins().getLast().route()
                    ==PublicEnvelopeProvenance.Route.SPLIT_PUBLIC,
                "existing filter/provenance agree on ordered public second half");
        var bridge=new ReadOnlyShadowObservationBridge();
        var obs=bridge.observe(battle,one,true);
        check(obs.state()==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && bridge.withdrawn() && bridge.publicLease(battle)==null,
                "6.9 bridge cannot turn split audit into public source lease");

        deny(add(original, "update\n|split|p1\n"+PRIVATE_DAMAGE),
                "unfinished private-only split");
        deny(add(original, "update\n|split|p1\n"+PRIVATE_DAMAGE+"\n"),
                "empty public line");
        deny(add(original, "update\n|split|p1\n\n"+PUBLIC_DAMAGE),
                "empty private line");
        deny(add(original, "update\n|split|p3\n"+PRIVATE_DAMAGE+"\n"+PUBLIC_DAMAGE),
                "unsupported p3 split target");
        deny(add(original, "update\n|split\n"+PRIVATE_DAMAGE+"\n"+PUBLIC_DAMAGE),
                "split without explicit target");
        deny(add(original, split("p1","|-damage|p1a: Own|65/100",
                "|-heal|p1a: Own|70/100")),
                "mismatched public/private kinds");
        deny(add(original, split("p1","|-damage|p2a: Opp|65/100",
                "|-damage|p2a: Opp|70/100")),
                "target side and actor side disagreement");
        deny(add(original, split("p1","|-damage|p1a: Own|65/100",
                "|-damage|p1b: Own|70/100")),
                "active slot identity mismatch");
        deny(add(original, split("p1","|-damage|p1a: PRIVATE_ALIAS|65/100",
                "|-damage|p1a: Own|70/100")),
                "private and public appearance not the same");
        deny(add(original, split("p1","|request|"+SECRET,
                PUBLIC_DAMAGE)),
                "private request is never a split mechanics half");
        deny(add(original, split("p1",PRIVATE_DAMAGE,
                "|request|"+SECRET)),
                "request cannot masquerade as public half");
        deny(add(original, split("p1",PRIVATE_DAMAGE,
                "|-damage|p1a: Own|PRIVATE_HP")),
                "public value malformed");
        deny(add(original, split("p1","|-damage|p1a: Own|PRIVATE_HP",
                PUBLIC_DAMAGE)),
                "private value malformed without exposure");
        deny(add(original, split("p1",PRIVATE_DAMAGE,
                "|-damage|p1a: Own|100/100|[hidden] "+SECRET)),
                "hidden/unknown annotation refused");
        deny(add(original, split("p1",PRIVATE_DAMAGE,
                "|-damage|p1a: Own|100/100\r")),
                "CRLF cannot create parser disagreement");
        deny(add(original, "sideupdate\np1\n|request|"+SECRET),
                "separate private sideupdate");
        deny(add(original, "update\n|split|p1\n"+PRIVATE_DAMAGE+"\n"
                +PUBLIC_DAMAGE+"\nsideupdate\n|request|"+SECRET),
                "private route embedded in complete split batch");
        deny(add(original,"update\n|split|p1\n"+PRIVATE_DAMAGE
                +"\n|split|p1\n"+PUBLIC_DAMAGE),
                "nested split in place of expected second half");
        deny(add(original,"update\n|split|p1\n"+PRIVATE_DAMAGE+"\n"
                +PUBLIC_DAMAGE+"\n|unknown|"+SECRET),
                "unexpected extra event after complete split");
        deny(add(original,"update\n|split|p1\n"+PRIVATE_DAMAGE+"\n"
                +PUBLIC_DAMAGE+"\n"),
                "trailing partial line");
        deny(add(original,"update\n|split|p1\n"
                +"|switch|p1a: Own|Genesect, L50|75/100\n"
                +"|switch|p1a: Own|Genesect, L50|75/100\n"
                +"|split|p2\n|-heal|p2a: Opp|50/100\n|-heal|p2a: Opp|60/100\n"
                +"|request|"+SECRET),
                "complete pairs plus trailing private event");

        var switchPair=add(original,split("p1",
                "|switch|p1a: Own|Genesect, L50|75/100",
                "|switch|p1a: Own|Genesect, L50|75/100"));
        check(inspect(new PublicSplitRoutingGate.Session(),battle,switchPair)
                .splitPairs()==1,"Cobblemon switch pair is allowed structurally");
        var dragPair=add(original,split("p2",
                "|drag|p2a: Opp|Zoroark, L50|90/100",
                "|drag|p2a: Opp|Zoroark, L50|100/100"));
        check(inspect(new PublicSplitRoutingGate.Session(),battle,dragPair)
                .splitPairs()==1,"drag pair shares source-verified split class");
        var hpPair=add(original,split("p1",
                "|-sethp|p1a: Own|51/100",
                "|-sethp|p1a: Own|60/100"));
        check(inspect(new PublicSplitRoutingGate.Session(),battle,hpPair)
                .splitPairs()==1,"sethp pair has strictly validated structural shape");

        // Syntactically identical halves do NOT tell which branch was
        // actually delivered, nor authenticate the original producer.
        var equal=add(original,split("p1",PUBLIC_DAMAGE,PUBLIC_DAMAGE));
        var equalReport=inspect(new PublicSplitRoutingGate.Session(),battle,equal);
        check(equalReport.splitPublicExtractionStructurallyMatched()
                && !equalReport.liveSourceDeliveryAuthenticated()
                && !equalReport.publicReadinessLeaseAuthorized(),
                "indistinguishable half-swaps can never establish custody");

        var tamper=new PublicSplitRoutingGate.Session();
        var first=inspect(tamper,battle,one);
        var altered=add(original,split("p1",
                "|-damage|p1a: Own|57/100",PUBLIC_DAMAGE));
        var changed=inspect(tamper,battle,altered);
        check(changed.verdict()==PublicSplitRoutingGate.Verdict.HISTORY_REWRITTEN
                && tamper.withdrawn() && !first.current(),
                "private-half edit invalidates salted append-only custody");
        check(tamper.inspect(battle,one,true).verdict()
                ==PublicSplitRoutingGate.Verdict.WITHDRAWN,
                "original source restoration cannot revive poisoned session");
        var truncated=new PublicSplitRoutingGate.Session();
        inspect(truncated,battle,one);
        check(inspect(truncated,battle,original).verdict()
                ==PublicSplitRoutingGate.Verdict.HISTORY_REWRITTEN,
                "history truncation rejected");
        var explicit=new PublicSplitRoutingGate.Session();
        var live=inspect(explicit,battle,one);
        explicit.withdraw();
        check(!live.current() && inspect(explicit,battle,one).verdict()
                ==PublicSplitRoutingGate.Verdict.WITHDRAWN,
                "explicit withdrawal erases report freshness");
        System.out.println("Phase 6.16 split-routing custody PASS: "
                +checks+" structural and adversarial checks");
    }
}
