package com.maestroai.training;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Synthetic-only Phase 5.14 integrity/compatibility/adversarial regressions. */
final class PublicSpeciesBuildLegalityRegression {
    private PublicSpeciesBuildLegalityRegression(){}
    private static int checks;
    private static void check(boolean ok) {
        if(!ok)throw new AssertionError("Phase 5.14 pinned species build evidence");
        checks++;
    }
    private static void reject(Runnable task) {
        try {task.run();}
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SYNTHETIC_PRIVATE_PERSON")
                    && !expected.getMessage().contains("SecretNick"));return;
        }
        throw new AssertionError("Incompatible species source was accepted");
    }
    private static final long NOW=1_700_000_100L;
    private static final CompetitiveFormatNormalizer.Target PIN=CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final String INTRO="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SYNTHETIC_PRIVATE_PERSON\n|start\n";
    private static PublicReplayCorpus.Source source(int serial) {
        return new PublicReplayCorpus.Source("gen9ou-"+serial,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static String log(String pokemon,String actions) {
        return INTRO+"|switch|p1a: MyAlly|Pikachu, L100|100/100\n"
                +"|switch|p2a: SecretNick|"+pokemon+", L100|100/100\n"
                +"|turn|1\n"+actions+"|win|SYNTHETIC_PRIVATE_PERSON\n";
    }
    private static String move(String attack) {
        return "|move|p2a: SecretNick|"+attack+"|p1a: MyAlly\n";
    }
    private record Authority(PublicSignedCustody.SignedManifest signed,
                             PublicSignedCustody.TrustedPolicy policy) {}
    private static Authority authority(PublicReplayCorpus.Source source,String raw) {
        try {
            var e=PublicReplayCorpus.ingest(source,raw);
            var approved=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    e.inputSha256(),PublicReplayCustody.Status.APPROVED,NOW-20,NOW+1000,
                    "c".repeat(64));
            var history=new PublicReplayCustody.Ledger(1,1,List.of(approved));
            var key=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(key.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"test-reviewer",
                    NOW-10,"0".repeat(64),history));
            var signed=new PublicSignedCustody.SignedManifest(1,"test-reviewer",NOW-10,
                    "0".repeat(64),history,Base64.getEncoder().encodeToString(signer.sign()));
            var publicKey=new PublicSignedCustody.TrustedKey("test-reviewer",
                    Base64.getEncoder().encodeToString(key.getPublic().getEncoded()),
                    NOW-100,0);
            return new Authority(signed,new PublicSignedCustody.TrustedPolicy(
                    1,1,1,NOW-100,"",List.of(publicKey)));
        } catch(GeneralSecurityException ex){throw new AssertionError(ex);}
    }
    private static PublicOpponentBuildHypotheses.CandidateQuery suggest(
            int appearance,String[] moves,String[] abilities,String[] items) {
        return new PublicOpponentBuildHypotheses.CandidateQuery(appearance,
                Set.of(moves),Set.of(abilities),Set.of(items));
    }
    private static PublicSpeciesBuildLegality.Report inspect(PublicReplayCorpus.Source source,
            String raw,List<PublicOpponentBuildHypotheses.CandidateQuery> choices,
            Authority auth) {
        return PublicSpeciesBuildLegality.analyze(source,raw,choices,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                auth.signed(),auth.policy(),NOW);
    }
    private static PublicSpeciesBuildLegality.Candidate find(
            PublicSpeciesBuildLegality.Appearance a,PublicOpponentBuildHypotheses.Kind k,String id) {
        return a.candidates().stream().filter(e->e.kind()==k && e.id().equals(id))
                .findFirst().orElseThrow();
    }
    private static final PublicOpponentBuildHypotheses.Kind MOVE=PublicOpponentBuildHypotheses.Kind.MOVE;
    private static final PublicOpponentBuildHypotheses.Kind ABILITY=PublicOpponentBuildHypotheses.Kind.ABILITY;
    private static final PublicOpponentBuildHypotheses.Kind ITEM=PublicOpponentBuildHypotheses.Kind.ITEM;
    private static final PublicSpeciesBuildLegality.Status LEVEL=
            PublicSpeciesBuildLegality.Status.SOURCE_LISTED_LEVEL_MOVE;
    private static final PublicSpeciesBuildLegality.Status OTHER=
            PublicSpeciesBuildLegality.Status.SOURCE_LISTED_OTHER_METHOD;
    private static final PublicSpeciesBuildLegality.Status UNKNOWN=
            PublicSpeciesBuildLegality.Status.UNKNOWN_IN_SOURCE_SLICE;
    private static final PublicSpeciesBuildLegality.Status UNSPECIFIED=
            PublicSpeciesBuildLegality.Status.SOURCE_FORM_METHOD_OR_ABILITY_UNSPECIFIED;
    static void run() {
        check(PublicSpeciesBuildLegality.SOURCE_REVISION.length()==40);
        check(PublicSpeciesBuildLegality.SOURCE_REPOSITORY.equals("MayIHaveK/cobblemon-1.7.3"));
        check(PublicSpeciesBuildLegality.NORMALIZED_SHA256.matches("[0-9a-f]{64}"));
        byte[] rawResource;
        try(var resource=PublicSpeciesBuildLegalityRegression.class.getResourceAsStream(
                PublicSpeciesBuildLegality.RESOURCE)) {
            check(resource!=null);
            rawResource=Objects.requireNonNull(resource).readAllBytes();
        }catch(IOException e){throw new AssertionError(e);}
        check(PublicSpeciesBuildLegality.acceptsExactSource(rawResource));
        check(!PublicSpeciesBuildLegality.acceptsExactSource(new byte[0]));
        var mutated=rawResource.clone();mutated[mutated.length/2]^=0x04;
        check(!PublicSpeciesBuildLegality.acceptsExactSource(mutated));
        check(!PublicSpeciesBuildLegality.acceptsExactSource(
                Arrays.copyOf(rawResource,rawResource.length-1)));
        check(!PublicSpeciesBuildLegality.acceptsExactSource(
                Arrays.copyOf(rawResource,rawResource.length+1)));
        var alteredSource=new String(rawResource,java.nio.charset.StandardCharsets.UTF_8)
                .replace("h:illusion","h:pressure");
        check(!PublicSpeciesBuildLegality.acceptsExactSource(
                alteredSource.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        check(!PublicSpeciesBuildLegality.acceptsExactSource(
                new String(rawResource,java.nio.charset.StandardCharsets.UTF_8)
                .replace("zoroarkhisui\t","zoroarkfake\t")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        reject(()->new PublicSpeciesBuildLegality.SourcePin("other",
                PublicSpeciesBuildLegality.SOURCE_REVISION,
                PublicSpeciesBuildLegality.NORMALIZED_SHA256,PIN));
        reject(()->new PublicSpeciesBuildLegality.SourcePin(
                PublicSpeciesBuildLegality.SOURCE_REPOSITORY,"0".repeat(40),
                PublicSpeciesBuildLegality.NORMALIZED_SHA256,PIN));
        reject(()->new PublicSpeciesBuildLegality.SourcePin(
                PublicSpeciesBuildLegality.SOURCE_REPOSITORY,
                PublicSpeciesBuildLegality.SOURCE_REVISION,"0".repeat(64),PIN));
        reject(()->new PublicSpeciesBuildLegality.SourcePin(
                PublicSpeciesBuildLegality.SOURCE_REPOSITORY,
                PublicSpeciesBuildLegality.SOURCE_REVISION,
                PublicSpeciesBuildLegality.NORMALIZED_SHA256,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","unknown","other")));
        check(Arrays.stream(PublicSpeciesBuildLegality.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));

        var zoroark=source(12345678);
        String zLog=log("Zoroark",
                "|-ability|p2a: SecretNick|Illusion\n"
                +"|-item|p2a: SecretNick|Leftovers\n"+move("Nasty Plot"));
        var zAuth=authority(zoroark,zLog);
        var candidates=List.of(suggest(2,
                new String[]{"nastyplot","swordsdance","shadowsneak","swift","transform"},
                new String[]{"illusion","imposter","pressure"},
                new String[]{"leftovers","choicescarf"}));
        var z=inspect(zoroark,zLog,candidates,zAuth);
        check(z.appearances().size()==1 && z.appearances().get(0).index()==2);
        check(z.sourceScope().equals(CobblemonMechanics.SOURCE));
        check(!z.fullCobblemonRegistryAttested() && !z.completeLearnsetOrAbilityCoverage());
        check(!z.itemLegalityVerified() && !z.currentOpponentBuildConfirmed());
        check(!z.shadowTacticalReady() && !z.livePromotionAuthorized());
        check(!z.toString().contains("SYNTHETIC_PRIVATE_PERSON")
                && !z.toString().contains("SecretNick")
                && !z.toString().contains("gen9ou-"));
        var e=z.appearances().get(0);
        check(e.exactSourceForm() && e.identityUncertain()
                && !e.confirmedSamePartyMember());
        check(find(e,MOVE,"nastyplot").sourceStatus()==LEVEL);
        check(find(e,MOVE,"nastyplot").publiclyObserved());
        check(find(e,MOVE,"nastyplot").listedMethods().contains("level"));
        check(find(e,MOVE,"swordsdance").sourceStatus()==OTHER);
        check(find(e,MOVE,"swordsdance").listedMethods().contains("tm"));
        check(find(e,MOVE,"swift").sourceStatus()==OTHER);
        check(find(e,MOVE,"shadowsneak").sourceStatus()==UNKNOWN);
        check(find(e,MOVE,"transform").sourceStatus()==UNKNOWN);
        check(find(e,ABILITY,"illusion").sourceStatus()==
                PublicSpeciesBuildLegality.Status.SOURCE_LISTED_NORMAL_ABILITY);
        check(find(e,ABILITY,"illusion").publiclyObserved());
        check(find(e,ABILITY,"imposter").sourceStatus()==UNKNOWN);
        check(find(e,ABILITY,"pressure").sourceStatus()==UNKNOWN);
        check(find(e,ITEM,"leftovers").sourceStatus()==UNKNOWN);
        check(find(e,ITEM,"leftovers").publiclyObserved());
        check(find(e,ITEM,"choicescarf").sourceStatus()==UNKNOWN);
        check(e.candidates().stream().allMatch(c->!c.runtimeLegalVerified()
                && !c.currentBuildConfirmed()));
        try { e.candidates().clear();throw new AssertionError("mutable"); }
        catch(UnsupportedOperationException expected){checks++;}
        try { find(e,MOVE,"swordsdance").listedMethods().add("fake");throw new AssertionError("mutable"); }
        catch(UnsupportedOperationException expected){checks++;}

        // Hisuian Zoroark has an EXPLICIT form moves array, but no explicit
        // ability list. Never inherit ordinary Zoroark's abilities silently.
        var hisui=source(12345679);
        String hLog=log("Zoroark-Hisui",move("Shadow Sneak"));
        var hAuth=authority(hisui,hLog);
        var hs=inspect(hisui,hLog,List.of(suggest(2,
                new String[]{"shadowsneak","nastyplot","uturn","falsenative"},
                new String[]{"illusion"},new String[]{"choicescarf"})),hAuth);
        var h=hs.appearances().get(0);
        check(h.displayedSpecies().equals("zoroarkhisui") && h.exactSourceForm());
        check(find(h,MOVE,"shadowsneak").sourceStatus()==LEVEL);
        check(find(h,MOVE,"nastyplot").sourceStatus()==LEVEL);
        check(find(h,MOVE,"uturn").sourceStatus()==LEVEL);
        check(find(h,MOVE,"falsenative").sourceStatus()==UNKNOWN);
        check(find(h,ABILITY,"illusion").sourceStatus()==UNSPECIFIED);
        check(find(h,ITEM,"choicescarf").sourceStatus()==UNKNOWN);

        // Ditto has level Transform and explicit normal/hidden abilities.
        var ditto=source(12345680);
        String dLog=log("Ditto",move("Transform"));
        var dAuth=authority(ditto,dLog);
        var dr=inspect(ditto,dLog,List.of(suggest(2,
                new String[]{"transform","tackle"},
                new String[]{"limber","imposter","illusion"},
                new String[]{"choiceband"})),dAuth);
        var d=dr.appearances().get(0);
        check(find(d,MOVE,"transform").sourceStatus()==LEVEL);
        check(find(d,MOVE,"tackle").sourceStatus()==UNKNOWN);
        check(find(d,ABILITY,"limber").sourceStatus()==
                PublicSpeciesBuildLegality.Status.SOURCE_LISTED_NORMAL_ABILITY);
        check(find(d,ABILITY,"imposter").sourceStatus()==
                PublicSpeciesBuildLegality.Status.SOURCE_LISTED_HIDDEN_ABILITY);
        check(find(d,ABILITY,"illusion").sourceStatus()==UNKNOWN);

        // Form with explicit abilities but no form move list must NEVER
        // borrow base-Mewtwo's learnset or claim a complete legal set.
        var mega=source(12345681);
        String mLog=log("Mewtwo-Mega-Y",move("Psystrike"));
        var mAuth=authority(mega,mLog);
        var mr=inspect(mega,mLog,List.of(suggest(2,
                new String[]{"psystrike","swift"},new String[]{"insomnia","pressure"},
                new String[]{"leftovers"})),mAuth);
        var m=mr.appearances().get(0);
        check(m.exactSourceForm());
        check(find(m,MOVE,"psystrike").sourceStatus()==UNSPECIFIED);
        check(find(m,MOVE,"swift").sourceStatus()==UNSPECIFIED);
        check(find(m,ABILITY,"insomnia").sourceStatus()==
                PublicSpeciesBuildLegality.Status.SOURCE_LISTED_NORMAL_ABILITY);
        check(find(m,ABILITY,"pressure").sourceStatus()==UNKNOWN);

        var pikachu=source(12345682);
        String pLog=log("Pikachu",move("Quick Attack"));
        var pAuth=authority(pikachu,pLog);
        var pr=inspect(pikachu,pLog,List.of(suggest(2,
                new String[]{"quickattack","thunder","shadowclaw"},
                new String[]{"static","lightningrod"},
                new String[]{})),pAuth);
        var p=pr.appearances().get(0);
        check(find(p,MOVE,"quickattack").sourceStatus()==LEVEL);
        check(find(p,MOVE,"thunder").sourceStatus()==LEVEL);
        check(find(p,MOVE,"shadowclaw").sourceStatus()==UNKNOWN);
        check(find(p,ABILITY,"lightningrod").sourceStatus()==
                PublicSpeciesBuildLegality.Status.SOURCE_LISTED_HIDDEN_ABILITY);

        // Unsupported external form cannot inherit known base-species facts.
        var unsupported=source(12345683);
        String unknownForm=log("Mewtwo-Mega-Z",move("Psystrike"));
        var ua=authority(unsupported,unknownForm);
        var ur=inspect(unsupported,unknownForm,List.of(suggest(2,
                new String[]{"psystrike"},new String[]{"pressure"},new String[]{})),ua);
        check(!ur.appearances().get(0).exactSourceForm());
        check(ur.appearances().get(0).candidates().stream().allMatch(c->
                c.sourceStatus()==UNKNOWN));

        // Appearance identity cannot be transferred by repeated display name,
        // even if both have a source-listed move.
        var repeatSource=source(12345684);
        String repeat=INTRO+"|switch|p1a: MyAlly|Pikachu, L100|100/100\n"
                +"|switch|p2a: SecretNick|Zoroark, L100|100/100\n"
                +"|turn|1\n"+move("Nasty Plot")
                +"|switch|p2a: SecretNick|Zoroark, L100|100/100\n"
                +"|turn|2\n"+move("Swift")
                +"|win|SYNTHETIC_PRIVATE_PERSON\n";
        var ra=authority(repeatSource,repeat);
        var rr=inspect(repeatSource,repeat,List.of(),ra);
        check(rr.appearances().size()==2);
        check(rr.appearances().get(0).index()==2 && rr.appearances().get(1).index()==3);
        check(rr.appearances().get(0).candidates().stream().anyMatch(c->
                c.id().equals("nastyplot") && c.publiclyObserved()));
        check(rr.appearances().get(1).candidates().stream().noneMatch(c->
                c.id().equals("nastyplot") && c.publiclyObserved()));
        check(rr.appearances().stream().allMatch(a->a.identityUncertain()
                && !a.confirmedSamePartyMember()));

        // A copied Transform move cannot be treated as a native learnset.
        var ts=source(12345685);
        String transformed=log("Ditto",
                "|-transform|p2a: SecretNick|p1a: MyAlly\n"+move("Tackle"));
        var ta=authority(ts,transformed);
        var tr=inspect(ts,transformed,List.of(suggest(2,
                new String[]{"transform","tackle"},new String[]{"imposter"},
                new String[]{})),ta);
        check(tr.appearances().get(0).transformed());
        check(tr.appearances().get(0).candidates().stream().allMatch(c->
                c.sourceStatus()==PublicSpeciesBuildLegality.Status.TRANSFORM_OR_IDENTITY_UNCERTAIN));
        check(tr.appearances().get(0).candidates().stream().noneMatch(
                PublicSpeciesBuildLegality.Candidate::currentBuildConfirmed));

        // All source pin, signature, evidence scope and expiry failures
        // propagate. No forged observation can promote a listed set.
        reject(()->PublicSpeciesBuildLegality.analyze(zoroark,zLog,candidates,
                null,zAuth.signed(),zAuth.policy(),NOW));
        reject(()->PublicSpeciesBuildLegality.analyze(zoroark,zLog,candidates,
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,zAuth.policy(),NOW));
        reject(()->PublicSpeciesBuildLegality.analyze(zoroark,zLog,candidates,
                PublicSpeciesBuildLegality.SourcePin.pinned(),zAuth.signed(),null,NOW));
        reject(()->PublicSpeciesBuildLegality.analyze(zoroark,zLog,candidates,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                zAuth.signed(),zAuth.policy(),NOW+1200));
        reject(()->inspect(zoroark,zLog.replace("Nasty Plot","Swords Dance"),candidates,zAuth));
        reject(()->inspect(zoroark,zLog+"|request|SYNTHETIC_PRIVATE_PERSON\n",candidates,zAuth));
        reject(()->inspect(zoroark,zLog+"|split|p2\n",candidates,zAuth));
        reject(()->inspect(zoroark,zLog,List.of(suggest(1,
                new String[]{"swordsdance"},new String[]{},new String[]{})),zAuth));
        reject(()->inspect(zoroark,zLog,List.of(suggest(99,
                new String[]{"swordsdance"},new String[]{},new String[]{})),zAuth));
        reject(()->inspect(zoroark,zLog,List.of(candidates.get(0),candidates.get(0)),zAuth));
        check(z.appearances().stream().noneMatch(a->a.candidates().stream().anyMatch(c->
                c.runtimeLegalVerified() || c.currentBuildConfirmed())));
        System.out.println("Phase 5.14 synthetic pinned build legality PASS: "+checks+" checks");
    }
}
