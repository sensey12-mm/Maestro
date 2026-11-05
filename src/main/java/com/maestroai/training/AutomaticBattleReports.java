package com.maestroai.training;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local, crash-recoverable, observation-only writer for Maestro-owned battles.
 * Accepts ONLY the minimized capture/decision projections, never a raw Cobblemon
 * history, private request, trainer identity, UUID or log line.
 *
 * A live report is NOT a reviewed archive or proof of spectator delivery.
 * Disk failures fail closed and NEVER influence battle choice behavior.
 */
public final class AutomaticBattleReports implements AutoCloseable {
    public static final String PROPERTY = "maestroai.autoBattleReports";
    private static final String SCHEMA = "maestroai.auto-battle-report.v1";
    private static final int MAX_BATTLES = 999999;
    private static final int MAX_EVENTS = 4000;
    private static final int MAX_ISSUES_PER_BATTLE = 256;
    private static final Pattern B_FOLDER = Pattern.compile("B([0-9]{2,6})");
    private static final Pattern REPLAY = Pattern.compile(
            "\\[MaestroAI\\] SHADOW_REPLAY battle=REDACTED capture=([0-9]+) seq=([0-9]+) event=(.*)");
    private static final Pattern ENVELOPE = Pattern.compile(
            "\\[MaestroAI\\] SHADOW_ENVELOPE capture=([0-9]+) seq=([0-9]+) route=(DIRECT_UPDATE|SPLIT_PUBLIC|UNFRAMED_UNKNOWN) split_target=(p1|p2|NONE) spectator=EXPECTED_NOT_OBSERVED branch_attested=(true|false)");
    private static final Pattern GAP = Pattern.compile(
            "\\[MaestroAI\\] SHADOW_REPLAY_GAP battle=REDACTED capture=([0-9]+) after=([0-9]+)");
    private static final Pattern FAILURE = Pattern.compile(
            "\\[MaestroAI\\] SHADOW_ENVELOPE_FAILURE capture=([0-9]+) reason=([A-Z_]+) status=UNKNOWN");
    private static final Pattern TERMINAL = Pattern.compile(
            "\\[MaestroAI\\] SHADOW_TERMINAL capture=([0-9]+) after=([0-9]+) event=COBBLEMON_BATTLE_VICTORY winner=REDACTED beginning=UNKNOWN delivery=UNVERIFIED coverage=(INCOMPLETE|START_UNKNOWN)");
    private static final Pattern SLOT = Pattern.compile("p[12][a-f]");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> EVENT_TYPES = Set.of(
            "turn","move","switch","drag","replace","detailschange","-formechange",
            "-mega","faint","-boost","-unboost","-setboost","-clearboost",
            "-clearallboost","-copyboost","-swapboost","-invertboost",
            "-item","-enditem","-ability","-endability","-status",
            "-curestatus","-transform","-start","-end","-activate",
            "-terastallize","-damage","-heal","-sethp","-sidestart",
            "-sideend","-swapsideconditions","-weather","-fieldstart",
            "-fieldend","-dynamax","-zpower","-singleturn","-singlemove",
            "-prepare","-mustrecharge","cant","-block");
    private static final Set<String> SPECIES = Set.of(
            "zoroark","greninja","ditto","mew","pikachu","blissey","shedinja",
            "shuckle","genesect","calyrexshadow","arceus","xerneas","marshadow",
            "mewtwo","mewtwomegay","gholdengo","charizard","gengar","lucario");
    private static final Set<String> STATUS = Set.of("brn","par","psn","tox","slp","frz","none");
    private static final Set<String> TYPES = Set.of(
            "normal","fire","water","electric","grass","ice","fighting",
            "poison","ground","flying","psychic","bug","rock","ghost",
            "dragon","dark","steel","fairy","stellar");
    private static final Set<String> ITEMS = Set.of(
            "leftovers","choicescarf","choiceband","choicespecs","lifeorb",
            "powerherb","sitrusberry","focussash","assaultvest","blacksludge",
            "heavydutyboots","mewtwonitey","marshadiumz");
    private static final Set<String> ABILITIES = Set.of(
            "pressure","download","shadowshield","fairyaura","multitype",
            "technician","asone","levitate","intimidate","prankster");
    private static volatile AutomaticBattleReports installed;

