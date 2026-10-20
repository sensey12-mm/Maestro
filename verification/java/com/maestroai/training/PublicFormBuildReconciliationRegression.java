package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** No actual Pokémon battle, real replay, registry or private key is imported. */
final class PublicFormBuildReconciliationRegression {
    private PublicFormBuildReconciliationRegression(){}
    private static int checks;
    private static void check(boolean good) {
        if(!good) throw new AssertionError("Phase 5.15 pinned form reconciliation");
        checks++;
    }
    private static void reject(Runnable task) {
        try{task.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains("PRIVATE_PLAYER")
                    && !expected.getMessage().contains("SecretNick"));return;
        }
        throw new AssertionError("Phase 5.15 unsupported source incorrectly accepted");
    }
    private static final long NOW=1_700_000_100L;
    private static final PublicOpponentBuildHypotheses.Kind MOVE=
            PublicOpponentBuildHypotheses.Kind.MOVE;
    private static final PublicOpponentBuildHypotheses.Kind ABILITY=
            PublicOpponentBuildHypotheses.Kind.ABILITY;
    private static final PublicOpponentBuildHypotheses.Kind ITEM=
            PublicOpponentBuildHypotheses.Kind.ITEM;
    private static final PublicFormBuildReconciliation.Finding LISTED=
            PublicFormBuildReconciliation.Finding.SOURCE_LISTED_LEGAL_QUERY_METHOD;
    private static final PublicFormBuildReconciliation.Finding NONLEGAL=
            PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NONLEGAL_QUERY_ONLY;
    private static final PublicFormBuildReconciliation.Finding CONFLICT=
            PublicFormBuildReconciliation.Finding.PUBLIC_EVENT_SOURCE_DISAGREEMENT;
    private static final PublicFormBuildReconciliation.Finding ABSENT=
            PublicFormBuildReconciliation.Finding.SOURCE_NOT_LISTED;
    private static final PublicFormBuildReconciliation.Finding OVERRIDDEN=
            PublicFormBuildReconciliation.Finding.ABSTAIN_REPORTED_OVERRIDE;
    private static final PublicFormBuildReconciliation.Finding UNKNOWN=
            PublicFormBuildReconciliation.Finding.ABSTAIN_INCOMPLETE_SOURCE;
    private static final PublicFormBuildReconciliation.Origin INHERITED=
            PublicFormBuildReconciliation.Origin.FORM_INHERITS_BASE;
    private static final PublicFormBuildReconciliation.Origin REPLACED=
            PublicFormBuildReconciliation.Origin.FORM_REPLACES_BASE;

    private record Case(PublicReplayCorpus.Source source,String log,
                        List<PublicOpponentBuildHypotheses.CandidateQuery> guesses,
                        PublicSignedCustody.SignedManifest signed,
                        PublicSignedCustody.TrustedPolicy policy) {
        PublicFormBuildReconciliation.Report run(Set<PublicFormBuildReconciliation.OverrideNotice> notices) {
            return PublicFormBuildReconciliation.reconcile(source,log,guesses,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    notices,signed,policy,NOW);
        }
    }
    private static String intro(String species) {
        return "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
                +"|player|p1|PRIVATE_PLAYER\n|start\n"
                +"|switch|p1a: Friend|Pikachu, L100|100/100\n"
                +"|switch|p2a: SecretNick|"+species+", L100|100/100\n"
                +"|turn|1\n";
    }
    private static String move(String attack) {
        return "|move|p2a: SecretNick|"+attack+"|p1a: Friend\n";
    }
    private static PublicOpponentBuildHypotheses.CandidateQuery query(int index,
                    String[] moves,String[] abilities,String[] items) {
        return new PublicOpponentBuildHypotheses.CandidateQuery(index,
                Set.of(moves),Set.of(abilities),Set.of(items));
    }
    private static Case fixture(int n,String log,
                    List<PublicOpponentBuildHypotheses.CandidateQuery> guesses) {
        try {
            var source=new PublicReplayCorpus.Source("gen9ou-"+n,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,log);
            var approval=new PublicReplayCustody.Decision(source.replayId(),
                    source.format(),evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    NOW-25,NOW+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(approval));
            var k=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(k.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"synthetic",
                    NOW-10,"0".repeat(64),ledger));
            var manifest=new PublicSignedCustody.SignedManifest(1,"synthetic",NOW-10,
                    "0".repeat(64),ledger,Base64.getEncoder().encodeToString(signer.sign()));
            var trusted=new PublicSignedCustody.TrustedKey("synthetic",
                    Base64.getEncoder().encodeToString(k.getPublic().getEncoded()),
                    NOW-100,0);
            return new Case(source,log,guesses,manifest,new PublicSignedCustody.TrustedPolicy(
                    1,1,1,NOW-100,"",List.of(trusted)));
        } catch(GeneralSecurityException failure){throw new AssertionError(failure);}
    }
    private static PublicFormBuildReconciliation.Evidence find(
            PublicFormBuildReconciliation.Appearance appearance,
            PublicOpponentBuildHypotheses.Kind kind,String id) {
        return appearance.evidence().stream().filter(e->e.kind()==kind
                && e.id().equals(id)).findFirst().orElseThrow();
    }
    private static PublicFormBuildReconciliation.OverrideNotice notice(String form,
                    PublicFormBuildReconciliation.Field field) {
        return new PublicFormBuildReconciliation.OverrideNotice(form,field);
    }
    static void run() {
        check(PublicFormBuildReconciliation.UPSTREAM_REVISION.equals(
                PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(PublicFormBuildReconciliation.FORM_RESOLVER_BLOB.length()==40);
        check(PublicFormBuildReconciliation.LEARNSET_BLOB.length()==40);
        check(PublicFormBuildReconciliation.LEARNSET_QUERY_BLOB.length()==40);
        check(PublicFormBuildReconciliation.LEARNSET_ADAPTER_BLOB.length()==40);
        check(PublicFormBuildReconciliation.SPECIES_ADDITIONS_BLOB.length()==40);
        var pinned=PublicFormBuildReconciliation.ResolverPin.pinned();
        check(pinned.commit().equals(PublicSpeciesBuildLegality.SOURCE_REVISION));
        reject(()->new PublicFormBuildReconciliation.ResolverPin("0".repeat(40),
                pinned.formBlob(),pinned.learnsetBlob(),pinned.queryBlob(),
                pinned.adapterBlob(),pinned.additionsBlob()));
        reject(()->new PublicFormBuildReconciliation.ResolverPin(pinned.commit(),
                "0".repeat(40),pinned.learnsetBlob(),pinned.queryBlob(),
                pinned.adapterBlob(),pinned.additionsBlob()));
        reject(()->new PublicFormBuildReconciliation.ResolverPin(pinned.commit(),
                pinned.formBlob(),pinned.learnsetBlob(),"0".repeat(40),
                pinned.adapterBlob(),pinned.additionsBlob()));
        reject(()->new PublicFormBuildReconciliation.ResolverPin(pinned.commit(),
                pinned.formBlob(),pinned.learnsetBlob(),pinned.queryBlob(),
                pinned.adapterBlob(),"0".repeat(40)));
        check(PublicFormBuildReconciliation.acceptsFullSourceRecord(
                "zoroarkhisui",PublicFormBuildReconciliation.Field.MOVES));
        check(PublicFormBuildReconciliation.acceptsFullSourceRecord(
                "zoroarkhisui",PublicFormBuildReconciliation.Field.ABILITIES));
        check(PublicFormBuildReconciliation.acceptsFullSourceRecord(
                "mewtwomegay",PublicFormBuildReconciliation.Field.MOVES));
        check(PublicFormBuildReconciliation.acceptsFullSourceRecord(
                "mewtwomegay",PublicFormBuildReconciliation.Field.ABILITIES));
        check(!PublicFormBuildReconciliation.acceptsFullSourceRecord(
                "mewtwomegaz",PublicFormBuildReconciliation.Field.MOVES));
        check(!PublicFormBuildReconciliation.acceptsFullSourceRecord(null,
                PublicFormBuildReconciliation.Field.ABILITIES));
        check(Arrays.stream(PublicFormBuildReconciliation.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));

        // Hisui: own explicit moves replace base; absent abilities inherit base.
        String hisuiLog=intro("Zoroark-Hisui")+move("Shadow Sneak")
                +"|win|PRIVATE_PLAYER\n";
        var hisui=fixture(12345678,hisuiLog,List.of(query(2,
                new String[]{"shadowsneak","nastyplot","pursuit","happyhour","notamove"},
                new String[]{"illusion","imposter"},new String[]{"leftovers"})));
        var hr=hisui.run(Set.of());
        check(hr.appearances().size()==1);
        check(!hr.installedModpackCompatibilityVerified()
                && !hr.battleFormIdentityVerified() && !hr.itemLegalityKnown()
                && !hr.livePromotionAuthorized() && !hr.shadowTacticalReady());
        check(!hr.toString().contains("PRIVATE_PLAYER")
                && !hr.toString().contains("SecretNick")
                && !hr.toString().contains("gen9ou-"));
        var h=hr.appearances().get(0);
        check(h.displayedId().equals("zoroarkhisui") && h.identityUncertain());
        check(h.moveOrigin()==REPLACED && h.abilityOrigin()==INHERITED);
        check(find(h,MOVE,"shadowsneak").finding()==LISTED);
        check(find(h,MOVE,"shadowsneak").publiclyObserved());
        check(find(h,MOVE,"nastyplot").finding()==LISTED);
        check(find(h,MOVE,"pursuit").finding()==ABSENT);
        check(!find(h,MOVE,"pursuit").sourceListed());
        check(find(h,MOVE,"happyhour").finding()==NONLEGAL);
        check(find(h,MOVE,"happyhour").listedMethods().stream().allMatch(x->
                !x.legalQueryMember() && x.name().equals("special")));
        check(find(h,ABILITY,"illusion").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY);
        check(find(h,ABILITY,"imposter").finding()==ABSENT);
        check(find(h,ITEM,"leftovers").finding()
                ==PublicFormBuildReconciliation.Finding.ITEM_LEGALITY_UNKNOWN);
        check(!find(h,ITEM,"leftovers").sourceListed());
        check(find(h,MOVE,"notamove").finding()==ABSENT);
        check(h.evidence().stream().noneMatch(e->e.fullInstalledRegistryAttested()
                || e.nativeBuildConfirmed()));
        try{h.evidence().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        try{find(h,MOVE,"shadowsneak").listedMethods().add(
                new PublicFormBuildReconciliation.Method("tm",true));
            throw new AssertionError("mutable");
        }catch(UnsupportedOperationException expected){checks++;}

        // Base ordinary Zoroark and source listed legacy ONLY are distinct
        // from a query-LEGAL learn method. Observed non-native conflicting move
        // is a disagreement, NOT proof of illegal player behavior.
        String ordinaryLog=intro("Zoroark")+move("Shadow Sneak")
                +"|win|PRIVATE_PLAYER\n";
        var ordinary=fixture(12345679,ordinaryLog,List.of(query(2,
                new String[]{"frustration","nastyplot","swordsdance"},
                new String[]{"illusion"},new String[]{})));
        var o=ordinary.run(Set.of()).appearances().get(0);
        check(o.moveOrigin()==PublicFormBuildReconciliation.Origin.BASE_EXPLICIT);
        check(find(o,MOVE,"shadowsneak").finding()==CONFLICT);
        check(find(o,MOVE,"shadowsneak").publiclyObserved());
        check(!find(o,MOVE,"shadowsneak").sourceListed());
        check(find(o,MOVE,"frustration").finding()==NONLEGAL);
        check(find(o,MOVE,"frustration").listedMethods().stream().allMatch(x->
                !x.legalQueryMember() && x.name().equals("legacy")));
        check(find(o,MOVE,"nastyplot").finding()==LISTED);
        check(find(o,MOVE,"nastyplot").listedMethods().stream().anyMatch(x->
                x.legalQueryMember() && x.name().equals("level")));
        check(find(o,MOVE,"swordsdance").finding()==LISTED);
        check(find(o,MOVE,"swordsdance").listedMethods().stream().anyMatch(x->
                x.legalQueryMember() && x.name().equals("tm")));

        // Mega Y: omitted moves inherit ALL base methods, explicit
        // form abilities replace base. Pressure does not leak from base.
        String megaLog=intro("Mewtwo-Mega-Y")+move("Psystrike")
                +"|win|PRIVATE_PLAYER\n";
        var mega=fixture(12345680,megaLog,List.of(query(2,
                new String[]{"psystrike","swift","frustration"},
                new String[]{"insomnia","pressure","unnerve"},
                new String[]{})));
        var m=mega.run(Set.of()).appearances().get(0);
        check(m.moveOrigin()==INHERITED && m.abilityOrigin()==REPLACED);
        check(find(m,MOVE,"psystrike").finding()==LISTED);
        check(find(m,MOVE,"psystrike").listedMethods().stream().anyMatch(x->
                x.name().equals("level") && x.legalQueryMember()));
        check(find(m,MOVE,"swift").finding()==LISTED);
        check(find(m,ABILITY,"insomnia").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY);
        check(find(m,ABILITY,"pressure").finding()==ABSENT);
        check(find(m,ABILITY,"unnerve").finding()==ABSENT);

        // A base override can invalidate only inherited fields; it must
        // not silently erase an explicitly replaced Hisuian move list.
        var overriddenBase=hisui.run(Set.of(notice("zoroark",
                PublicFormBuildReconciliation.Field.ABILITIES))).appearances().get(0);
        check(find(overriddenBase,ABILITY,"illusion").finding()==OVERRIDDEN);
        check(find(overriddenBase,MOVE,"shadowsneak").finding()==LISTED);
        var ownOverride=hisui.run(Set.of(notice("zoroarkhisui",
                PublicFormBuildReconciliation.Field.MOVES))).appearances().get(0);
        check(find(ownOverride,MOVE,"shadowsneak").finding()==OVERRIDDEN);
        check(find(ownOverride,ABILITY,"illusion").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY);
        var overriddenMegaBase=mega.run(Set.of(notice("mewtwo",
                PublicFormBuildReconciliation.Field.MOVES))).appearances().get(0);
        check(find(overriddenMegaBase,MOVE,"psystrike").finding()==OVERRIDDEN);
        check(find(overriddenMegaBase,ABILITY,"insomnia").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY);
        var ownMega=mega.run(Set.of(notice("mewtwomegay",
                PublicFormBuildReconciliation.Field.ABILITIES))).appearances().get(0);
        check(find(ownMega,ABILITY,"insomnia").finding()==OVERRIDDEN);
        check(find(ownMega,MOVE,"psystrike").finding()==LISTED);
        reject(()->hisui.run(Set.of(notice("unknownform",
                PublicFormBuildReconciliation.Field.MOVES))));
        reject(()->notice("Mewtwo-Mega-Y",
                PublicFormBuildReconciliation.Field.MOVES));
        reject(()->notice("zoroarkhisui",null));

        // Unsupported display cannot use any base form or fallback.
        var unknownLog=intro("Mewtwo-Mega-Z")+move("Psystrike")
                +"|win|PRIVATE_PLAYER\n";
        var unknown=fixture(12345681,unknownLog,List.of(query(2,
                new String[]{"psystrike"},new String[]{"pressure"},new String[]{})));
        var u=unknown.run(Set.of()).appearances().get(0);
        check(u.moveOrigin()==PublicFormBuildReconciliation.Origin.UNSUPPORTED_FORM);
        check(u.abilityOrigin()==PublicFormBuildReconciliation.Origin.UNSUPPORTED_FORM);
        check(u.evidence().stream().allMatch(e->e.finding()==UNKNOWN));

        // Ability slot membership is distinct from installed-effect behavior.
        var dittoLog=intro("Ditto")+move("Transform")+"|win|PRIVATE_PLAYER\n";
        var ditto=fixture(12345682,dittoLog,List.of(query(2,
                new String[]{"transform","tackle"},
                new String[]{"limber","imposter","illusion"},new String[]{"choiceband"})));
        var d=ditto.run(Set.of()).appearances().get(0);
        check(find(d,MOVE,"transform").finding()==LISTED);
        check(find(d,MOVE,"tackle").finding()==ABSENT);
        check(find(d,ABILITY,"limber").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_NORMAL_ABILITY);
        check(find(d,ABILITY,"imposter").finding()
                ==PublicFormBuildReconciliation.Finding.SOURCE_LISTED_HIDDEN_ABILITY);
        check(find(d,ABILITY,"illusion").finding()==ABSENT);

        // Transform copied moves, even if listed by a species elsewhere,
        // are never native or legal under this appearance.
        var copiedLog=intro("Ditto")
                +"|-transform|p2a: SecretNick|p1a: Friend\n"+move("Tackle")
                +"|win|PRIVATE_PLAYER\n";
        var copied=fixture(12345683,copiedLog,List.of(query(2,
                new String[]{"transform","tackle"},
                new String[]{"imposter"},new String[]{"leftovers"})));
        var t=copied.run(Set.of()).appearances().get(0);
        check(t.transformed());
        check(find(t,MOVE,"transform").finding()
                ==PublicFormBuildReconciliation.Finding.ABSTAIN_TRANSFORMED_APPEARANCE);
        check(find(t,ABILITY,"imposter").finding()
                ==PublicFormBuildReconciliation.Finding.ABSTAIN_TRANSFORMED_APPEARANCE);
        check(find(t,ITEM,"leftovers").finding()
                ==PublicFormBuildReconciliation.Finding.ITEM_LEGALITY_UNKNOWN);

        // Multiple identical names/species never link observed moves or
        // overrides across appearance boundaries.
        String repeated="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
                +"|player|p1|PRIVATE_PLAYER\n|start\n"
                +"|switch|p1a: Friend|Pikachu, L100|100/100\n"
                +"|switch|p2a: SecretNick|Zoroark, L100|100/100\n"
                +"|turn|1\n"+move("Nasty Plot")
                +"|switch|p2a: SecretNick|Zoroark, L100|100/100\n"
                +"|turn|2\n"+move("Swift")+"|win|PRIVATE_PLAYER\n";
        var repeatedCase=fixture(12345684,repeated,List.of());
        var appearances=repeatedCase.run(Set.of()).appearances();
        check(appearances.size()==2 && appearances.get(0).index()==2
                && appearances.get(1).index()==3);
        check(appearances.get(0).evidence().stream().anyMatch(e->
                e.id().equals("nastyplot") && e.publiclyObserved()));
        check(appearances.get(1).evidence().stream().noneMatch(e->
                e.id().equals("nastyplot") && e.publiclyObserved()));
        check(appearances.stream().allMatch(PublicFormBuildReconciliation.Appearance::identityUncertain));

        // Constructor and validation probes are NOT an authority for a
        // competing source, unknown methods or synthetic verified gameplay.
        reject(()->new PublicFormBuildReconciliation.Method("special",true));
        reject(()->new PublicFormBuildReconciliation.Method("tm",false));
        reject(()->new PublicFormBuildReconciliation.Method("unapproved",true));
        reject(()->new PublicFormBuildReconciliation.Evidence(2,ITEM,"leftovers",
                LISTED,REPLACED,Set.of(),false,true,true,false));
        reject(()->new PublicFormBuildReconciliation.Evidence(2,MOVE,"psystrike",
                LISTED,INHERITED,Set.of(),false,true,false,true));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),null,pinned,Set.of(),
                hisui.signed(),hisui.policy(),NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,Set.of(),
                hisui.signed(),hisui.policy(),NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),pinned,null,
                hisui.signed(),hisui.policy(),NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),pinned,Set.of(),
                null,hisui.policy(),NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),pinned,Set.of(),
                hisui.signed(),null,NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log(),hisui.guesses(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),pinned,Set.of(),
                hisui.signed(),hisui.policy(),NOW+2000));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log().replace("Shadow Sneak","Shadow Claw"),
                hisui.guesses(),PublicSpeciesBuildLegality.SourcePin.pinned(),
                pinned,Set.of(),hisui.signed(),hisui.policy(),NOW));
        reject(()->PublicFormBuildReconciliation.reconcile(
                hisui.source(),hisui.log()+"|request|PRIVATE_PLAYER\n",
                hisui.guesses(),PublicSpeciesBuildLegality.SourcePin.pinned(),
                pinned,Set.of(),hisui.signed(),hisui.policy(),NOW));
        check(PublicFormBuildReconciliation.Report.class.getDeclaredConstructors()[0]
                .getModifiers()!=Modifier.PUBLIC);
        System.out.println("Phase 5.15 synthetic form reconciliation PASS: "+checks+" checks");
    }
}
