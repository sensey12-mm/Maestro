package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shadow Promotion 5 adversarial evidence tests, NOT Minecraft acceptance. */
final class PublicLiveOpponentSnapshotRegression {
    private static int checks;
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError("Shadow Promotion 5 "+reason);
        checks++;
    }
    private static String frame(String message){return "update\n"+message;}
    private static String split(String side,String hidden,String visible){
        return "update\n|split|"+side+"\n"+hidden+"\n"+visible;
    }
    private static List<String> opening(){
        return List.of(
            split("p1","|switch|p1a: Mine|Genesect, L100|55/100",
                  "|switch|p1a: Mine|Genesect, L100|100/100"),
            split("p2","|switch|p2a: Opp|Greninja, L100|20/100",
                  "|switch|p2a: Opp|Greninja, L100|85/100"),
            frame("|turn|1"));
    }
    private static List<String> with(List<String> history,String...add){
        var result=new ArrayList<>(history);
        for(String event:add)result.add(frame(event));
        return List.copyOf(result);
    }
    private static BattleSnapshot own(){
        return new BattleSnapshot(1,
            new BattleSnapshot.Combatant("own",.9,List.of("normal"),
                Map.of(),List.of(),null,null,false),
            new BattleSnapshot.Combatant("UNVERIFIED",-1,List.of(),
                Map.of(),List.of(),null,null,false),
            List.of(),List.of(),List.of("extremespeed","shadowclaw"),
            List.of(),Map.of(),false,null);
    }
    private static PublicLiveOpponentSnapshot.Capture capture(
            Object battle,String ownSide,int turn,BattleSnapshot snapshot,
            List<String> source){
        return PublicLiveOpponentSnapshot.capture(battle,ownSide,turn,
            snapshot,true,()->source,()->true);
    }
    static void run()throws Exception{
        Object battle=new Object();
        var history=new ArrayList<>(opening());
        var display=PublicLiveOpponentSnapshot.capture(
                battle,"p1",1,own(),true,()->history,()->true);
        check(display!=null && display.current()
                && display.snapshot().opponent().hpKnown()
                && display.snapshot().opponent().hpFraction()>.8
                && display.snapshot().opponent().hpFraction()<.9,
                "quantized public HP admitted, private split HP ignored");
        check(display.snapshot().opponent().types().isEmpty()
                && display.snapshot().opponent().species().equals("UNVERIFIED"),
                "public switch species appearance never proves true types under Illusion");
        check(!display.safeLine().contains("Greninja")
                && !display.safeLine().contains("Opp")
                && !display.safeLine().contains("85/100"),
                "diagnostic never leaks identities or public/private HP values");
        var ownUnknown=ShadowLiveDecisionEngine.ordinary(display.snapshot(),
                false,false,true);
        check(ownUnknown!=null && ownUnknown.action().equals("move:extremespeed"),
                "without confirmed opponent typing, normal priority remains allowed");
        history.add(frame("|-terastallize|p2a: Opp|Ghost"));
        history.add(frame("|move|p2a: Opp|Shadow Ball|p1a: Mine"));
        var typed=PublicLiveOpponentSnapshot.capture(
                battle,"p1",1,own(),true,()->history,()->true);
        check(typed!=null && typed.current()
                && typed.snapshot().opponentTerastallized()
                && typed.snapshot().opponent().types().equals(List.of("ghost"))
                && typed.snapshot().opponent().revealedMoves().equals(List.of("shadowball")),
                "exact current spectator Tera typing and revealed move are usable");
        var chosen=ShadowLiveDecisionEngine.ordinary(typed.snapshot(),
                false,false,true);
        check(chosen!=null && chosen.action().equals("move:shadowclaw"),
                "public Ghost immunity changes decision away from Normal-type priority");
        check(!display.current(),
                "an earlier source capture expires when history changes");
        history.add(frame("|turn|2"));
        check(!typed.current(),
                "append-only source advancement expires the original tactical receipt");
        var change=with(opening(),"|-start|p2a: Opp|typechange|Ghost");
        var transformedType=capture(new Object(),"p1",1,own(),change);
        check(transformedType!=null
                && transformedType.snapshot().opponent().types().equals(List.of("ghost"))
                && !transformedType.snapshot().opponentTerastallized(),
                "explicit public typechange is distinct from Tera");
        check(capture(new Object(),"p3",1,own(),opening())==null
                && capture(new Object(),"p1",3,own(),opening())==null
                && PublicLiveOpponentSnapshot.capture(new Object(),"p1",1,own(),
                    false,PublicLiveOpponentSnapshotRegression::opening,()->true)==null
                && PublicLiveOpponentSnapshot.capture(new Object(),"p1",1,own(),
                    true,PublicLiveOpponentSnapshotRegression::opening,()->false)==null,
                "wrong owner side, stale turn, unpinned source, foreign owner abstain");
        check(capture(new Object(),"p1",1,own(),
                with(opening(),"|-transform|p2a: Opp|p1a: Mine"))==null,
                "Transform poisons public Pokemon continuity");
        check(capture(new Object(),"p1",1,own(),
                with(opening(),"|replace|p2a: Opp|Zoroark, L100"))==null,
                "Illusion reveal is NOT a real-time stable identity certificate");
        check(capture(new Object(),"p1",1,own(),
                with(opening(),"|-formechange|p2a: Opp|Greninja-Ash"))==null,
                "unverified form change abstains");
        check(capture(new Object(),"p1",1,own(),
                with(opening(),"|switch|p2a: Opp|Greninja, L100|100/100"))==null,
                "reused slot cannot masquerade as known same opponent");
        var incomplete=new ArrayList<>(opening());
        incomplete.add("update\n|split|p2\n|-damage|p2a: Opp|1/100");
        check(capture(new Object(),"p1",1,own(),incomplete)==null,
                "incomplete private/public split cannot enter scoring");
        check(capture(new Object(),"p1",1,own(),
                with(opening(),"|request|PRIVATE_HIDDEN_OPPONENT_SET"))==null,
                "hidden request never becomes public evidence");
        var source=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int chooser=source.indexOf("public ShowdownActionResponse choose(");
        int legacy=source.indexOf("private ShowdownActionResponse chooseLegacy(");
        int capture=source.indexOf("PublicLiveOpponentSnapshot.capture(",chooser);
        check(chooser>=0 && capture>chooser && capture<legacy
                && source.substring(chooser,capture).contains(
                    "captureIndependentShadowSnapshot(")
                && source.substring(capture,legacy).contains(
                    "ShadowPromotionPreparation.inspectPublicOnly(")
                && source.substring(capture,legacy).contains(
                    "ShadowPromotionPreparation.inspectOwnOnly("),
                "live chooser binds independent own root then source-scoped public "
                    +"evidence and retains independent own-only fallback");
        System.out.println("Shadow Promotion 5 public opponent regression PASS: "
                +checks+" assertions; installed Minecraft UNTESTED");
    }
}
