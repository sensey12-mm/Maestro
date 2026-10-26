package com.maestroai.training;

import java.util.*;

/** Phase 6.14: directly routed public protocol coverage and privacy attacks. */
final class ExpandedPublicEventGuardRegression {
    private ExpandedPublicEventGuardRegression(){}
    private static int checks;
    private static void check(boolean yes,String name){
        if(!yes)throw new AssertionError("Phase 6.14 "+name);
        checks++;
    }
    private static List<String> history(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> with(List<String> base,String... events){
        var copy=new ArrayList<>(base);
        for(String event:events)copy.add("update\n"+event);
        return List.copyOf(copy);
    }
    private static void reject(List<String> frames,String name){
        Object battle=new Object();
        var bridge=new ReadOnlyShadowObservationBridge();
        var result=bridge.observe(battle,frames,true);
        check(result.state()==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && bridge.withdrawn() && bridge.publicLease(battle)==null,
                "unsafe event denied: "+name);
        check(!result.toString().contains("PRIVATE"),"redacted "+name);
    }
    static void run(){
        var base=history();
        var extended=with(base,
                "|-status|p1a: Own|brn",
                "|-damage|p1a: Own|90/100 brn",
                "|-heal|p1a: Own|95/100 brn",
                "|-curestatus|p1a: Own|brn",
                "|-damage|p1a: Own|70/100");
        Object battle=new Object();
        var bridge=new ReadOnlyShadowObservationBridge();
        var first=bridge.observe(battle,extended,true);
        check(first.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && first.publicTurn()==1 && first.publicEventCount()==9
                && first.revision()==1 && bridge.publicLease(battle)!=null,
                "status, damage and healing added without authority");
        check(bridge.observe(battle,extended,true).revision()==1,
                "duplicate polling does not add revisions");
        var prefix=bridge.publicLease(battle);
        var second=with(extended,"|-damage|p1a: Own|50/100");
        check(bridge.observe(battle,second,true).publicEventCount()==10
                && !prefix.current(),
                "append-only public damage expires previous evidence lease");
        var turn2=with(second,"|turn|2","|move|p1a: Own|Tackle|p2a: Opp");
        check(bridge.observe(battle,turn2,true).publicTurn()==2,
                "continuous next turn accepted");
        check(bridge.publicLease(battle)!=null
                && !bridge.publicLease(battle).toString().contains("Own"),
                "expanded public lease carries no raw private data");
        reject(with(base,"|-curestatus|p1a: Own|brn"),
                "cure without observed status history");
        reject(with(base,"|-status|p1a: Own|PRIVATE_SECRET"),
                "unrecognized status code");
        reject(with(base,"|-status|p1a: Own|brn","|-status|p1a: Own|psn"),
                "status override missing cure");
        reject(with(base,"|-damage|p1a: Own|101/100"),
                "invalid health fraction");
        reject(with(base,"|-damage|p1a: Own|110/110"),
                "changed observed maximum within appearance");
        reject(with(base,"|-damage|p1a: Own|120/100"),
                "HP above maximum");
        reject(with(base,"|-damage|p1a: Own|100/100|PRIVATE_REQUEST"),
                "unknown private trailing tag");
        reject(with(base,"|-damage|p1a: Own|20/100","|-heal|p1a: Own|10/100"),
                "healing decreases HP");
        reject(with(base,"|-damage|p1a: Own|20/100","|-damage|p1a: Own|60/100"),
                "damage increases HP");
        reject(with(base,"|-damage|p1a: Own|0/100 fnt",
                "|move|p1a: Own|Tackle|p2a: Opp"),
                "fainted actor cannot perform new move");
        var faint=with(base,"|-damage|p1a: Own|0/100 fnt","|faint|p1a: Own");
        check(new ReadOnlyShadowObservationBridge().observe(new Object(),faint,true).state()
                ==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE,
                "ordered zero HP and faint accepted");
        reject(with(base,"|faint|p1a: Own"),
                "faint before a witnessed zero HP");
        reject(with(base,"|-damage|p2a: PRIVATE_ALIAS|20/100"),
                "same-slot actor alias not current");
        reject(with(base,"|-damage|p3a: Unknown|20/100"),
                "foreign or absent battle actor");
        reject(with(base,"|turn|3"),
                "missing turn 2");
        reject(with(base,"|turn|1"),
                "duplicate turn 1");
        reject(with(base,"update\n|-damage|p1a: Own|50/100"),
                "nested update framing and multiple events per packet");
        reject(with(base,"|request|PRIVATE_REQUEST"),
                "private request in public route");
        reject(with(base,"|-weather|RainDance"),
                "unsupported public weather abstains");

        // Illusion/forme/transform are recognized, but *never* upgrade native
        // actor identity: those appearances remain ambiguous for this battle.
        Object illusionBattle=new Object();
        var illusion=new ReadOnlyShadowObservationBridge();
        var endIllusion=with(base,"|replace|p2a: Zoroark|Zoroark, L50");
        var ill=illusion.observe(illusionBattle,endIllusion,true);
        check(ill.state()==ReadOnlyShadowObservationBridge.State.APPEARANCE_AMBIGUOUS
                && !illusion.withdrawn() && illusion.publicLease(illusionBattle)==null,
                "Illusion ends, source stays observable but no cross-stream lease");
        check(illusion.observe(illusionBattle,endIllusion,true).revision()==1,
                "ambiguity remains through identical polling");
        var later=with(endIllusion,"|turn|2","|move|p2a: Zoroark|Tackle|p1a: Own");
        check(illusion.observe(illusionBattle,later,true).state()
                ==ReadOnlyShadowObservationBridge.State.APPEARANCE_AMBIGUOUS,
                "accepted new identity never clears uncertainty in same battle");
        reject(with(endIllusion,"|move|p2a: Opp|Tackle|p1a: Own"),
                "old disguised identity cannot issue a move after reveal");
        reject(with(base,"|replace|p2a: Zoroark|"),
                "incomplete Illusion replacement");
        reject(with(base,"|replace|p1a: Own|Zoroark|101/100"),
                "malformed Illusion replacement health");
        var form=new ReadOnlyShadowObservationBridge();
        Object formBattle=new Object();
        check(form.observe(formBattle,with(base,
                "|detailschange|p1a: Own|Genesect-Other, L50"),true).state()
                ==ReadOnlyShadowObservationBridge.State.APPEARANCE_AMBIGUOUS
                && form.publicLease(formBattle)==null,
                "forme change blocks actor-binding claims");
        var transform=new ReadOnlyShadowObservationBridge();
        Object trBattle=new Object();
        check(transform.observe(trBattle,with(base,
                "|-transform|p1a: Own|p2a: Opp"),true).state()
                ==ReadOnlyShadowObservationBridge.State.APPEARANCE_AMBIGUOUS
                && transform.publicLease(trBattle)==null,
                "Transform cannot promote copied actor identity");
        reject(with(base,"|-transform|p1a: Own|p9a: Fake"),
                "invalid transform target");
        reject(with(base,"|replace|p2a: Zoroark"),
                "missing replacement species");
        reject(with(base,"|-formechange|p1a: Own|"),
                "empty form");

        var split=with(base,"|split|p1\n|-damage|p1a: Own|SECRET/100\n"
                +"|-damage|p1a: Own|30/100");
        reject(split,"split includes private source half");
        reject(with(base,"sideupdate\n|request|PRIVATE_TEAM"),
                "sideupdate private request");
        reject(with(base,"|-damage|p1a: Own|30/100",
                "|split|p2"),"incomplete private split");
        var rewritten=new ReadOnlyShadowObservationBridge();
        Object rb=new Object();
        var firstHistory=with(base,"|-damage|p1a: Own|80/100");
        var rewrittenHistory=with(base,"|-damage|p1a: Own|70/100");
        rewritten.observe(rb,firstHistory,true);
        check(rewritten.observe(rb,rewrittenHistory,true).state()
                ==ReadOnlyShadowObservationBridge.State.REPLAY_OR_REWRITE
                && rewritten.withdrawn(),
                "rewrite of previously accepted expanded public event poisons source");

        System.out.println("Phase 6.14 direct-public coverage PASS: "+checks+" checks");
    }
}
