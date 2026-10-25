package com.maestroai.training;

import java.util.Set;

/** Synthetic public replay fragments, never training data or a Minecraft test. */
final class PublicReplayCorpusRegression {
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("Phase 5.1 replay contract");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SENSITIVE_TEST_SECRET")); return;
        }
        throw new AssertionError("Expected a sanitized rejection");
    }
    private static void immutable(Runnable action) {
        try { action.run(); }
        catch (UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError("Mutable replay evidence");
    }
    private static PublicReplayCorpus.Source approved() {
        return new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static final String LOG="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SENSITIVE_TEST_SECRET_PLAYER\n"
            +"|start\n|switch|p1a: SENSITIVE_TEST_SECRET_NICK|Pikachu, L100|100/100\n"
            +"|switch|p2a: OpponentNick|Zoroark, L100|100/100\n|turn|1\n"
            +"|move|p2a: OpponentNick|Nasty Plot|p1a: SENSITIVE_TEST_SECRET_NICK\n"
            +"|-ability|p2a: OpponentNick|Illusion\n|-item|p2a: OpponentNick|Leftovers\n"
            +"|chat|p1|SENSITIVE_TEST_SECRET_CHAT\n"
            +"|replace|p2a: OpponentNick|Zoroark, L100\n"
            +"|move|p2a: OpponentNick|Dark Pulse|p1a: SENSITIVE_TEST_SECRET_NICK\n"
            +"|switch|p2a: OpponentNick|Zoroark, L100|100/100\n|turn|2\n"
            +"|move|p2a: OpponentNick|U-turn|p1a: SENSITIVE_TEST_SECRET_NICK\n"
            +"|win|SENSITIVE_TEST_SECRET_PLAYER\n";
    static void run() {
        var e=PublicReplayCorpus.ingest(approved(),LOG);
        check(e.replayId().equals("gen9ou-12345678") && e.format().equals("gen9ou"));
        check(e.inputSha256().matches("[a-f0-9]{64}") && e.turns()==2);
        check(e.appearances().size()==3 && e.publicEventCount()>3);
        check(e.compatibility().contains("COBBLEMON_TRANSFER_UNVERIFIED"));
        var first=e.appearances().get(1);
        check(first.displayedSpecies().equals("zoroark"));
        check(first.nativeMoves().containsAll(Set.of("nastyplot","darkpulse")));
        check(first.revealedItem()==null && first.revealedAbility()==null);
        check(!first.identityLinked() && first.identityUncertain());
        check(!e.appearances().get(2).identityLinked());
        check(!e.toString().contains("SENSITIVE_TEST_SECRET") && !e.toString().contains("OpponentNick"));
        immutable(()->e.appearances().add(first));
        immutable(()->first.nativeMoves().add("fake"));
        check(e.equals(PublicReplayCorpus.ingest(approved(),LOG)));
        check(!e.inputSha256().equals(PublicReplayCorpus.ingest(approved(),
                LOG.replace("SENSITIVE_TEST_SECRET_PLAYER","ANOTHER_USER")).inputSha256()));
        var transform=PublicReplayCorpus.ingest(approved(),LOG.replace(
                "|move|p2a: OpponentNick|U-turn",
                "|-transform|p2a: OpponentNick|p1a: SENSITIVE_TEST_SECRET_NICK\n"
                +"|move|p2a: OpponentNick|U-turn"));
        check(transform.appearances().get(2).transformed());
        check(!transform.appearances().get(2).nativeMoves().contains("uturn"));
        reject(()->new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",false,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION));
        reject(()->new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",true,null));
        reject(()->new PublicReplayCorpus.Source("gen9ou-12345678","gen9ubers",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION));
        reject(()->new PublicReplayCorpus.Source("https://invalid","gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION));
        reject(()->PublicReplayCorpus.ingest(approved(),null));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG.replace("|gen|9","|gen|8")));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG.replace("|gametype|singles","|gametype|doubles")));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG.replace("[Gen 9] OU","[Gen 9] Ubers")));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG.replace("|win|","|nonsupported|")));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG.replace("|turn|2","|turn|0")));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"|request|SENSITIVE_TEST_SECRET\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"sideupdate\np1\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"|split|p1\n|secret|x\n|public|x\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"|pp_update|SENSITIVE_TEST_SECRET\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"|inputlog|SENSITIVE_TEST_SECRET\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"|showteam|SENSITIVE_TEST_SECRET\n"));
        reject(()->PublicReplayCorpus.ingest(approved(),LOG+"\0"));
        reject(()->PublicReplayCorpus.ingest(approved(),"x".repeat(2_000_001)));
        System.out.println("Phase 5.1 offline synthetic regressions PASS: "+checks+" checks");
    }
}
