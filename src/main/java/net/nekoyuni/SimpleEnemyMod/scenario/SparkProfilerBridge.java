package net.nekoyuni.SimpleEnemyMod.scenario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional bridge to Spark's command interface.
 *
 * <p>The mod must still compile and run without Spark, so this class deliberately avoids direct Spark API imports.
 * It starts/stops Spark through the server command dispatcher and captures the generated viewer URL from command output
 * or, when Spark reports to player chat, from latest.log as a fallback.</p>
 */
public final class SparkProfilerBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Map<String, ProfileRun> RUNS = new ConcurrentHashMap<>();
    private static final Map<String, LoopRun> LOOP_RUNS = new ConcurrentHashMap<>();

    @Nullable
    private static volatile String activeLoopRunId;

    private SparkProfilerBridge() {
    }

    public static boolean isSparkCommandAvailable(MinecraftServer server) {
        return server != null && server.getCommands().getDispatcher().getRoot().getChild("spark") != null;
    }

    public static void startLoopAllRun(MinecraftServer server, Path minecraftRoot, String loopRunId) {
        startLoopAllRun(server, minecraftRoot, loopRunId, "_loop_all", "loop_all");
    }

    public static void startLoopAllRun(MinecraftServer server, Path minecraftRoot, String loopRunId, String directoryName, String displayName) {
        if (!TacticalAiConfig.enableSemLoopSparkProfiling() || server == null || minecraftRoot == null || loopRunId == null || loopRunId.isBlank()) {
            return;
        }
        LoopRun run = new LoopRun(loopRunId, minecraftRoot, safeSuiteDirectory(directoryName), displayName != null && !displayName.isBlank() ? displayName : "loop_all");
        LOOP_RUNS.put(loopRunId, run);
        activeLoopRunId = loopRunId;
        run.writeLifecycle("LOOP_SPARK_START", "started", 0, 0);
        LOGGER.info("SEM {} Spark profile collection started: runId={} log={}", run.displayName, loopRunId, run.logFile);
    }

    public static void finishLoopAllRun(@Nullable String loopRunId, String reason) {
        if (loopRunId == null || loopRunId.isBlank()) {
            return;
        }
        LoopRun run = LOOP_RUNS.remove(loopRunId);
        if (loopRunId.equals(activeLoopRunId)) {
            activeLoopRunId = null;
        }
        if (run == null) {
            return;
        }
        run.finish(reason != null ? reason : "completed");
    }

    /** Backward-compatible entry point for older runtime code. */
    public static void startLoopScenarioProfile(ServerLevel level, SemScenarioDefinition scenario, String sessionId, Path logFile, long tick) {
        startLoopScenarioProfile(level, scenario, sessionId, logFile, tick, null);
    }

    /**
     * Starts Spark for one loop_all scenario. When a real player command source is available, Spark is executed
     * as that player because Spark posts viewer links to the command sender's chat path.
     */
    public static void startLoopScenarioProfile(ServerLevel level, SemScenarioDefinition scenario, String sessionId, Path logFile, long tick, @Nullable CommandSourceStack playerCommandSource) {
        if (!TacticalAiConfig.enableSemLoopSparkProfiling() || level == null || scenario == null || sessionId == null || logFile == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (!isSparkCommandAvailable(server)) {
            LOGGER.info("SEM loop Spark profiling requested, but Spark command is not registered.");
            return;
        }

        ProfileRun run = new ProfileRun(scenario.id(), sessionId, logFile, tick, activeLoopRunId, playerCommandSource);
        RUNS.put(sessionId, run);
        LoopRun loopRun = run.loopRunId != null ? LOOP_RUNS.get(run.loopRunId) : null;
        if (loopRun != null) {
            loopRun.recordScenarioStarted(run);
        }

        SemScenarioLogger.appendSparkStartLine(logFile, scenario.id(), sessionId, tick, run.profileId);
        int result = executeSparkCommand(level, run, "spark profiler start");
        LOGGER.info("Started Spark profiler for SEM scenario {} session {} profileId={} result={}", scenario.id(), sessionId, run.profileId, result);
    }

    public static void stopLoopScenarioProfile(ServerLevel level, @Nullable String sessionId, long tick) {
        if (!TacticalAiConfig.enableSemLoopSparkProfiling() || level == null || sessionId == null) {
            return;
        }
        ProfileRun run = RUNS.get(sessionId);
        if (run == null) {
            return;
        }
        run.endTick = tick;
        SemScenarioLogger.appendSparkStopLine(run.logFile, run.scenarioId, run.sessionId, tick, run.profileId);
        if (!isSparkCommandAvailable(level.getServer())) {
            return;
        }

        int result = executeSparkCommand(level, run, "spark profiler stop");
        LOGGER.info("Stopped Spark profiler for SEM scenario {} session {} profileId={} result={}", run.scenarioId, sessionId, run.profileId, result);
        recoverSparkUrlFromLatestLog(level.getServer(), run);
        run.tryAppendSparkUrl();
    }

    public static void markScenarioLogClosed(@Nullable String sessionId) {
        if (sessionId == null) {
            return;
        }
        ProfileRun run = RUNS.get(sessionId);
        if (run == null) {
            return;
        }
        run.logClosed = true;
        if (run.url == null || run.url.isBlank()) {
            recoverSparkUrlFromLatestLog(null, run);
        }
        run.tryAppendSparkUrl();
    }

    private static int executeSparkCommand(ServerLevel level, ProfileRun run, String command) {
        try {
            CommandSourceStack source = run.playerCommandSource;
            if (source != null) {
                return level.getServer().getCommands().performPrefixedCommand(source, command);
            }

            CommandSourceStack fallbackSource = new CommandSourceStack(
                    new CapturingCommandSource(run),
                    Vec3.ZERO,
                    Vec2.ZERO,
                    level,
                    4,
                    "SEM Spark",
                    Component.literal("SEM Spark"),
                    level.getServer(),
                    null
            );
            return level.getServer().getCommands().performPrefixedCommand(fallbackSource, command);
        } catch (Exception e) {
            LOGGER.warn("Failed to execute Spark command '{}' for SEM scenario {} session {}", command, run.scenarioId, run.sessionId, e);
            return 0;
        }
    }

    private static void recoverSparkUrlFromLatestLog(@Nullable MinecraftServer server, ProfileRun run) {
        if (run == null || (run.url != null && !run.url.isBlank())) {
            return;
        }
        Path minecraftRoot = null;
        LoopRun loopRun = run.loopRunId != null ? LOOP_RUNS.get(run.loopRunId) : null;
        if (loopRun != null) {
            minecraftRoot = loopRun.minecraftRoot;
        }
        if (minecraftRoot == null) {
            return;
        }

        Path latestLog = minecraftRoot.resolve("logs").resolve("latest.log");
        if (!Files.isRegularFile(latestLog)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(latestLog, StandardCharsets.UTF_8);
            List<String> urls = new ArrayList<>();
            for (String line : lines) {
                Matcher matcher = URL_PATTERN.matcher(line);
                while (matcher.find()) {
                    String url = trimUrl(matcher.group());
                    if (url.contains("spark.lucko.me")) {
                        urls.add(url);
                    }
                }
            }
            if (urls.isEmpty()) {
                return;
            }

            Set<String> alreadyAssigned = new HashSet<>();
            if (loopRun != null) {
                synchronized (loopRun.records) {
                    for (LoopSparkRecord record : loopRun.records) {
                        if (record.url != null && !record.url.isBlank()) {
                            alreadyAssigned.add(record.url);
                        }
                    }
                }
            }

            String selected = null;
            for (int i = urls.size() - 1; i >= 0; i--) {
                String candidate = urls.get(i);
                if (!alreadyAssigned.contains(candidate)) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null) {
                selected = urls.get(urls.size() - 1);
            }

            run.url = selected;
            if (loopRun != null) {
                loopRun.recordScenarioUrl(run, selected);
            }
            LOGGER.info("Recovered SEM Spark URL from latest.log for scenario {} session {}: {}", run.scenarioId, run.sessionId, selected);
        } catch (Exception e) {
            LOGGER.debug("Failed to recover SEM Spark URL from latest.log for scenario {} session {}", run.scenarioId, run.sessionId, e);
        }
    }

    @Nullable
    private static String extractUrl(@Nullable Component message) {
        if (message == null) {
            return null;
        }

        String fromText = firstUrl(message.getString());
        if (fromText != null) {
            return fromText;
        }

        String fromStyle = firstUrlFromClickEvent(message.getStyle().getClickEvent());
        if (fromStyle != null) {
            return fromStyle;
        }

        for (Component sibling : message.getSiblings()) {
            String fromSibling = extractUrl(sibling);
            if (fromSibling != null) {
                return fromSibling;
            }
        }

        return null;
    }

    @Nullable
    private static String firstUrlFromClickEvent(@Nullable ClickEvent clickEvent) {
        if (clickEvent == null) {
            return null;
        }
        return firstUrl(clickEvent.getValue());
    }

    @Nullable
    private static String firstUrl(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return trimUrl(matcher.group());
    }

    private static String trimUrl(String url) {
        String result = url;
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last == ')' || last == ']' || last == '}' || last == ',' || last == ';' || last == '.') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }
        return result;
    }

    private static String safeSuiteDirectory(String directoryName) {
        String value = directoryName == null || directoryName.isBlank() ? "_loop_all" : directoryName.trim();
        value = value.replaceAll("[^A-Za-z0-9_-]", "_");
        if (!value.startsWith("_")) {
            value = "_" + value;
        }
        return value;
    }

    private static final class CapturingCommandSource implements CommandSource {
        private final ProfileRun run;

        private CapturingCommandSource(ProfileRun run) {
            this.run = run;
        }

        @Override
        public void sendSystemMessage(Component message) {
            String url = extractUrl(message);
            if (url != null && !url.isBlank()) {
                this.run.url = url;
                this.run.tryAppendSparkUrl();
                LoopRun loopRun = this.run.loopRunId != null ? LOOP_RUNS.get(this.run.loopRunId) : null;
                if (loopRun != null) {
                    loopRun.recordScenarioUrl(this.run, url);
                }
            }
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }

    private static final class ProfileRun {
        private final String scenarioId;
        private final String sessionId;
        private final Path logFile;
        private final long startTick;
        private final String profileId;
        @Nullable
        private final String loopRunId;
        @Nullable
        private final CommandSourceStack playerCommandSource;
        private volatile long endTick;
        private volatile String url;
        private volatile boolean logClosed;
        private volatile boolean profileLogged;

        private ProfileRun(String scenarioId, String sessionId, Path logFile, long startTick, @Nullable String loopRunId, @Nullable CommandSourceStack playerCommandSource) {
            this.scenarioId = scenarioId;
            this.sessionId = sessionId;
            this.logFile = logFile;
            this.startTick = startTick;
            this.profileId = scenarioId + "_" + sessionId;
            this.endTick = startTick;
            this.loopRunId = loopRunId;
            this.playerCommandSource = playerCommandSource;
        }

        private void tryAppendSparkUrl() {
            if (this.profileLogged || this.url == null || this.url.isBlank()) {
                return;
            }
            this.profileLogged = true;
            SemScenarioLogger.appendSparkProfileLine(this.logFile, this.scenarioId, this.sessionId, this.endTick, this.profileId, this.url);
            LoopRun loopRun = this.loopRunId != null ? LOOP_RUNS.get(this.loopRunId) : null;
            if (loopRun != null) {
                loopRun.recordScenarioUrl(this, this.url);
            }
            RUNS.remove(this.sessionId);
        }
    }

    private static final class LoopRun {
        private final String loopRunId;
        private final Path minecraftRoot;
        private final Path logFile;
        private final Path markdownReportFile;
        private final String directoryName;
        private final String displayName;
        private final List<LoopSparkRecord> records = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean finished;

        private LoopRun(String loopRunId, Path minecraftRoot, String directoryName, String displayName) {
            this.loopRunId = loopRunId;
            this.minecraftRoot = minecraftRoot;
            this.directoryName = directoryName;
            this.displayName = displayName;
            Path directory = minecraftRoot.resolve("logs").resolve("sem_scenarios").resolve(directoryName);
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                LOGGER.warn("Failed to create SEM loop_all Spark profile directory {}", directory, e);
            }
            String prefix = FILE_TIME.format(LocalDateTime.now()) + "_" + loopRunId;
            this.logFile = directory.resolve(prefix + "_spark_profiles.jsonl");
            this.markdownReportFile = directory.resolve(prefix + "_performance_report.md");
        }

        private void recordScenarioStarted(ProfileRun run) {
            synchronized (this.records) {
                LoopSparkRecord existing = findRecord(run.sessionId);
                if (existing == null) {
                    this.records.add(new LoopSparkRecord(run.scenarioId, run.sessionId, run.profileId, run.logFile, run.startTick, run.endTick));
                }
            }
        }

        private void recordScenarioUrl(ProfileRun run, String url) {
            LoopSparkRecord record;
            boolean changed;
            synchronized (this.records) {
                record = findRecord(run.sessionId);
                if (record == null) {
                    record = new LoopSparkRecord(run.scenarioId, run.sessionId, run.profileId, run.logFile, run.startTick, run.endTick);
                    this.records.add(record);
                }
                record.endTick = run.endTick;
                changed = record.url == null || !record.url.equals(url);
                record.url = url;
            }
            if (changed) {
                appendRecord("LOOP_SPARK_PROFILE", record, null);
            }
        }

        @Nullable
        private LoopSparkRecord findRecord(String sessionId) {
            for (LoopSparkRecord record : this.records) {
                if (record.sessionId.equals(sessionId)) {
                    return record;
                }
            }
            return null;
        }

        private void finish(String reason) {
            if (this.finished) {
                return;
            }
            this.finished = true;

            List<LoopSparkRecord> snapshot;
            synchronized (this.records) {
                snapshot = new ArrayList<>(this.records);
            }

            long withUrl = snapshot.stream().filter(record -> record.url != null && !record.url.isBlank()).count();
            writeLifecycle("LOOP_SPARK_SUMMARY", reason, snapshot.size(), withUrl);

            LOGGER.info("SEM scenario suite Spark profile summary: runId={} profilesWithUrl={}/{} log={}", this.loopRunId, withUrl, snapshot.size(), this.logFile);
            for (int i = 0; i < snapshot.size(); i++) {
                LoopSparkRecord record = snapshot.get(i);
                String url = record.url != null && !record.url.isBlank() ? record.url : "NO_URL_CAPTURED";
                LOGGER.info("SEM scenario suite Spark profile [{}/{}] {} session={} {}", i + 1, snapshot.size(), record.scenarioId, record.sessionId, url);
                appendRecord("LOOP_SPARK_PROFILE_FINAL", record, reason);
            }

            writeMarkdownPerformanceReport(reason, snapshot, withUrl);
        }

        private void writeLifecycle(String type, String reason, long scenarioCount, long urlCount) {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("loopRunId", this.loopRunId);
            json.addProperty("suite", this.displayName);
            json.addProperty("directory", this.directoryName);
            json.addProperty("reason", reason);
            json.addProperty("scenarioCount", scenarioCount);
            json.addProperty("urlCount", urlCount);
            json.addProperty("logFile", this.logFile.toString());
            if (this.markdownReportFile != null) {
                json.addProperty("markdownReportFile", this.markdownReportFile.toString());
            }
            appendJson(json);
        }

        private void appendRecord(String type, LoopSparkRecord record, @Nullable String reason) {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("loopRunId", this.loopRunId);
            json.addProperty("suite", this.displayName);
            json.addProperty("directory", this.directoryName);
            json.addProperty("scenario", record.scenarioId);
            json.addProperty("sessionId", record.sessionId);
            json.addProperty("profileId", record.profileId);
            json.addProperty("startTick", record.startTick);
            json.addProperty("endTick", record.endTick);
            json.addProperty("url", record.url != null && !record.url.isBlank() ? record.url : "NO_URL_CAPTURED");
            json.addProperty("scenarioLogFile", record.logFile != null ? record.logFile.toString() : "unknown");
            if (reason != null) {
                json.addProperty("reason", reason);
            }
            appendJson(json);
        }

        private void writeMarkdownPerformanceReport(String reason, List<LoopSparkRecord> snapshot, long withUrl) {
            List<ScenarioStats> stats = new ArrayList<>();
            for (LoopSparkRecord record : snapshot) {
                stats.add(ScenarioStats.from(record));
            }

            StringBuilder md = new StringBuilder(16_384);
            md.append("# SEM ").append(escapeMarkdownText(this.displayName)).append(" Performance Report\n\n");
            md.append("- Suite: `").append(escapeMarkdownInline(this.displayName)).append("`\n");
            md.append("- Directory: `").append(escapeMarkdownInline(this.directoryName)).append("`\n");
            md.append("- Loop run: `").append(escapeMarkdownInline(this.loopRunId)).append("`\n");
            md.append("- Reason: `").append(escapeMarkdownInline(reason)).append("`\n");
            md.append("- Scenarios: ").append(snapshot.size()).append("\n");
            md.append("- Spark URLs: ").append(withUrl).append(" / ").append(snapshot.size()).append("\n");
            md.append("- Spark JSONL: `").append(escapeMarkdownInline(this.logFile.toString())).append("`\n\n");

            md.append("## Scenario summary\n\n");
            md.append("| # | Scenario | Winner | Duration | Warns | Severe | Max tick | Avg sample | Cover R/S/A | Max path checks | Spark |\n");
            md.append("|---:|---|---|---:|---:|---:|---:|---:|---|---:|---|\n");
            for (int i = 0; i < stats.size(); i++) {
                ScenarioStats stat = stats.get(i);
                LoopSparkRecord record = stat.record;
                md.append("| ").append(i + 1)
                        .append(" | ").append(escapeTable(record.scenarioId))
                        .append(" | ").append(escapeTable(stat.winner))
                        .append(" | ").append(stat.durationTicks >= 0 ? stat.durationTicks : Math.max(0L, record.endTick - record.startTick))
                        .append(" | ").append(stat.tickWarns)
                        .append(" | ").append(stat.tickSevere)
                        .append(" | ").append(formatMs(stat.maxTickMs))
                        .append(" | ").append(formatMs(stat.avgTickMs()))
                        .append(" | ").append(stat.coverReached).append('/').append(stat.coverSelected).append('/').append(stat.coverAborted)
                        .append(" | ").append(stat.maxCoverPathChecksThisTick)
                        .append(" | ").append(formatSparkLink(record.url))
                        .append(" |\n");
            }

            md.append("\n## Spark URLs\n\n");
            for (int i = 0; i < snapshot.size(); i++) {
                LoopSparkRecord record = snapshot.get(i);
                md.append(i + 1).append(". **").append(escapeMarkdownText(record.scenarioId)).append("** — ");
                if (record.url != null && !record.url.isBlank()) {
                    md.append(record.url);
                } else {
                    md.append("NO_URL_CAPTURED");
                }
                md.append("\n");
            }

            md.append("\n## Notes\n\n");
            md.append("- `Max tick` and `Avg sample` are derived from SEM `TICK_SAMPLE` / `TICK_WARN` JSONL events, not from Spark internals.\n");
            md.append("- `Cover R/S/A` means `COVER_REACHED / COVER_SELECTED / COVER_ABORTED`.\n");
            md.append("- Use Spark links for flamegraph details; use this report for quick regression triage.\n");

            try (BufferedWriter writer = Files.newBufferedWriter(this.markdownReportFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(md.toString());
            } catch (IOException e) {
                LOGGER.warn("Failed to write SEM loop_all Markdown performance report {}", this.markdownReportFile, e);
                return;
            }

            JsonObject json = new JsonObject();
            json.addProperty("type", "LOOP_PERFORMANCE_REPORT");
            json.addProperty("loopRunId", this.loopRunId);
            json.addProperty("suite", this.displayName);
            json.addProperty("directory", this.directoryName);
            json.addProperty("scenarioCount", snapshot.size());
            json.addProperty("urlCount", withUrl);
            json.addProperty("markdownReportFile", this.markdownReportFile.toString());
            appendJson(json);

            LOGGER.info("SEM scenario suite Markdown performance report written: {}", this.markdownReportFile);
        }

        private void appendJson(JsonObject json) {
            try (BufferedWriter writer = Files.newBufferedWriter(this.logFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(GSON.toJson(json));
                writer.newLine();
            } catch (IOException e) {
                LOGGER.warn("Failed to write SEM loop_all Spark profile summary log {}", this.logFile, e);
            }
        }
    }

    private static final class LoopSparkRecord {
        private final String scenarioId;
        private final String sessionId;
        private final String profileId;
        @Nullable
        private final Path logFile;
        private final long startTick;
        private volatile long endTick;
        @Nullable
        private volatile String url;

        private LoopSparkRecord(String scenarioId, String sessionId, String profileId, @Nullable Path logFile, long startTick, long endTick) {
            this.scenarioId = scenarioId;
            this.sessionId = sessionId;
            this.profileId = profileId;
            this.logFile = logFile;
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    private static final class ScenarioStats {
        private final LoopSparkRecord record;
        private String winner = "unknown";
        private long durationTicks = -1L;
        private int tickSamples;
        private int tickWarns;
        private int tickSevere;
        private double tickMsSum;
        private double maxTickMs;
        private int maxCoverPathChecksThisTick;
        private int coverSelected;
        private int coverReached;
        private int coverAborted;

        private ScenarioStats(LoopSparkRecord record) {
            this.record = record;
        }

        private static ScenarioStats from(LoopSparkRecord record) {
            ScenarioStats stats = new ScenarioStats(record);
            if (record.logFile == null || !Files.isRegularFile(record.logFile)) {
                return stats;
            }
            try {
                List<String> lines = Files.readAllLines(record.logFile, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line == null || line.isBlank() || line.charAt(0) != '{') {
                        continue;
                    }
                    JsonObject json;
                    try {
                        json = GSON.fromJson(line, JsonObject.class);
                    } catch (Exception ignored) {
                        continue;
                    }
                    if (json == null || !json.has("type")) {
                        continue;
                    }
                    String type = stringValue(json, "type", "");
                    if ("TICK_SAMPLE".equals(type) || "TICK_WARN".equals(type)) {
                        double tickMs = doubleValue(json, "tickMs", 0.0D);
                        stats.tickSamples++;
                        stats.tickMsSum += tickMs;
                        if (tickMs > stats.maxTickMs) {
                            stats.maxTickMs = tickMs;
                        }
                        if ("TICK_WARN".equals(type)) {
                            stats.tickWarns++;
                        }
                        String severity = stringValue(json, "severity", "");
                        if ("severe".equalsIgnoreCase(severity) || tickMs >= 100.0D) {
                            stats.tickSevere++;
                        }
                        int pathChecks = intValue(json, "coverPathChecksThisTick", 0);
                        if (pathChecks > stats.maxCoverPathChecksThisTick) {
                            stats.maxCoverPathChecksThisTick = pathChecks;
                        }
                    } else if ("SESSION_SUMMARY".equals(type)) {
                        stats.winner = stringValue(json, "winner", stats.winner);
                        stats.durationTicks = longValue(json, "durationElapsedTicks", stats.durationTicks);
                    } else if ("COVER_SELECTED".equals(type)) {
                        stats.coverSelected++;
                    } else if ("COVER_REACHED".equals(type)) {
                        stats.coverReached++;
                    } else if ("COVER_ABORTED".equals(type)) {
                        stats.coverAborted++;
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to analyze SEM scenario log {} for loop_all performance report", record.logFile, e);
            }
            return stats;
        }

        private double avgTickMs() {
            return this.tickSamples > 0 ? this.tickMsSum / (double) this.tickSamples : 0.0D;
        }
    }

    private static String stringValue(JsonObject json, String key, String fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int intValue(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long longValue(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double doubleValue(JsonObject json, String key, double fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String formatMs(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatSparkLink(@Nullable String url) {
        if (url == null || url.isBlank()) {
            return "NO_URL";
        }
        return "[Spark](" + url + ")";
    }

    private static String escapeTable(String value) {
        return escapeMarkdownText(value).replace("|", "\\|");
    }

    private static String escapeMarkdownInline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "'");
    }

    private static String escapeMarkdownText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", " ").replace("\r", " ");
    }
}