    private final Path root;
    private final String version;
    private final String artifactSha;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final Map<Long, Battle> active = new HashMap<>();
    // Avoid duplicate records if a callback or diagnostic receipt is delivered
    // twice in the same game session (including with console flags toggled).
    private final Set<Long> finishedCaptures = new HashSet<>();
    private int nextNumber = 1;
    private boolean broken;
    private boolean closed;

    private static final class Battle {
        final String id;
        final Path dir;
        final Set<Long> replaySequences = new HashSet<>();
        final Set<Long> provenanceSequences = new HashSet<>();
        final Set<String> decisionReceipts = new HashSet<>();
        final Set<String> legacyReceipts = new HashSet<>();
        long lastReplaySeq;
        int records;
        int publicEvents;
        int decisions;
        int issues;
        int turn;
        boolean turnKnown;
        boolean gap;
        boolean capped;
        boolean issueCapped;
        String status = "ACTIVE_INCOMPLETE";
        Battle(String id, Path dir) { this.id = id; this.dir = dir; }
    }

    private AutomaticBattleReports(Path base, String version, String sha) throws IOException {
        if (Files.isSymbolicLink(base))
            throw new IOException("report root is a symlink");
        Files.createDirectories(base);
        root = base;
        this.version = version != null && version.matches("[0-9A-Za-z._-]{1,30}")
                ? version : "UNKNOWN";
        artifactSha = sha != null && HASH.matcher(sha).matches() ? sha : "UNKNOWN";
        Path lockPath = root.resolve(".writer.lock");
        if (Files.isSymbolicLink(lockPath)) throw new IOException("lock symlink");
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        FileLock acquired;
        try { acquired = channel.tryLock(); }
        catch (OverlappingFileLockException busy) { acquired = null; }
        if (acquired == null) {
            channel.close();
            throw new IOException("report folder already has a writer");
        }
        lockChannel = channel;
        lock = acquired;
        try {
            Path issues = root.resolve("issues.jsonl");
            if (Files.isSymbolicLink(issues)) throw new IOException("issues symlink");
            if (!Files.exists(issues)) Files.createFile(issues);
            recoverOldBattles();
        } catch (IOException failed) {
            lock.release();
            lockChannel.close();
            throw failed;
        }
    }

    /** Called once from Fabric initialization using FabricLoader's game dir. */
    public static synchronized void install(Path gameDir, String modVersion, String artifactSha) {
        if (installed != null) return;
        try {
            Path config = gameDir.resolve("config").resolve("maestroai-battle-reports.properties");
            boolean on = readEnabled(config);
            String override = System.getProperty(PROPERTY);
            if (override != null) {
                if (!override.equals("true") && !override.equals("false"))
                    throw new IOException("invalid configuration override");
                on = Boolean.parseBoolean(override);
            }
            if (!on) {
                System.out.println("[MaestroAI] Automatic battle reports disabled by configuration");
                return;
            }
            AutomaticBattleReports reporter = new AutomaticBattleReports(
                    gameDir.resolve("battle-reports"), modVersion, artifactSha);
            installed = reporter;
            Runtime.getRuntime().addShutdownHook(new Thread(reporter::close,
                    "maestroai-report-shutdown"));
            System.out.println("[MaestroAI] Automatic battle reports enabled folder=battle-reports");
        } catch (IOException | RuntimeException unavailable) {
            // Never expose a local filesystem path or prevent the mod loading.
            System.err.println("[MaestroAI] Automatic battle reports unavailable code=LOCAL_STORAGE");
        }
    }

