package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Promotion 7: non-omniscient public display and mandatory CI-custody audit. */
final class ShadowPromotion7Regression {
    private static int checks;
    private static void check(boolean valid,String meaning) {
        if(!valid)throw new AssertionError("Shadow Promotion 7: "+meaning);
        checks++;
    }
    static void run()throws Exception {
        check(PublicSpeciesCatalog.displayedTypeHypothesis("zoroark")
                    .equals(List.of("dark"))
                && PublicSpeciesCatalog.displayedTypeHypothesis("not_in_catalog").isEmpty(),
                "frozen catalog can supply public rules but cannot prove opponent identity");
        var moves=List.of(
                "|switch|p2a: Foil|Zoroark, L100|100/100",
                "|move|p2a: Foil|Night Daze|p1a: Own");
        check(PublicOpponentAppearance.displayedTypeHypothesis("p2a",moves,
                PublicSpeciesCatalog::displayedTypeHypothesis).equals(List.of("dark"))
                && PublicOpponentAppearance.witnessedRevealSpecies("p2a",moves)==null,
                "ordinary switch remains conditional even for an actual Zoroark display");
        var illusion=List.of(
                "|switch|p2a: Foil|Gengar, L100|100/100",
                "|replace|p2a: Foil|Zoroark, L100");
        check(PublicOpponentAppearance.displayedTypeHypothesis("p2a",illusion,
                id->List.of("ghost")).isEmpty()
                && "zoroark".equals(PublicOpponentAppearance.witnessedRevealSpecies(
                    "p2a",illusion)),
                "positive current replace revokes earlier disguised appearance");
        var transform=List.of(
                "|switch|p2a: Foil|Gengar, L100|100/100",
                "|replace|p2a: Foil|Zoroark, L100",
                "|-transform|p2a: Foil|p1a: Own");
        check(PublicOpponentAppearance.displayedTypeHypothesis("p2a",transform,
                id->List.of("ghost")).isEmpty()
                && PublicOpponentAppearance.witnessedRevealSpecies("p2a",transform)==null,
                "Transform invalidates both displayed and confirmed-identity typing");
        var returned=List.of(
                "|switch|p2a: Foil|Gengar, L100|100/100",
                "|replace|p2a: Foil|Zoroark, L100",
                "|switch|p2a: Foil|Gengar, L100|100/100");
        check(PublicOpponentAppearance.witnessedRevealSpecies("p2a",returned)==null,
                "switch reentry never retains a prior positive identity revelation");
        var tera=List.of(
                "|switch|p2a: Foil|Gengar, L100|100/100",
                "|-terastallize|p2a: Foil|Fairy");
        check(PublicOpponentAppearance.displayedTypeHypothesis("p2a",tera,
                id->List.of("ghost")).isEmpty(),
                "public Tera supersedes hypothetical native display typing");
        var malformed=List.of("|switch|p2a: Foil|Gengar|100/100");
        check(PublicOpponentAppearance.displayedTypeHypothesis("p2a",malformed,
                id->List.of("ghost","ghost")).isEmpty()
                && PublicOpponentAppearance.displayedTypeHypothesis("p1a",moves,
                    id->List.of("ghost")).isEmpty(),
                "malformed catalog and wrong actor never leak conditional types");
        var own=new BattleSnapshot.Combatant("own",.85,List.of("normal"),
                Map.of(),List.of(),null,null,false);
        var opponent=new BattleSnapshot.Combatant("UNVERIFIED",.8,List.of(),
                Map.of(),List.of(),null,null,false);
        var snapshot=new BattleSnapshot(1,own,opponent,List.of(),List.of(),
                List.of("extremespeed","shadowclaw"),List.of(),Map.of(),false,null);
        var old=ShadowLiveDecisionEngine.ordinary(snapshot,false,false,true,Map.of());
        var conditional=ShadowLiveDecisionEngine.ordinary(
                snapshot,false,false,true,Map.of(),List.of("ghost","poison"));
        check(old!=null && old.action().equals("move:extremespeed")
                && conditional!=null && conditional.action().equals("move:shadowclaw")
                && snapshot.opponent().types().isEmpty(),
                "hypothesis changes action without certifying ghost immunity");
        check(ShadowLiveDecisionEngine.ordinary(
                snapshot,false,false,false,Map.of(),List.of("ghost"))==null,
                "stale native source cannot convert display to a Shadow decision");
        var workflow=Files.readString(Path.of(".github/workflows/verify.yml"));
        check(workflow.contains("group: verify-${{ github.event.pull_request.number || github.ref }}")
                && workflow.contains("cancel-in-progress:")
                && !workflow.contains("&& github.sha || 'shared'")
                && workflow.contains("needs: [verify, pack_source]")
                && workflow.contains("python3 verification/source_oracle.py --native")
                && workflow.contains("python3 verification/source_oracle.py --pack"),
                "same-branch superseded CI runs cancel, while both exact-SHA native gates remain");
        var adapter=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/LivePublicSpeciesTyping.java"));
        check(adapter.contains("PokemonSpecies.getSpecies()")
                && adapter.contains("entry.showdownId()")
                && !adapter.contains("getBattlePokemon(")
                && !adapter.contains("getRequest(")
                && !adapter.contains("getActor("),
                "public rules adapter never reads hidden opposing battle objects");
        System.out.println("Shadow Promotion 7 PASS: "+checks
                +" deterministic public-identity/CI-custody checks; Minecraft NOT TESTED");
    }
}
