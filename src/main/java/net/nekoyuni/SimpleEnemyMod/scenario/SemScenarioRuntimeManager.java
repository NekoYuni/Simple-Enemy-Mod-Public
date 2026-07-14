package net.nekoyuni.SimpleEnemyMod.scenario;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.LadderRouteNavigator;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runtime owner for one active SEM scenario session.
 *
 * <p>This is a conservative migration of the public scenario runner into the current private architecture. It keeps the
 * public JSONL log shape and built-in scenario IDs, but does not import public-only systems such as bandits, guard-site
 * generation, loot, scenario loadouts, reinforcement spawners, suppression managers or role profiles.</p>
 */
public final class SemScenarioRuntimeManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String SESSION_TAG = "SemScenarioSessionId";
    private static final int DEFAULT_SNAPSHOT_INTERVAL_TICKS = 20;
    private static final double SCENARIO_CLEANUP_RADIUS = 128.0D;

    private static final SemScenarioRuntimeManager INSTANCE = new SemScenarioRuntimeManager();

    @Nullable
    private SemScenarioDefinition activeScenario;
    @Nullable
    private String sessionId;
    @Nullable
    private ServerLevel level;
    @Nullable
    private SemScenarioLogger logger;
    @Nullable
    private BlockPos center;

    private long startTick;
    private int durationTicks;
    private int snapshotIntervalTicks = DEFAULT_SNAPSHOT_INTERVAL_TICKS;
    private int lastSnapshotTick;
    private final Set<UUID> spawnedEntities = new LinkedHashSet<>();
    private final Set<UUID> deadEntitiesLogged = new HashSet<>();
    private final Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
    private final Map<String, List<Mob>> scenarioGroups = new LinkedHashMap<>();
    private final Map<UUID, String> scenarioEntityLabels = new HashMap<>();
    private final Map<UUID, String> lastSnapshotState = new HashMap<>();
    private final Map<UUID, UUID> lastSnapshotTarget = new HashMap<>();
    private boolean engagementBootstrapped;
    private boolean loopMode;
    /**
     * True for /sem test loop_all: run every registered scenario exactly once, then stop.
     * /sem test loop <scenario> keeps the old repeating behavior.
     */
    private boolean loopAllOnce;
    private ScenarioState state = ScenarioState.IDLE;
    @Nullable
    private SemScenarioDefinition loopSingleScenario;
    private int loopScenarioDurationTicks = 1200;
    private int loopNextIndex;
    @Nullable
    private String loopRunId;
    @Nullable
    private CommandSourceStack loopCommandSource;
    private List<SemScenarioDefinition> loopScenarios = Collections.emptyList();
    private String loopDirectoryName = "_loop_all";
    private String loopDisplayName = "loop_all";
    private final Set<String> scenarioTriggeredEvents = new HashSet<>();

    private SemScenarioRuntimeManager() {
    }

    public static SemScenarioRuntimeManager get() {
        return INSTANCE;
    }

    public boolean isRunning() {
        return this.activeScenario != null;
    }

    public ScenarioState getState() {
        return this.state;
    }

    @Nullable
    public SemScenarioDefinition getActiveScenario() {
        return this.activeScenario;
    }

    public int getDurationTicks() {
        return this.durationTicks;
    }

    public long getStartTick() {
        return this.startTick;
    }

    public boolean isLoopMode() {
        return this.loopMode;
    }

    @Nullable
    public String getSessionId() {
        return this.sessionId;
    }

    @Nullable
    public Path getCurrentLogFile() {
        return this.logger != null ? this.logger.getLogFile() : null;
    }

    public synchronized boolean start(ServerLevel level, BlockPos center, SemScenarioDefinition scenario, @Nullable Integer customDurationTicks) {
        this.loopMode = false;
        this.loopAllOnce = false;
        this.loopSingleScenario = null;
        this.loopRunId = null;
        this.loopCommandSource = null;
        this.loopScenarios = Collections.emptyList();
        this.loopDirectoryName = "_loop_all";
        this.loopDisplayName = "loop_all";
        return startInternal(level, center, scenario, customDurationTicks != null ? customDurationTicks : scenario.defaultDurationTicks());
    }

    public synchronized boolean startLoop(ServerLevel level, BlockPos center, int durationTicksPerScenario, @Nullable SemScenarioDefinition singleScenario, @Nullable CommandSourceStack commandSource) {
        if (singleScenario == null) {
            return startSuiteInternal(level, center, durationTicksPerScenario, List.of(SemScenarioDefinition.values()), "loop_all", "_loop_all", commandSource, true);
        }

        if (this.isRunning()) {
            return false;
        }
        this.loopMode = true;
        this.loopAllOnce = false;
        this.loopSingleScenario = singleScenario;
        this.loopScenarios = List.of(singleScenario);
        this.loopScenarioDurationTicks = Math.max(20, durationTicksPerScenario);
        this.loopNextIndex = 0;
        this.loopCommandSource = commandSource;
        this.loopDirectoryName = singleScenario.id();
        this.loopDisplayName = singleScenario.id();
        this.loopRunId = null;
        return startNextLoopScenario(level, center.immutable());
    }

    public synchronized boolean startSuite(ServerLevel level, BlockPos center, int durationTicksPerScenario, SemTestSuite suite, @Nullable CommandSourceStack commandSource) {
        if (suite == null) {
            return false;
        }
        return startSuiteInternal(level, center, durationTicksPerScenario, suite.scenarios(), suite.id(), suite.directoryName(), commandSource, true);
    }

    private synchronized boolean startSuiteInternal(ServerLevel level, BlockPos center, int durationTicksPerScenario, List<SemScenarioDefinition> scenarios, String displayName, String directoryName, @Nullable CommandSourceStack commandSource, boolean oneShot) {
        if (this.isRunning() || scenarios == null || scenarios.isEmpty()) {
            return false;
        }
        this.loopMode = true;
        this.loopAllOnce = oneShot;
        this.loopSingleScenario = null;
        this.loopScenarios = List.copyOf(scenarios);
        this.loopScenarioDurationTicks = Math.max(20, durationTicksPerScenario);
        this.loopNextIndex = 0;
        this.loopCommandSource = commandSource;
        this.loopDisplayName = displayName != null && !displayName.isBlank() ? displayName : "loop_all";
        this.loopDirectoryName = directoryName != null && !directoryName.isBlank() ? directoryName : "_" + this.loopDisplayName;
        this.loopRunId = this.loopDisplayName + "_" + Long.toHexString(System.currentTimeMillis()) + "_" + Integer.toHexString(level.random.nextInt());
        SparkProfilerBridge.startLoopAllRun(level.getServer(), resolveMinecraftRoot(), this.loopRunId, this.loopDirectoryName, this.loopDisplayName);
        return startNextLoopScenario(level, center.immutable());
    }

    public synchronized void stopLoop(String reason) {
        String finishedLoopRunId = this.loopRunId;
        boolean finishLoopSummary = this.loopAllOnce && finishedLoopRunId != null;
        this.loopSingleScenario = null;
        this.loopAllOnce = false;
        this.loopScenarios = Collections.emptyList();
        this.loopDirectoryName = "_loop_all";
        this.loopDisplayName = "loop_all";
        if (this.isRunning()) {
            finishActiveScenario(reason);
        }
        this.loopMode = false;
        this.loopRunId = null;
        this.loopCommandSource = null;
        this.loopScenarios = Collections.emptyList();
        this.loopDirectoryName = "_loop_all";
        this.loopDisplayName = "loop_all";
        if (finishLoopSummary) {
            SparkProfilerBridge.finishLoopAllRun(finishedLoopRunId, reason);
        }
    }

    private boolean startInternal(ServerLevel level, BlockPos center, SemScenarioDefinition scenario, int durationTicks) {
        if (this.isRunning()) {
            return false;
        }

        // Match the public scenario harness isolation model: each scenario starts from a clean
        // non-player entity set, not merely from a local SEM-unit cleanup. This prevents old mobs
        // from stealing targets and prevents manually spawned SEM units from appearing as EXTERNAL.
        clearNonPlayerEntities(level);

        this.state = ScenarioState.STARTING;
        this.activeScenario = scenario;
        this.level = level;
        this.center = center.immutable();
        this.sessionId = scenario.id() + "_" + Long.toHexString(System.currentTimeMillis()) + "_" + Integer.toHexString(level.random.nextInt());
        this.startTick = level.getGameTime();
        this.durationTicks = durationTicks;
        this.lastSnapshotTick = -9999;
        this.spawnedEntities.clear();
        this.deadEntitiesLogged.clear();
        this.originalBlocks.clear();
        this.scenarioGroups.clear();
        this.scenarioEntityLabels.clear();
        this.lastSnapshotState.clear();
        this.lastSnapshotTarget.clear();
        this.engagementBootstrapped = false;
        this.scenarioTriggeredEvents.clear();

        LadderRouteNavigator.resetSessionCounters();

        try {
            this.logger = new SemScenarioLogger(resolveMinecraftRoot(), scenario.id(), this.sessionId);
        } catch (IOException e) {
            LOGGER.error("Failed to create SEM scenario log file", e);
            resetActiveStateOnly();
            return false;
        }

        this.logger.sessionStart(level.getGameTime(), this.center, this.durationTicks, this.loopMode);
        buildScenario(level, this.center, scenario);
        if (scenario == SemScenarioDefinition.SOUND_AWARENESS_IDLE) {
            bootstrapIdleSoundScenario(level);
        } else {
            bootstrapEngagement(level);
        }

        if (this.loopMode) {
            SparkProfilerBridge.startLoopScenarioProfile(level, scenario, this.sessionId, this.logger.getLogFile(), level.getGameTime(), this.loopCommandSource);
        }

        this.state = ScenarioState.RUNNING;
        return true;
    }

    private boolean startNextLoopScenario(ServerLevel level, BlockPos center) {
        SemScenarioDefinition next;
        if (this.loopSingleScenario != null) {
            next = this.loopSingleScenario;
        } else {
            List<SemScenarioDefinition> scenarios = this.loopScenarios != null && !this.loopScenarios.isEmpty()
                    ? this.loopScenarios
                    : List.of(SemScenarioDefinition.values());
            if (scenarios.isEmpty()) {
                this.loopMode = false;
                this.loopAllOnce = false;
                this.loopNextIndex = 0;
                return false;
            }
            if (this.loopAllOnce && this.loopNextIndex >= scenarios.size()) {
                this.loopMode = false;
                this.loopAllOnce = false;
                this.loopNextIndex = 0;
                return false;
            }
            next = scenarios.get(this.loopAllOnce ? this.loopNextIndex : this.loopNextIndex % scenarios.size());
            this.loopNextIndex++;
        }
        return startInternal(level, center, next, this.loopScenarioDurationTicks);
    }

    public synchronized void stop(String reason) {
        String finishedLoopRunId = this.loopRunId;
        boolean finishLoopSummary = this.loopAllOnce && finishedLoopRunId != null;
        if (!this.isRunning()) {
            this.loopMode = false;
            this.loopAllOnce = false;
            this.loopRunId = null;
            this.loopCommandSource = null;
            this.loopScenarios = Collections.emptyList();
            this.loopDirectoryName = "_loop_all";
            this.loopDisplayName = "loop_all";
            if (finishLoopSummary) {
                SparkProfilerBridge.finishLoopAllRun(finishedLoopRunId, reason);
            }
            return;
        }
        finishActiveScenario(reason);
        this.loopMode = false;
        this.loopAllOnce = false;
        this.loopRunId = null;
        this.loopCommandSource = null;
        this.loopScenarios = Collections.emptyList();
        this.loopDirectoryName = "_loop_all";
        this.loopDisplayName = "loop_all";
        if (finishLoopSummary) {
            SparkProfilerBridge.finishLoopAllRun(finishedLoopRunId, reason);
        }
    }

    private void finishActiveScenario(String reason) {
        this.state = ScenarioState.STOPPING;
        ServerLevel activeLevel = this.level;
        SemScenarioLogger activeLogger = this.logger;
        String finishedSessionId = this.sessionId;
        boolean stopSparkProfile = this.loopMode;
        if (stopSparkProfile && activeLevel != null) {
            SparkProfilerBridge.stopLoopScenarioProfile(activeLevel, finishedSessionId, activeLevel.getGameTime());
        }
        if (activeLevel != null && activeLogger != null) {
            activeLogger.summary(activeLevel.getGameTime(), buildSummary(activeLevel));
            activeLogger.message("SESSION_END", activeLevel.getGameTime(), reason);
        }
        cleanup(false);
        closeLogger();
        SparkProfilerBridge.markScenarioLogClosed(finishedSessionId);
        resetActiveStateOnly();
    }

    public synchronized int cleanup(boolean standalone) {
        this.state = ScenarioState.CLEANING;
        int removed = 0;
        ServerLevel activeLevel = this.level;
        if (activeLevel == null) {
            return 0;
        }

        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = activeLevel.getEntity(uuid);
            if (entity != null) {
                entity.discard();
                removed++;
            }
        }
        this.spawnedEntities.clear();
        this.deadEntitiesLogged.clear();

        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(this.originalBlocks.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<BlockPos, BlockState> entry : entries) {
            activeLevel.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        int restoredBlocks = this.originalBlocks.size();
        this.originalBlocks.clear();
        this.scenarioGroups.clear();
        this.scenarioEntityLabels.clear();
        this.lastSnapshotState.clear();
        this.lastSnapshotTarget.clear();
        this.engagementBootstrapped = false;

        if (this.logger != null) {
            this.logger.message("CLEANUP", activeLevel.getGameTime(), "removedEntities=" + removed + " restoredBlocks=" + restoredBlocks + " standalone=" + standalone);
        }
        return removed;
    }

    public synchronized void logTrackedEntityEvent(String type, Entity entity, String message) {
        if (!this.isRunning() || this.logger == null || entity == null) {
            return;
        }
        if (!this.spawnedEntities.contains(entity.getUUID()) && !entity.getPersistentData().contains(SESSION_TAG)) {
            return;
        }
        this.logger.entityMessage(type, entity.level().getGameTime(), entity, message != null ? message : "");
    }

    public synchronized boolean isTrackedScenarioEntity(Entity entity) {
        if (!this.isRunning() || entity == null) {
            return false;
        }
        return this.spawnedEntities.contains(entity.getUUID()) || entity.getPersistentData().contains(SESSION_TAG);
    }

    public synchronized boolean trackExternalScenarioEntity(Entity entity, String label) {
        if (!this.isRunning() || this.level == null || this.logger == null || entity == null || entity instanceof Player) {
            return false;
        }
        if (this.spawnedEntities.contains(entity.getUUID()) || entity.getPersistentData().contains(SESSION_TAG)) {
            return false;
        }
        if (this.center != null && entity.blockPosition().distSqr(this.center) > SCENARIO_CLEANUP_RADIUS * SCENARIO_CLEANUP_RADIUS) {
            return false;
        }
        String safeLabel = label == null || label.isBlank() ? "EXTERNAL" : label;
        entity.getPersistentData().putString(SESSION_TAG, this.sessionId != null ? this.sessionId : "unknown");
        entity.getPersistentData().putString("SemScenarioLabel", safeLabel);
        this.spawnedEntities.add(entity.getUUID());
        this.scenarioEntityLabels.put(entity.getUUID(), safeLabel);
        if (entity instanceof Mob mob) {
            this.scenarioGroups.computeIfAbsent(safeLabel, ignored -> new ArrayList<>()).add(mob);
        }
        this.logger.entity("EXTERNAL_ENTITY_TRACKED", entity.level().getGameTime(), entity);
        return true;
    }

    private Map<String, Object> buildSummary(ServerLevel activeLevel) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trackedEntities", this.spawnedEntities.size());
        summary.put("deadEntities", this.deadEntitiesLogged.size());
        int alive = 0;
        Map<String, Integer> aliveByLabel = new LinkedHashMap<>();
        Map<String, Integer> deadByLabel = new LinkedHashMap<>();
        int aliveFriendly = 0;
        int aliveHostile = 0;
        int deadFriendly = 0;
        int deadHostile = 0;

        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = activeLevel.getEntity(uuid);
            String label = entity != null ? entity.getPersistentData().getString("SemScenarioLabel") : this.scenarioEntityLabels.getOrDefault(uuid, "unknown");
            if (label == null || label.isBlank()) {
                label = "unknown";
            }
            String normalized = sanitizeSummaryKey(label);
            boolean friendly = label.startsWith("US") || label.startsWith("PMC");
            boolean hostile = label.startsWith("RU") || label.startsWith("HOSTILE");
            if (entity instanceof LivingEntity living && living.isAlive()) {
                alive++;
                aliveByLabel.merge(normalized, 1, Integer::sum);
                if (friendly) aliveFriendly++;
                if (hostile) aliveHostile++;
            } else {
                deadByLabel.merge(normalized, 1, Integer::sum);
                if (friendly) deadFriendly++;
                if (hostile) deadHostile++;
            }
        }

        summary.put("aliveEntities", alive);
        summary.put("aliveFriendly", aliveFriendly);
        summary.put("aliveHostile", aliveHostile);
        summary.put("deadFriendly", deadFriendly);
        summary.put("deadHostile", deadHostile);
        summary.put("durationElapsedTicks", this.level != null ? (int) Math.max(0L, activeLevel.getGameTime() - this.startTick) : 0);
        summary.put("winner", aliveFriendly > 0 && aliveHostile == 0 ? "FRIENDLY" : aliveHostile > 0 && aliveFriendly == 0 ? "HOSTILE" : "UNDECIDED");
        aliveByLabel.forEach((label, count) -> summary.put("alive_" + label, count));
        deadByLabel.forEach((label, count) -> summary.put("dead_" + label, count));
        LadderRouteNavigator.appendSessionCounters(summary);
        return summary;
    }

    private static String sanitizeSummaryKey(String label) {
        if (label == null || label.isBlank()) {
            return "unknown";
        }
        return label.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public void tick(ServerLevel tickingLevel) {
        if (!this.isRunning() || this.level != tickingLevel || this.logger == null) {
            return;
        }

        long now = tickingLevel.getGameTime();
        if (now - this.startTick >= this.durationTicks) {
            finishAndMaybeContinueLoop(tickingLevel, this.loopMode ? "loop_timeout" : "timeout");
            return;
        }

        sanitizeScenarioTargets(tickingLevel, now);
        runScriptedScenarioEvents(tickingLevel, now);

        String earlyEndReason = getEarlyEndReason(tickingLevel, now);
        if (earlyEndReason != null) {
            finishAndMaybeContinueLoop(tickingLevel, earlyEndReason);
            return;
        }

        if (now - this.lastSnapshotTick >= this.snapshotIntervalTicks) {
            this.lastSnapshotTick = (int) now;
            logSnapshots(tickingLevel, now);
        }
    }

    public void logTickProfile(SemTickProfiler.TickMetrics metrics) {
        if (!TacticalAiConfig.enableSemTickProfiler() || !this.isRunning() || this.level == null || this.logger == null || metrics == null) {
            return;
        }

        long now = this.level.getGameTime();
        long scenarioTick = Math.max(0L, now - this.startTick);
        boolean slowTick = metrics.tickMs >= TacticalAiConfig.semTickWarnThresholdMs();
        int sampleInterval = Math.max(1, TacticalAiConfig.semTickSampleIntervalTicks());
        boolean sampleTick = TacticalAiConfig.enableSemTickSamples() && scenarioTick % sampleInterval == 0L;

        if (!slowTick && !sampleTick) {
            return;
        }

        int tracked = this.spawnedEntities.size();
        int alive = 0;
        int aliveFriendly = 0;
        int aliveHostile = 0;
        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = this.level.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                continue;
            }
            alive++;
            String label = entity.getPersistentData().getString("SemScenarioLabel");
            if (label.startsWith("US") || label.startsWith("PMC")) {
                aliveFriendly++;
            } else if (label.startsWith("RU") || label.startsWith("HOSTILE")) {
                aliveHostile++;
            }
        }

        this.logger.tickProfile(slowTick ? "TICK_WARN" : "TICK_SAMPLE", now, scenarioTick, metrics, tracked, alive, aliveFriendly, aliveHostile);
    }

    private void finishAndMaybeContinueLoop(ServerLevel tickingLevel, String reason) {
        boolean continueLoop = this.loopMode;
        boolean wasLoopAllOnce = this.loopAllOnce;
        String finishedLoopRunId = this.loopRunId;
        BlockPos loopCenter = this.center;
        finishActiveScenario(reason);
        if (continueLoop && loopCenter != null) {
            boolean startedNext = startNextLoopScenario(tickingLevel, loopCenter);
            if (!startedNext) {
                LOGGER.info("SEM scenario suite completed: {} has run all scheduled scenarios once.", this.loopDisplayName);
                if (wasLoopAllOnce && finishedLoopRunId != null) {
                    SparkProfilerBridge.finishLoopAllRun(finishedLoopRunId, "loop_all_completed");
                    this.loopRunId = null;
                }
            }
        }
    }

    @Nullable
    private String getEarlyEndReason(ServerLevel activeLevel, long now) {
        if (now - this.startTick < 40L || !this.engagementBootstrapped) {
            return null;
        }

        int aliveFriendly = 0;
        int aliveHostile = 0;
        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = activeLevel.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                continue;
            }
            String label = entity.getPersistentData().getString("SemScenarioLabel");
            if (label.startsWith("US") || label.startsWith("PMC")) {
                aliveFriendly++;
            } else if (label.startsWith("RU") || label.startsWith("HOSTILE")) {
                aliveHostile++;
            }
        }

        if (aliveFriendly == 0 && aliveHostile == 0) {
            logEarlyScenarioEnd(activeLevel, now, "NO_ACTIVE_COMBATANTS", aliveFriendly, aliveHostile);
            return this.loopMode ? "loop_no_active_combatants" : "no_active_combatants";
        }
        if (aliveFriendly > 0 && aliveHostile == 0) {
            logTeamEliminated(activeLevel, now, "HOSTILE", aliveFriendly, aliveHostile);
            return this.loopMode ? "loop_side_eliminated:HOSTILE" : "side_eliminated:HOSTILE";
        }
        if (aliveHostile > 0 && aliveFriendly == 0) {
            logTeamEliminated(activeLevel, now, "FRIENDLY", aliveFriendly, aliveHostile);
            return this.loopMode ? "loop_side_eliminated:FRIENDLY" : "side_eliminated:FRIENDLY";
        }
        return null;
    }


    private void runScriptedScenarioEvents(ServerLevel activeLevel, long now) {
        if (this.activeScenario == null || this.center == null) {
            return;
        }
        long scenarioTick = Math.max(0L, now - this.startTick);
        if (this.activeScenario == SemScenarioDefinition.CLOSE_THREAT_SWITCH_TEST
                && scenarioTick >= 50L
                && this.scenarioTriggeredEvents.add("close_threat_spawned")) {
            spawnMany(activeLevel, ModEntities.RUUNIT.get(), this.center.offset(-2, 1, 2), 1, "RU_CLOSE", "SMG", "LIGHT");
            if (this.logger != null) {
                this.logger.message("SCRIPTED_EVENT", now, "event=close_threat_spawned expected=target_switch_to_near_threat");
            }
        }
    }

    private void logTeamEliminated(ServerLevel activeLevel, long now, String eliminatedTeam, int aliveFriendly, int aliveHostile) {
        if (this.logger == null) {
            return;
        }
        String message = "team=" + eliminatedTeam + " aliveFriendly=" + aliveFriendly + " aliveHostile=" + aliveHostile;
        this.logger.message("TEAM_ELIMINATED", now, message);
        this.logger.message("SCENARIO_EARLY_END", now, "reason=TEAM_ELIMINATED " + message);
    }

    private void logEarlyScenarioEnd(ServerLevel activeLevel, long now, String reason, int aliveFriendly, int aliveHostile) {
        if (this.logger == null) {
            return;
        }
        this.logger.message("SCENARIO_EARLY_END", now, "reason=" + reason + " aliveFriendly=" + aliveFriendly + " aliveHostile=" + aliveHostile);
    }

    public void onEntityDeath(LivingEntity entity) {
        if (!this.isRunning() || this.logger == null) {
            return;
        }
        if (this.spawnedEntities.contains(entity.getUUID()) && this.deadEntitiesLogged.add(entity.getUUID())) {
            this.logger.entity("ENTITY_DEATH", entity.level().getGameTime(), entity);
        }
    }

    private void logSnapshots(ServerLevel activeLevel, long now) {
        int alive = 0;
        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = activeLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                if (living.isAlive()) {
                    alive++;
                    this.logger.entitySnapshot(now, living);
                    logSnapshotTransitions(now, living);
                } else if (this.deadEntitiesLogged.add(uuid)) {
                    this.logger.entity("ENTITY_DEATH", now, living);
                }
            }
        }
        this.logger.message("SESSION_TICK", now, "aliveEntities=" + alive + " trackedEntities=" + this.spawnedEntities.size());
    }

    private void logSnapshotTransitions(long now, LivingEntity living) {
        if (this.logger == null) {
            return;
        }
        UUID uuid = living.getUUID();
        String state = "none";
        if (living instanceof AbstractUnit unit) {
            state = unit.getSoldierState().name();
        }
        String previousState = this.lastSnapshotState.put(uuid, state);
        if (previousState != null && !previousState.equals(state)) {
            this.logger.entityStateChange(now, living, previousState, state);
        }

        UUID currentTarget = null;
        if (living instanceof Mob mob && mob.getTarget() != null) {
            currentTarget = mob.getTarget().getUUID();
        }
        UUID previousTarget = this.lastSnapshotTarget.put(uuid, currentTarget);
        if (previousTarget != null && currentTarget == null) {
            this.logger.entity("TARGET_LOST", now, living);
        }
    }

    private void sanitizeScenarioTargets(ServerLevel activeLevel, long now) {
        for (UUID uuid : new ArrayList<>(this.spawnedEntities)) {
            Entity entity = activeLevel.getEntity(uuid);
            if (!(entity instanceof Mob attacker) || !attacker.isAlive() || attacker.isRemoved()) {
                continue;
            }

            LivingEntity currentTarget = attacker.getTarget();
            if (this.activeScenario == SemScenarioDefinition.SOUND_AWARENESS_IDLE
                    && currentTarget == null
                    && labelOf(attacker).startsWith("US_IDLE")) {
                continue;
            }
            if (isValidScenarioTargetFor(attacker, currentTarget)) {
                continue;
            }

            LivingEntity replacement = nearestLiving(attacker, opposingTargetsFor(attacker));
            if (replacement != null) {
                reassignScenarioTarget(activeLevel, now, attacker, replacement);
            } else if (currentTarget != null) {
                attacker.setTarget(null);
            }
        }
    }

    private boolean isValidScenarioTargetFor(Mob attacker, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (!this.spawnedEntities.contains(target.getUUID())) {
            return false;
        }

        String attackerLabel = labelOf(attacker);
        String targetLabel = labelOf(target);
        return (isFriendlyLabel(attackerLabel) && isHostileLabel(targetLabel))
                || (isHostileLabel(attackerLabel) && isFriendlyLabel(targetLabel));
    }

    private List<Mob> opposingTargetsFor(Mob attacker) {
        String label = labelOf(attacker);
        if (isFriendlyLabel(label)) {
            return collectGroups("RU", "HOSTILE");
        }
        if (isHostileLabel(label)) {
            return collectGroups("US", "PMC");
        }
        return Collections.emptyList();
    }

    private void reassignScenarioTarget(ServerLevel level, long now, Mob attacker, LivingEntity target) {
        if (attacker.getTarget() == target) {
            return;
        }
        attacker.setTarget(target);
        if (attacker instanceof AbstractUnit unit) {
            unit.setLastKnownTargetPos(target.position());
            unit.setLastSeenTargetTick(level.getGameTime());
            unit.setCoverSearchOrigin(attacker.blockPosition());
        }
        if (this.logger != null) {
            this.logger.entityPair("TARGET_ASSIGNED", now, attacker, target);
        }
    }

    private String labelOf(Entity entity) {
        String label = entity.getPersistentData().getString("SemScenarioLabel");
        if ((label == null || label.isBlank()) && this.scenarioEntityLabels.containsKey(entity.getUUID())) {
            label = this.scenarioEntityLabels.get(entity.getUUID());
        }
        return label != null ? label : "";
    }

    private static boolean isFriendlyLabel(String label) {
        return label != null && (label.startsWith("US") || label.startsWith("PMC"));
    }

    private static boolean isHostileLabel(String label) {
        return label != null && (label.startsWith("RU") || label.startsWith("HOSTILE"));
    }

    private void buildScenario(ServerLevel level, BlockPos center, SemScenarioDefinition scenario) {
        switch (scenario) {
            case LADDER_TEST -> buildLadderTest(level, center);
            case NAVIGATION_POLISH_TEST -> buildNavigationPolishTest(level, center);
            case GLASS_FOLIAGE_LOS_TEST -> buildGlassFoliageLosTest(level, center);
            case SOFT_OBSTACLE_BREAK_TEST -> buildSoftObstacleBreakTest(level, center);
            case HAZARD_AVOIDANCE_TEST -> buildHazardAvoidanceTest(level, center);
            case ROOFTOP_5V5 -> buildRooftop5v5(level, center);
            case URBAN_10V10 -> buildUrban10v10(level, center);
            case RETREAT_TEST -> buildRetreatTest(level, center);
            case COVER_CONFIDENCE_TEST -> buildCoverConfidenceTest(level, center);
            case KILLZONE_AWARENESS_TEST -> buildKillzoneAwarenessTest(level, center);
            case SUPPRESSION_TEST -> buildSuppressionTest(level, center);
            case SOUND_AWARENESS_IDLE -> buildSoundAwarenessIdle(level, center);
            case LONG_RANGE_SUPPRESSION_TEST -> buildLongRangeSuppressionTest(level, center);
            case TARGET_DISTANCE_PRIORITY_TEST -> buildTargetDistancePriorityTest(level, center);
            case CLOSE_THREAT_SWITCH_TEST -> buildCloseThreatSwitchTest(level, center);
            case SQUAD_MEMORY_TEST -> buildSquadMemoryTest(level, center);
            case SQUAD_MEMORY_LOS_LOST_TEST -> buildSquadMemoryLosLostTest(level, center);
            case AMBUSH_TEST -> buildAmbushTest(level, center);
            case ROLE_PROFILE_TEST -> buildRoleProfileTest(level, center);
            case TACTICAL_SPEED_TEST -> buildTacticalSpeedTest(level, center);
            case INDOOR_CQB -> buildIndoorCqb(level, center);
            case DOORWAY_CQB_TEST -> buildDoorwayCqbTest(level, center);
            case LONG_RANGE_5V5 -> buildLongRange5v5(level, center);
            case LONG_RANGE_10V10 -> buildLongRange10v10(level, center);
        }
    }

    private void buildLadderTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 18, 14);
        platform(level, c.offset(-5, 0, -5), c.offset(5, 0, 5), Blocks.STONE.defaultBlockState());
        platform(level, c.offset(-5, 8, -5), c.offset(5, 8, 5), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= 9; y++) {
            setBlock(level, c.offset(-1, y, 0), Blocks.STONE.defaultBlockState());
            setBlock(level, c.offset(0, y, 0), Blocks.STONE.defaultBlockState());
            setBlock(level, c.offset(1, y, 0), Blocks.STONE.defaultBlockState());
        }
        for (int y = 1; y <= 8; y++) {
            setBlock(level, c.offset(0, y, -1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        for (int y = 1; y <= 11; y++) {
            setBlock(level, c.offset(0, y, -2), Blocks.AIR.defaultBlockState());
            setBlock(level, c.offset(0, y, -3), Blocks.AIR.defaultBlockState());
        }
        setBlock(level, c.offset(0, 8, -1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        setBlock(level, c.offset(0, 9, -1), Blocks.AIR.defaultBlockState());
        setBlock(level, c.offset(0, 10, -1), Blocks.AIR.defaultBlockState());
        setBlock(level, c.offset(0, 8, -2), Blocks.STONE.defaultBlockState());
        setBlock(level, c.offset(0, 9, -2), Blocks.AIR.defaultBlockState());
        setBlock(level, c.offset(0, 10, -2), Blocks.AIR.defaultBlockState());
        for (int z = -4; z <= -2; z++) {
            setBlock(level, c.offset(0, 1, z), Blocks.AIR.defaultBlockState());
            setBlock(level, c.offset(0, 2, z), Blocks.AIR.defaultBlockState());
        }
        scenarioNote(level, "Ladder arena built from public scenario; defenders are US, attackers are RU in the private generic-unit architecture.");
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-3, 9, -3), 3, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(3, 1, 3), 3, "RU", "SMG", "HEAVY");
    }

    private void buildRooftop5v5(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 28, 12);
        platform(level, c.offset(-16, 7, -5), c.offset(-6, 7, 5), Blocks.STONE.defaultBlockState());
        platform(level, c.offset(6, 7, -5), c.offset(16, 7, 5), Blocks.STONE.defaultBlockState());
        coverLine(level, c.offset(-11, 8, -4), 4, true);
        coverLine(level, c.offset(11, 8, 1), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-12, 8, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(12, 8, 0), 5, "RU", "RIFLE", "HEAVY");
    }

    private void buildUrban10v10(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 34, 8);
        for (int x = -18; x <= 18; x += 6) {
            wall(level, c.offset(x, 1, -10), c.offset(x, 3, -4), Blocks.STONE.defaultBlockState());
            wall(level, c.offset(x, 1, 4), c.offset(x, 3, 10), Blocks.STONE.defaultBlockState());
        }
        for (int z = -12; z <= 12; z += 6) {
            coverLine(level, c.offset(-3, 1, z), 3, false);
            coverLine(level, c.offset(3, 1, z + 2), 3, false);
        }
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-22, 1, 0), 10, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(22, 1, 0), 10, "RU", "SMG", "HEAVY");
    }

    private void buildRetreatTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 26, 6);
        coverLine(level, c.offset(-8, 1, -4), 5, true);
        coverLine(level, c.offset(6, 1, 4), 5, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-14, 1, 0), 4, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(13, 1, 0), 8, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Retreat test currently validates Phase 2 cover/fallback behavior rather than public suppression retreat.");
    }

    private void buildIndoorCqb(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 18, 5);
        wall(level, c.offset(-4, 1, -6), c.offset(-4, 3, 6), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(4, 1, -6), c.offset(4, 3, 6), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(-4, 1, -6), c.offset(4, 3, -6), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(-4, 1, 6), c.offset(4, 3, 6), Blocks.STONE.defaultBlockState());
        setDoorway(level, c.offset(-4, 1, 0));
        setDoorway(level, c.offset(4, 1, 0));
        coverLine(level, c.offset(-2, 1, -2), 3, true);
        coverLine(level, c.offset(0, 1, 3), 3, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-12, 1, 0), 5, "US", "SMG", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(9, 1, 0), 5, "RU", "SMG", "HEAVY");
    }

    private void buildCoverConfidenceTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 30, 6);
        for (int z = -8; z <= 8; z += 4) {
            coverLine(level, c.offset(-8, 1, z), 4, true);
            coverLine(level, c.offset(6, 1, z + 1), 4, true);
        }
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-18, 1, 0), 6, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(18, 1, 0), 6, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Cover confidence test should produce HOLD_COVER/SEEK_COVER transitions in ENTITY_SNAPSHOT logs.");
    }

    private void buildSuppressionTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 30, 6);
        coverLine(level, c.offset(-12, 1, -4), 6, true);
        coverLine(level, c.offset(10, 1, 4), 6, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-18, 1, 0), 4, "US", "LMG", "HEAVY");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(17, 1, 0), 8, "RU", "PISTOL", "LIGHT");
        scenarioNote(level, "Suppression scenario validates Phase 3 suppression pressure, accuracy penalty and suppression-driven cover requests.");
    }

    private void buildSquadMemoryTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 26, 6);
        wall(level, c.offset(0, 1, -8), c.offset(0, 3, 8), Blocks.STONE.defaultBlockState());
        setDoorway(level, c.offset(0, 1, 0));
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-10, 1, -2), 4, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(10, 1, 2), 4, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Squad memory test validates shared enemy memory and last-known-position reuse between nearby allied units.");
    }

    private void buildAmbushTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 28, 6);
        wall(level, c.offset(-5, 1, -10), c.offset(-5, 3, 10), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(5, 1, -10), c.offset(5, 3, 10), Blocks.STONE.defaultBlockState());
        coverLine(level, c.offset(-10, 1, -5), 4, false);
        coverLine(level, c.offset(8, 1, 3), 4, false);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-16, 1, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(16, 1, 0), 5, "RU", "RIFLE", "HEAVY");
    }

    private void buildRoleProfileTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 30, 6);
        coverLine(level, c.offset(-12, 1, -4), 5, true);
        coverLine(level, c.offset(10, 1, 4), 5, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-18, 1, 0), 5, "US", "MIXED", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(18, 1, 0), 5, "RU", "MIXED", "HEAVY");
        scenarioNote(level, "Role profile test keeps the public scenario ID but uses current generic roles only.");
    }

    private void buildNavigationPolishTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 28, 8);
        for (int x = -8; x <= 8; x += 4) {
            wall(level, c.offset(x, 1, -6), c.offset(x, 3, 6), Blocks.GLASS.defaultBlockState());
            setDoorway(level, c.offset(x, 1, 0));
        }
        coverLine(level, c.offset(-10, 1, -8), 5, true);
        coverLine(level, c.offset(7, 1, 8), 5, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-18, 1, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(18, 1, 0), 5, "RU", "RIFLE", "MEDIUM");
        scenarioNote(level, "Navigation polish test includes glass/doorway clutter for Phase 1 LoS and Phase 2 cover checks.");
    }

    private void buildGlassFoliageLosTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 26, 5);
        wall(level, c.offset(-2, 1, -7), c.offset(-2, 3, 7), Blocks.GLASS.defaultBlockState());
        wall(level, c.offset(2, 1, -7), c.offset(2, 3, 7), Blocks.OAK_LEAVES.defaultBlockState());
        coverLine(level, c.offset(-11, 1, -4), 4, true);
        coverLine(level, c.offset(8, 1, 4), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-14, 1, 0), 4, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(14, 1, 0), 4, "RU", "RIFLE", "MEDIUM");
        scenarioNote(level, "Glass/foliage LoS regression: tactical target acquisition and firing should ignore glass/leaves as visual clutter while hard cover remains blocking.");
    }

    private void buildSoftObstacleBreakTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 24, 5);
        for (int z = -5; z <= 5; z++) {
            setBlock(level, c.offset(-2, 1, z), Blocks.GLASS.defaultBlockState());
            setBlock(level, c.offset(-1, 1, z), Blocks.OAK_LEAVES.defaultBlockState());
            setBlock(level, c.offset(0, 1, z), Blocks.TALL_GRASS.defaultBlockState());
        }
        coverLine(level, c.offset(-9, 1, -4), 4, true);
        coverLine(level, c.offset(7, 1, 4), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-13, 1, 0), 4, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(13, 1, 0), 4, "RU", "SMG", "MEDIUM");
        scenarioNote(level, "Soft-obstacle regression: units should break glass/leaves/grass when these blocks obstruct navigation.");
    }

    private void buildHazardAvoidanceTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 28, 5);
        for (int z = -7; z <= 7; z++) {
            setBlock(level, c.offset(0, 0, z), Blocks.LAVA.defaultBlockState());
            if (z % 3 == 0) {
                setBlock(level, c.offset(3, 0, z), Blocks.SAND.defaultBlockState());
                setBlock(level, c.offset(3, 1, z), Blocks.CACTUS.defaultBlockState());
            }
        }
        coverLine(level, c.offset(-10, 1, -5), 5, true);
        coverLine(level, c.offset(7, 1, 5), 5, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-15, 1, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(15, 1, 0), 5, "RU", "RIFLE", "MEDIUM");
        scenarioNote(level, "Hazard regression: lava/cactus line should be rejected by tactical movement and cover selection.");
    }

    private void buildTargetDistancePriorityTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 30, 5);
        coverLine(level, c.offset(-6, 1, -4), 3, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-6, 1, 0), 1, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(4, 1, -2), 1, "RU", "SMG", "LIGHT");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(22, 1, 4), 1, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Target distance-priority test: the closer visible RU target should be preferred over the far target.");
    }

    private void buildCloseThreatSwitchTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 30, 5);
        coverLine(level, c.offset(-9, 1, -4), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-8, 1, 0), 1, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(18, 1, 0), 1, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Close-threat switch test: a nearby hostile is spawned after the engagement starts and should steal target priority from the far target.");
    }

    private void buildSquadMemoryLosLostTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 32, 6);
        wall(level, c.offset(0, 1, -10), c.offset(0, 3, 10), Blocks.STONE.defaultBlockState());
        setDoorway(level, c.offset(0, 1, -3));
        setDoorway(level, c.offset(0, 1, 5));
        coverLine(level, c.offset(-10, 1, -5), 4, true);
        coverLine(level, c.offset(7, 1, 5), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-14, 1, -2), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(14, 1, 4), 3, "RU", "RIFLE", "HEAVY");
        scenarioNote(level, "Squad memory LOS-lost regression: units should preserve/search shared last-known enemy positions after direct visibility breaks.");
    }

    private void buildDoorwayCqbTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 20, 5);
        wall(level, c.offset(0, 1, -8), c.offset(0, 3, -2), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(0, 1, 2), c.offset(0, 3, 8), Blocks.STONE.defaultBlockState());
        setDoorway(level, c.offset(0, 1, 0));
        coverLine(level, c.offset(-7, 1, -3), 3, true);
        coverLine(level, c.offset(3, 1, 3), 3, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-10, 1, 0), 5, "US", "SMG", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(6, 1, 2), 5, "RU", "SMG", "HEAVY");
        scenarioNote(level, "Doorway CQB test replaces the old public breach-stack scenario with a generic short-range doorway fight.");
    }

    private void buildTacticalSpeedTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 32, 6);
        for (int z = -8; z <= 8; z += 4) {
            coverLine(level, c.offset(-14, 1, z), 4, true);
            coverLine(level, c.offset(10, 1, z), 4, true);
        }
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-20, 1, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(20, 1, 0), 5, "RU", "RIFLE", "MEDIUM");
    }

    private void buildKillzoneAwarenessTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 46, 6);
        coverLine(level, c.offset(-22, 1, -5), 5, true);
        coverLine(level, c.offset(-8, 1, 4), 4, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-26, 1, 0), 5, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(22, 1, 0), 4, "RU", "LMG", "HEAVY");
        scenarioNote(level, "Killzone awareness test validates exposed long-lane pressure, cover selection and Phase 4 killzone response.");
    }

    private void buildSoundAwarenessIdle(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 34, 6);
        wall(level, c.offset(-20, 1, 6), c.offset(-8, 3, 6), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(-20, 1, 12), c.offset(-8, 3, 12), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(-20, 1, 6), c.offset(-20, 3, 12), Blocks.STONE.defaultBlockState());
        wall(level, c.offset(-8, 1, 6), c.offset(-8, 3, 12), Blocks.STONE.defaultBlockState());
        setDoorway(level, c.offset(-14, 1, 6));

        coverLine(level, c.offset(-15, 1, -2), 4, true);
        coverLine(level, c.offset(8, 1, 2), 4, true);

        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(-18, 1, 0), 1, "RU_SHOOTER", "LMG", "HEAVY");
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(16, 1, 0), 1, "US_TARGET", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-16, 1, 9), 4, "US_IDLE", "RIFLE", "MEDIUM");
        scenarioNote(level, "Idle sound-awareness scenario: only the RU shooter receives a forced target; US_IDLE listeners should react via hearing/investigation, not initial forced engagement.");
    }

    private void buildLongRange5v5(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 64, 8);
        coverLine(level, c.offset(-32, 1, -6), 6, true);
        coverLine(level, c.offset(-32, 1, 6), 6, true);
        coverLine(level, c.offset(30, 1, -6), 6, true);
        coverLine(level, c.offset(30, 1, 6), 6, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-42, 1, 0), 5, "US", "DMR", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(42, 1, 0), 5, "RU", "RIFLE", "HEAVY");
    }

    private void buildLongRange10v10(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 76, 8);
        for (int z = -14; z <= 14; z += 7) {
            coverLine(level, c.offset(-38, 1, z), 5, true);
            coverLine(level, c.offset(34, 1, z + 2), 5, true);
        }
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-52, 1, -4), 10, "US", "RIFLE", "MEDIUM");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(52, 1, 4), 10, "RU", "RIFLE", "HEAVY");
    }

    private void buildLongRangeSuppressionTest(ServerLevel level, BlockPos c) {
        prepareFlatArena(level, c, 72, 8);
        coverLine(level, c.offset(-36, 1, -6), 8, true);
        coverLine(level, c.offset(28, 1, 6), 8, true);
        spawnMany(level, ModEntities.USUNIT.get(), c.offset(-46, 1, 0), 3, "US", "LMG", "HEAVY");
        spawnMany(level, ModEntities.RUUNIT.get(), c.offset(42, 1, 0), 8, "RU", "PISTOL", "LIGHT");
        scenarioNote(level, "Long-range suppression test validates sustained suppression, cover requests and performance under long-range fire.");
    }

    private void scenarioNote(ServerLevel level, String message) {
        if (this.logger != null) {
            this.logger.message("SCENARIO_NOTE", level.getGameTime(), message);
        }
    }

    private void prepareFlatArena(ServerLevel level, BlockPos c, int radius, int height) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                setBlock(level, c.offset(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 1; y <= height; y++) {
                    setBlock(level, c.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private void platform(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        forEachBox(from, to, pos -> setBlock(level, pos, state));
    }

    private void wall(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        forEachBox(from, to, pos -> setBlock(level, pos, state));
    }

    private void coverLine(ServerLevel level, BlockPos start, int length, boolean alongX) {
        for (int i = 0; i < length; i++) {
            BlockPos pos = alongX ? start.offset(i, 0, 0) : start.offset(0, 0, i);
            setBlock(level, pos, Blocks.COBBLESTONE.defaultBlockState());
            setBlock(level, pos.above(), Blocks.COBBLESTONE.defaultBlockState());
        }
    }

    private void setDoorway(ServerLevel level, BlockPos base) {
        setBlock(level, base, Blocks.AIR.defaultBlockState());
        setBlock(level, base.above(), Blocks.AIR.defaultBlockState());
        setBlock(level, base.above(2), Blocks.AIR.defaultBlockState());
    }

    private void forEachBox(BlockPos from, BlockPos to, Consumer<BlockPos> consumer) {
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    consumer.accept(new BlockPos(x, y, z));
                }
            }
        }
    }

    private void setBlock(ServerLevel level, BlockPos pos, BlockState state) {
        this.originalBlocks.putIfAbsent(pos.immutable(), level.getBlockState(pos));
        level.setBlock(pos, state, 3);
    }

    private void spawnMany(ServerLevel level, EntityType<? extends AbstractUnit> type, BlockPos origin, int count, String label, String loadoutProfile, String armorPreset) {
        RandomSource random = level.getRandom();
        List<Mob> group = this.scenarioGroups.computeIfAbsent(label, ignored -> new ArrayList<>());
        for (int i = 0; i < count; i++) {
            AbstractUnit unit = type.create(level);
            if (unit == null) {
                continue;
            }
            int dx = count == 1 ? 0 : (i % 5) - 2;
            int dz = count == 1 ? 0 : (i / 5) * 2;
            BlockPos pos = origin.offset(dx, 0, dz);
            unit.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            unit.getPersistentData().putString(SESSION_TAG, this.sessionId != null ? this.sessionId : "unknown");
            String stableId = label + "_" + (i + 1);
            unit.getPersistentData().putString("SemScenarioLabel", stableId);
            unit.getPersistentData().putString("SemScenarioStableId", stableId);
            unit.getPersistentData().putString("SemScenarioLoadoutProfile", loadoutProfile);
            unit.getPersistentData().putString("SemScenarioArmorPreset", armorPreset);
            this.scenarioEntityLabels.put(unit.getUUID(), stableId);

            unit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, (SpawnGroupData) null, null);
            unit.setPersistenceRequired();
            level.addFreshEntity(unit);
            group.add(unit);
            this.spawnedEntities.add(unit.getUUID());
            if (this.logger != null) {
                this.logger.entity("ENTITY_SPAWN", level.getGameTime(), unit);
                this.logger.entity("LOADOUT_ASSIGNED", level.getGameTime(), unit);
            }
        }
    }

    public synchronized int clearNonPlayerEntities(ServerLevel level) {
        int removed = 0;
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            entities.add(entity);
        }
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                continue;
            }
            entity.discard();
            removed++;
        }
        return removed;
    }

    private int clearScenarioArea(ServerLevel level, BlockPos cleanupCenter) {
        AABB box = AABB.ofSize(Vec3.atCenterOf(cleanupCenter), SCENARIO_CLEANUP_RADIUS * 2.0D, SCENARIO_CLEANUP_RADIUS * 2.0D, SCENARIO_CLEANUP_RADIUS * 2.0D);
        int removed = 0;
        for (AbstractUnit unit : level.getEntitiesOfClass(AbstractUnit.class, box, unit -> unit.isAlive() && !unit.isRemoved())) {
            unit.discard();
            removed++;
        }
        return removed;
    }

    private void bootstrapIdleSoundScenario(ServerLevel level) {
        List<Mob> shooters = this.scenarioGroups.getOrDefault("RU_SHOOTER", Collections.emptyList());
        List<Mob> targets = this.scenarioGroups.getOrDefault("US_TARGET", Collections.emptyList());
        int assigned = assignTargets(level, shooters, targets);
        this.engagementBootstrapped = true;
        if (this.logger != null) {
            this.logger.message("ENGAGEMENT_SKIPPED", level.getGameTime(), "scenario=sound_awareness_idle forcedOnlyShooterTargets=" + assigned + " idleListeners=" + this.scenarioGroups.getOrDefault("US_IDLE", Collections.emptyList()).size());
            this.logger.message("IDLE_SOUND_SCENARIO_READY", level.getGameTime(), "shooterTargets=" + assigned + " expectedEvents=GUNFIRE_HEARD,ALERT_FROM_SOUND,INVESTIGATE_SOUND");
        }
    }

    public synchronized int forceEngagement() {
        if (!this.isRunning() || this.level == null) {
            return 0;
        }
        return bootstrapEngagement(this.level);
    }

    private int bootstrapEngagement(ServerLevel level) {
        if (this.engagementBootstrapped && this.logger != null) {
            this.logger.message("ENGAGEMENT_REBOOTSTRAP", level.getGameTime(), "Manual engagement bootstrap requested.");
        }

        List<Mob> friendlySide = collectGroups("US", "PMC");
        List<Mob> hostileSide = collectGroups("RU", "HOSTILE");
        int assigned = assignTargets(level, friendlySide, hostileSide);
        assigned += assignTargets(level, hostileSide, friendlySide);
        this.engagementBootstrapped = true;

        if (this.logger != null) {
            this.logger.message("ENGAGEMENT_STARTED", level.getGameTime(), "assignedTargets=" + assigned + " friendlySide=" + friendlySide.size() + " hostileSide=" + hostileSide.size());
        }
        return assigned;
    }

    private List<Mob> collectGroups(String... labels) {
        List<Mob> result = new ArrayList<>();
        for (String label : labels) {
            List<Mob> group = this.scenarioGroups.get(label);
            if (group != null) {
                result.addAll(group);
            }
        }
        return result;
    }

    private int assignTargets(ServerLevel level, List<Mob> attackers, List<Mob> targets) {
        int assigned = 0;
        for (Mob attacker : attackers) {
            if (attacker == null || !attacker.isAlive() || attacker.isRemoved()) {
                continue;
            }
            LivingEntity target = nearestLiving(attacker, targets);
            if (target == null) {
                continue;
            }
            attacker.setTarget(target);
            if (attacker instanceof AbstractUnit unit) {
                unit.setLastKnownTargetPos(target.position());
                unit.setLastSeenTargetTick(level.getGameTime());
                unit.setCoverSearchOrigin(attacker.blockPosition());
            }
            assigned++;
            if (this.logger != null) {
                this.logger.entityPair("TARGET_ASSIGNED", level.getGameTime(), attacker, target);
            }
        }
        return assigned;
    }

    @Nullable
    private LivingEntity nearestLiving(Mob source, List<Mob> candidates) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob candidate : candidates) {
            if (candidate == null || candidate == source || !candidate.isAlive() || candidate.isRemoved()) {
                continue;
            }
            double distance = source.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static Path resolveMinecraftRoot() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    private void closeLogger() {
        if (this.logger != null) {
            try {
                this.logger.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close SEM scenario log", e);
            }
        }
    }

    private void resetActiveStateOnly() {
        this.state = ScenarioState.IDLE;
        this.activeScenario = null;
        this.sessionId = null;
        this.level = null;
        this.logger = null;
        this.center = null;
        this.startTick = 0L;
        this.durationTicks = 0;
        this.lastSnapshotTick = -9999;
        this.spawnedEntities.clear();
        this.deadEntitiesLogged.clear();
        this.originalBlocks.clear();
        this.scenarioGroups.clear();
        this.scenarioEntityLabels.clear();
        this.lastSnapshotState.clear();
        this.lastSnapshotTarget.clear();
        this.engagementBootstrapped = false;
        this.scenarioTriggeredEvents.clear();
    }

    public enum ScenarioState {
        IDLE,
        STARTING,
        RUNNING,
        STOPPING,
        CLEANING,
        FAILED
    }
}