    static boolean readEnabled(Path config) throws IOException {
        Path parent = config.getParent();
        if (Files.isSymbolicLink(parent)) throw new IOException("config folder symlink");
        Files.createDirectories(parent);
        if (Files.isSymbolicLink(config)) throw new IOException("config symlink");
        if (!Files.exists(config)) {
            try {
                Files.writeString(config,
                        "# Local MaestroAI battle reports (no uploads). Restart Minecraft to apply.\n"
                        + "enabled=true\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (java.nio.file.FileAlreadyExistsException concurrentlyCreated) {
                // Read the existing configuration instead of overwriting it.
            }
        }
        java.util.Properties properties = new java.util.Properties();
        try (var in = Files.newInputStream(config)) { properties.load(in); }
        String enabled = properties.getProperty("enabled", "true").trim();
        if (!enabled.equals("true") && !enabled.equals("false"))
            throw new IOException("invalid enabled value");
        return enabled.equals("true");
    }

    public static boolean enabled() {
        AutomaticBattleReports writer = installed;
        return writer != null && !writer.closed && !writer.broken;
    }

    // Production singleton with isolated temporary local reports for synthetic
    // no-flag regression; never used during normal Minecraft startup.
    static synchronized AutomaticBattleReports attachSyntheticForTest(Path gameDir,
                                                                       String version)
            throws IOException {
        if (installed != null) throw new IllegalStateException("writer already installed");
        AutomaticBattleReports writer = synthetic(gameDir, version);
        installed = writer;
        return writer;
    }
    static synchronized void detachSyntheticForTest(AutomaticBattleReports writer) {
        if (installed != writer) throw new IllegalStateException("writer mismatch");
        installed = null;
        writer.close();
    }

    /** Return-path wrappers: never throw into action selection. */
    public static void recordLines(long capture, List<String> lines) {
        AutomaticBattleReports writer = installed;
        if (writer != null) writer.acceptLines(capture, lines);
    }
    public static void recordDecision(long capture, String safeLine) {
        AutomaticBattleReports writer = installed;
        if (writer != null) writer.acceptDecision(capture, safeLine);
    }
    public static void recordLegacy(long capture, String safeReceipt) {
        AutomaticBattleReports writer = installed;
        if (writer != null) writer.acceptLegacy(capture, safeReceipt);
    }
    public static void recordTerminal(long capture, List<String> lines) {
        AutomaticBattleReports writer = installed;
        if (writer != null) writer.acceptTerminal(capture, lines);
    }
    public static void interrupt(long capture, String reason) {
        AutomaticBattleReports writer = installed;
        if (writer != null) writer.acceptInterrupt(capture, reason);
    }

    /** Single writer; keep the Minecraft hook side-effect-free on any failure. */
    private void guarded(RunnableIO action) {
        if (closed || broken) return;
        try { action.run(); }
        catch (IOException | RuntimeException unavailable) {
            broken = true;
            System.err.println("[MaestroAI] Automatic battle reports stopped code=WRITE_FAILURE");
        }
    }
    @FunctionalInterface private interface RunnableIO { void run() throws IOException; }

    private synchronized void acceptLines(long capture, List<String> lines) {
        guarded(() -> {
            if (capture <= 0 || lines == null || lines.isEmpty()
                    || finishedCaptures.contains(capture)) return;
            Battle battle = ensure(capture);
            for (String line : lines) {
                if (battle.capped) break;
                if (line == null || line.length() > 512) {
                    issue(battle, "INVALID_PROJECTION", "WARNING");
                    continue;
                }
                Matcher replay = REPLAY.matcher(line);
                Matcher envelope = ENVELOPE.matcher(line);
                Matcher gap = GAP.matcher(line);
                Matcher failure = FAILURE.matcher(line);
                if (replay.matches() && captureMatch(replay.group(1),capture)) {
                    long seq = sequence(replay.group(2));
                    if (seq <= 0) { issue(battle,"INVALID_SEQUENCE","WARNING"); continue; }
                    if (!battle.replaySequences.add(seq)) continue;
                    if (battle.lastReplaySeq != seq - 1) {
                        battle.gap = true;
                        issue(battle,"NONCONSECUTIVE_REPLAY","ERROR");
                    }
                    battle.lastReplaySeq = Math.max(battle.lastReplaySeq,seq);
                    String[] fields = safeEvent(replay.group(3));
                    if (fields[0].equals("UNKNOWN"))
                        issue(battle,"UNKNOWN_PUBLIC_EVENT","WARNING");
                    else if (fields[2].equals("UNKNOWN") && Set.of(
                            "turn","move","switch","drag","replace","detailschange",
                            "-formechange","-terastallize","-status","-curestatus",
                            "-item","-enditem","-ability","-endability")
                            .contains(fields[0]))
                        issue(battle,"UNKNOWN_PUBLIC_FIELD","WARNING");
                    if (fields[0].equals("turn") && !fields[2].equals("UNKNOWN")) {
                        battle.turn = Integer.parseInt(fields[2]);
                        battle.turnKnown = true;
                    }
                    battle.publicEvents++;
                    record(battle,"public_event",
                            ",\"sequence\":"+seq+",\"event\":"+json(fields[0])
                            +",\"actor\":"+json(fields[1])+",\"detail\":"+json(fields[2])
                            +",\"target\":"+json(fields[3]));
                    if (Set.of("replace","-transform","-terastallize","-formechange")
                            .contains(fields[0]))
                        record(battle,"identity",",\"evidence\":\"PUBLIC_APPEARANCE_ONLY\""
                                +",\"true_identity\":\"UNKNOWN\",\"uncertainty\":\"UNCERTAIN\""
                                +",\"event\":"+json(fields[0]));
                } else if (envelope.matches() && captureMatch(envelope.group(1),capture)) {
                    long seq = sequence(envelope.group(2));
                    if (seq <= 0 || !battle.provenanceSequences.add(seq)) continue;
                    record(battle,"provenance",",\"sequence\":"+seq
                            +",\"route\":"+json(envelope.group(3))
                            +",\"split_target\":"+json(envelope.group(4))
                            +",\"spectator_delivery\":\"UNVERIFIED\"");
                    if (envelope.group(3).equals("UNFRAMED_UNKNOWN"))
                        issue(battle,"UNFRAMED_PUBLIC_ENVELOPE","WARNING");
                } else if (gap.matches() && captureMatch(gap.group(1),capture)) {
                    battle.gap = true;
                    issue(battle,"PUBLIC_HISTORY_GAP","ERROR");
                } else if (failure.matches() && captureMatch(failure.group(1),capture)) {
                    battle.gap = true;
                    // Fixed code whitelist only, never raw message text.
                    issue(battle,"ENVELOPE_RECONCILIATION_FAILED","ERROR");
                } else {
                    issue(battle,"REJECTED_OBSERVATION","WARNING");
                }
            }
            summary(battle);
        });
    }

    private synchronized void acceptDecision(long capture, String line) {
        guarded(() -> {
            if (capture <= 0 || finishedCaptures.contains(capture)) return;
            if (line == null || !line.startsWith("[MaestroAI] DEV_DIAG_DECISION ")
                    || line.length() > 768) {
                Battle invalid = ensure(capture);
                issue(invalid, "INVALID_DECISION_RECEIPT", "WARNING");
                summary(invalid);
                return;
            }
            Battle b = ensure(capture);
            if (b.capped) return;
            Map<String,String> data = new HashMap<>();
            for (String piece : line.substring("[MaestroAI] DEV_DIAG_DECISION ".length()).split(";",-1)) {
                String[] pair = piece.split("=",2);
                if (pair.length != 2 || data.putIfAbsent(pair[0], pair[1]) != null) {
                    issue(b,"INVALID_DECISION","ERROR");
                    summary(b);
                    return;
                }
            }
            if (!String.valueOf(capture).equals(data.get("capture"))
                    || !Set.of("SHADOW","LEGACY").contains(data.getOrDefault("authority",""))
                    || !Set.of("true","false").contains(data.getOrDefault("forced",""))) {
                issue(b,"INVALID_DECISION","ERROR");
                summary(b);
                return;
            }
            boolean turnKnown = !"UNKNOWN".equals(data.get("turn"));
            int turn = turnKnown ? integer(data.get("turn"),10000) : -1;
            if (turnKnown && turn < 0) {
                issue(b,"INVALID_DECISION","ERROR"); summary(b); return;
            }
            // Accept only a single copy of this sanitized return-path receipt.
            // Distinct choices in the same turn remain independent.
            if (!b.decisionReceipts.add(line)) return;
            if (turnKnown) {
                b.turn = Math.max(b.turn,turn);
                b.turnKnown = true;
            }
            String action = DeveloperBattleDecisionCapture.safeAction(data.get("action"));
            String proposal = DeveloperBattleDecisionCapture.safeAction(data.get("proposal"));
            String reason = DeveloperBattleDecisionCapture.safeReason(data.get("reason"));
            String stage = DeveloperBattleDecisionCapture.safeReason(data.get("fallback_stage"));
            String fallback = DeveloperBattleDecisionCapture.safeReason(data.get("fallback_reason"));
            String legal = safeLegalFromReceipt(data.get("legal"));
            b.decisions++;
            record(b,"decision",",\"turn\":"
                    +(turnKnown ? Integer.toString(turn) : "null")
                    +",\"turn_evidence\":"+json(turnKnown ? "OWN_LEASE" : "UNKNOWN")
                    +",\"authority\":"+json(data.get("authority"))
                    +",\"selected_action\":"+json(action)+",\"proposal\":"+json(proposal)
                    +",\"forced\":"+data.get("forced")+",\"rationale\":"+json(reason)
                    +",\"fallback_stage\":"+json(stage)+",\"fallback_reason\":"+json(fallback)
                    +",\"legal_choices\":"+json(legal)
                    +",\"legal_evidence\":"+json(legal.equals("UNKNOWN")
                            ? "UNKNOWN" : "OWN_NATIVE_CERTIFIED")
                    +",\"confidence\":\"UNKNOWN\"");
            if (data.get("authority").equals("SHADOW"))
                record(b,"shadow_reason",",\"category\":"+json(reason)
                        +",\"confidence\":\"UNKNOWN\"");
            else
                record(b,"legacy_runtime_authority",
                        ",\"authority\":\"LEGACY\",\"selected_action\":"+json(action)
                        +",\"reason\":"+json(fallback));
            if (data.get("authority").equals("LEGACY") && !fallback.equals("UNKNOWN")) {
                record(b,"fallback",",\"turn\":"
                        +(turnKnown ? Integer.toString(turn) : "null")
                        +",\"reason\":"+json(fallback)
                        +",\"stage\":"+json(stage));
            }
            if (action.equals("UNKNOWN")) issue(b,"UNKNOWN_SELECTED_ACTION","WARNING");
            summary(b);
        });
    }

    private synchronized void acceptLegacy(long capture, String line) {
        guarded(() -> {
            if (capture <= 0 || finishedCaptures.contains(capture)) return;
            if (line == null || !line.startsWith("[MaestroAI] LEGACY_CHOICE_RECEIPT ")
                    || line.length() > 600) {
                Battle invalid = ensure(capture);
                issue(invalid, "INVALID_LEGACY_RECEIPT", "WARNING");
                summary(invalid);
                return;
            }
            String action = "UNKNOWN";
            Matcher matcher = Pattern.compile("(?:^| )action=(move:[a-z0-9_-]{1,64}|switch:slot[1-6])(?: |$)").matcher(line);
            if (matcher.find()) action = DeveloperBattleDecisionCapture.safeAction(matcher.group(1));
            Battle b = ensure(capture);
            if (b.capped || !b.legacyReceipts.add(line)) return;
            record(b,"legacy_receipt",",\"origin\":\"LEGACY_CHOOSE_RETURN\",\"action\":"
                    +json(action)+",\"custody\":\"REVIEW_REQUIRED\"");
            summary(b);
        });
    }

    private synchronized void acceptTerminal(long capture, List<String> lines) {
        guarded(() -> {
            if (capture <= 0 || lines == null || finishedCaptures.contains(capture)) return;
            // Victory is a real Cobblemon callback, but cannot certify
            // spectator delivery, replay beginning, or the winner identity.
            for (String line : lines) {
                if (line == null) continue;
                Matcher terminal = TERMINAL.matcher(line);
                if (!terminal.matches() || !captureMatch(terminal.group(1), capture)) continue;
                Battle b = ensure(capture);
                if (!b.replaySequences.equals(b.provenanceSequences)) {
                    b.gap = true;
                    issue(b,"ENVELOPE_SEQUENCE_MISMATCH","ERROR");
                }
                record(b,"terminal",",\"event\":\"COBBLEMON_BATTLE_VICTORY\""
                        +",\"winner\":\"REDACTED\",\"spectator_delivery\":\"UNVERIFIED\""
                        +",\"coverage\":"+json(terminal.group(3)));
                if (terminal.group(3).equals("INCOMPLETE")) b.gap = true;
                b.status = b.gap ? "COMPLETED_WITH_GAPS" : "COMPLETED_UNVERIFIED";
                record(b,"metadata",",\"terminal\":\"OBSERVED\""
                        +",\"completeness\":"+json(b.gap ? "INCOMPLETE" : "UNKNOWN")
                        +",\"spectator_delivery\":\"UNVERIFIED\"");
                summary(b);
                Files.deleteIfExists(b.dir.resolve("active.marker"));
                active.remove(capture);
                finishedCaptures.add(capture);
                return;
            }
            // Do not invent a terminal if the callback did not produce one.
        });
    }

    private synchronized void acceptInterrupt(long capture, String reason) {
        guarded(() -> {
            Battle b = active.remove(capture);
            if (b == null) return;
            markInterrupted(b, reason.equals("CAPACITY") ? "CAPACITY" : "REPLACED");
        });
    }

    private void markInterrupted(Battle b, String code) throws IOException {
        b.gap = true;
        b.status = "INTERRUPTED_" + code;
        issue(b, "INTERRUPTED_" + code, "WARNING");
        summary(b);
        Files.deleteIfExists(b.dir.resolve("active.marker"));
    }

    private Battle ensure(long capture) throws IOException {
        Battle existing = active.get(capture);
        if (existing != null) return existing;
        if (nextNumber > MAX_BATTLES) throw new IOException("battle folder limit");
        String id = String.format(Locale.ROOT, "B%02d", nextNumber++);
        Path dir = root.resolve(id);
        Files.createDirectory(dir); // never overwrite existing evidence
        Files.writeString(dir.resolve("active.marker"), "ACTIVE\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Battle b = new Battle(id,dir);
        active.put(capture,b);
        record(b,"diagnostic_presence",",\"category\":\"AUTOMATIC_JAVA_REPORTER\""
                +",\"content\":\"UNKNOWN\"");
        summary(b);
        return b;
    }

    private void recoverOldBattles() throws IOException {
        int highest = 0;
        List<Path> abandoned = new ArrayList<>();
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(root)) {
            for (Path folder : folders) {
                Matcher name = B_FOLDER.matcher(folder.getFileName().toString());
                if (!name.matches()) continue;
                int value = Integer.parseInt(name.group(1));
                highest = Math.max(highest, value);
                if (!Files.isSymbolicLink(folder) && Files.isDirectory(folder,LinkOption.NOFOLLOW_LINKS)
                        && Files.isRegularFile(folder.resolve("active.marker"),LinkOption.NOFOLLOW_LINKS))
                    abandoned.add(folder);
            }
        }
        nextNumber = highest + 1;
        for (Path folder : abandoned) {
            String id = folder.getFileName().toString();
            Path summary = folder.resolve("summary.md");
            if (Files.isSymbolicLink(summary) || Files.isSymbolicLink(folder.resolve("active.marker")))
                continue;
            // The last process could have persisted the real terminal but
            // crashed before removing its active.marker. Preserve that stronger
            // evidence instead of falsely relabeling the battle as interrupted.
            boolean completedCallback = persistedTerminal(folder.resolve("battle.jsonl"));
            atomicSummary(summary, "# MaestroAI battle " + id + "\n\n"
                    + "- Status: " + (completedCallback
                        ? "COMPLETED_CALLBACK_RECOVERED" : "INTERRUPTED_RESTART") + "\n"
                    + "- Completeness: " + (completedCallback
                        ? "UNKNOWN (callback persisted; delivery unverified)"
                        : "INCOMPLETE (no attested terminal)") + "\n"
                    + "- Spectator delivery: UNVERIFIED\n"
                    + "- Original log custody: LOCAL_UNREVIEWED\n"
                    + "- Events and decisions: inspect surviving battle.jsonl\n");
            if (!completedCallback)
                append(root.resolve("issues.jsonl"), "{\"schema\":"+json(SCHEMA)
                        +",\"battle\":"+json(id)+",\"code\":\"UNCLEAN_RESTART\""
                        +",\"severity\":\"WARNING\"}\n");
            Files.deleteIfExists(folder.resolve("active.marker"));
        }
    }

    /** Read only the writer's own bounded, already-minimized terminal records. */
    private static boolean persistedTerminal(Path jsonl) throws IOException {
        if (!Files.isRegularFile(jsonl, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(jsonl) || Files.size(jsonl) > 5_000_000)
            return false;
        try (var input = Files.newBufferedReader(jsonl, StandardCharsets.UTF_8)) {
            String line;
            int count = 0;
            while ((line = input.readLine()) != null && ++count <= MAX_EVENTS + 100) {
                if (line.length() > 1500 || !line.endsWith("}")) continue;
                if (line.startsWith("{\"schema\":\"maestroai.auto-battle-report.v1\"")
                        && line.contains("\"kind\":\"terminal\"")
                        && line.contains("\"event\":\"COBBLEMON_BATTLE_VICTORY\""))
                    return true;
            }
        }
        return false;
    }

    private void issue(Battle b, String code, String severity) throws IOException {
        if (b.issues >= MAX_ISSUES_PER_BATTLE) {
            if (!b.issueCapped) {
                b.issueCapped = true;
                b.gap = true;
                b.issues++;
                append(root.resolve("issues.jsonl"),
                        "{\"schema\":"+json(SCHEMA)+",\"battle\":"+json(b.id)
                        +",\"code\":\"ISSUE_LIMIT_REACHED\",\"severity\":\"ERROR\"}\n");
            }
            return;
        }
        b.issues++;
        String json = "{\"schema\":"+json(SCHEMA)+",\"battle\":"+json(b.id)
                +",\"code\":"+json(code)+",\"severity\":"+json(severity)+"}\n";
        append(root.resolve("issues.jsonl"),json);
        if (b.records < MAX_EVENTS)
            record(b,"issue",",\"code\":"+json(code)+",\"severity\":"+json(severity));
    }

    private void record(Battle b, String kind, String extra) throws IOException {
        if (b.records >= MAX_EVENTS && !kind.equals("terminal")) {
            if (!b.capped) {
                b.capped = true;
                b.gap = true;
                issue(b,"EVENT_LIMIT_REACHED","ERROR");
            }
            return;
        }
        b.records++;
        append(b.dir.resolve("battle.jsonl"),
                "{\"schema\":"+json(SCHEMA)+",\"battle\":"+json(b.id)
                +",\"order\":"+b.records+",\"kind\":"+json(kind)
                +",\"version\":"+json(version)+",\"artifact_sha256\":"+json(artifactSha)
                +",\"source_line\":null,\"source_sha256\":\"UNKNOWN\""
                +",\"public_beginning\":\"UNKNOWN\",\"custody\":\"LOCAL_UNREVIEWED\""
                +extra+"}\n");
    }

    private void summary(Battle b) throws IOException {
        atomicSummary(b.dir.resolve("summary.md"),
                "# MaestroAI battle " + b.id + "\n\n"
                + "- Status: " + b.status + "\n"
                + "- Completeness: " + (b.gap ? "INCOMPLETE" : "UNKNOWN") + "\n"
                + "- Public events: " + b.publicEvents + "\n"
                + "- Decisions: " + b.decisions + "\n"
                + "- Issues: " + b.issues + "\n"
                + "- Last observed turn: "
                + (b.turnKnown ? Integer.toString(b.turn) : "UNKNOWN") + "\n"
                + "- Version: " + version + "\n"
                + "- Spectator delivery: UNVERIFIED\n"
                + "- Original log custody: LOCAL_UNREVIEWED\n"
                + "- Winner: UNKNOWN\n");
    }

    private static void atomicSummary(Path dest, String text) throws IOException {
        if (Files.isSymbolicLink(dest)) throw new IOException("summary symlink");
        Path temp = dest.resolveSibling("summary.tmp");
        if (Files.isSymbolicLink(temp)) throw new IOException("temp summary symlink");
        Files.writeString(temp,text,StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }

    private static void append(Path path, String text) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("report file symlink");
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND,LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer data = ByteBuffer.wrap(bytes);
            while (data.hasRemaining()) file.write(data);
            file.force(true); // event survives ordinary shutdown and most crashes
        }
    }

    private static long sequence(String s) {
        try {
            long value = Long.parseLong(s);
            return value > 0 && value <= 10000000 ? value : -1;
        } catch (NumberFormatException invalid) { return -1; }
    }
    private static boolean captureMatch(String s, long expected) {
        return sequence(s) == expected;
    }
    private static int integer(String s,int max) {
        try {
            int value = Integer.parseInt(s);
            return value >= 0 && value <= max ? value : -1;
        } catch (RuntimeException invalid) { return -1; }
    }
    private static String safeLegalFromReceipt(String text) {
        if (text == null || text.equals("UNKNOWN")) return "UNKNOWN";
        String[] tokens = text.split(",",-1);
        return DeveloperBattleDecisionCapture.safeLegal(List.of(tokens));
    }

    private static String[] safeEvent(String value) {
        String[] fallback = {"UNKNOWN","UNKNOWN","UNKNOWN","UNKNOWN"};
        if (value == null || value.length() > 150 || !value.startsWith("|")) return fallback;
        String[] fields = value.split("\\|",-1);
        if (fields.length < 2 || !EVENT_TYPES.contains(fields[1])) return fallback;
        String kind = fields[1];
        if (kind.equals("turn")) {
            int turn = integer(fields.length > 2 ? fields[2] : null,10000);
            return new String[]{kind,"UNKNOWN",turn < 0 ? "UNKNOWN" : String.valueOf(turn),"UNKNOWN"};
        }
        String actor = fields.length > 2 && SLOT.matcher(fields[2]).matches()
                ? fields[2] : "UNKNOWN";
        String detail = "UNKNOWN";
        if (fields.length > 3) {
            String candidate = fields[3];
            if (kind.equals("move") && DeveloperBattleDecisionCapture.safeAction(
                    "move:"+candidate).equals("move:"+candidate))
                detail = candidate;
            else if (Set.of("switch","drag","replace","detailschange","-formechange")
                    .contains(kind) && SPECIES.contains(candidate))
                detail = candidate;
            else if (kind.equals("-terastallize") && TYPES.contains(candidate))
                detail = candidate;
            else if (Set.of("-status","-curestatus").contains(kind) && STATUS.contains(candidate))
                detail = candidate;
            else if (Set.of("-item","-enditem").contains(kind) && ITEMS.contains(candidate))
                detail = candidate;
            else if (Set.of("-ability","-endability").contains(kind) && ABILITIES.contains(candidate))
                detail = candidate;
        }
        String target = fields.length > 4 && SLOT.matcher(fields[4]).matches()
                ? fields[4] : "UNKNOWN";
        return new String[]{kind,actor,detail,target};
    }

    private static String json(String text) {
        // Every caller supplies a fixed enum, digit or independently allowlisted
        // token; this escaping is defense in depth, NOT a free-form export gate.
        StringBuilder builder = new StringBuilder("\"");
        for (int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if (ch=='\\' || ch=='\"') builder.append('\\').append(ch);
            else if (ch < 32) builder.append('?');
            else builder.append(ch);
        }
        return builder.append('\"').toString();
    }

    @Override public synchronized void close() {
        if (closed) return;
        if (!broken) {
            for (Battle b : List.copyOf(active.values())) {
                try { markInterrupted(b,"SHUTDOWN"); }
                catch (IOException failed) { broken = true; break; }
            }
        }
        active.clear();
        closed = true;
        try { lock.release(); lockChannel.close(); }
        catch (IOException ignored) { /* preserve unfinished marker for restart */ }
    }

    // Synthetic regression entry: isolated from the production static singleton.
    public static AutomaticBattleReports synthetic(Path gameDir, String version) throws IOException {
        return new AutomaticBattleReports(gameDir.resolve("battle-reports"),version,"UNKNOWN");
    }
    public synchronized void simulateCrashForTest() throws IOException {
        closed = true;
        active.clear();
        lock.release();
        lockChannel.close();
    }
    public void testLines(long capture, List<String> lines) { acceptLines(capture,lines); }
    public void testDecision(long capture, String line) { acceptDecision(capture,line); }
    public void testTerminal(long capture, List<String> lines) { acceptTerminal(capture,lines); }
    public void testInterrupt(long capture, String reason) { acceptInterrupt(capture,reason); }
}
