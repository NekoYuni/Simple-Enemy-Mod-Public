package net.nekoyuni.SimpleEnemyMod.scenario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.LadderRouteNavigator;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
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
import java.util.Map;
import java.util.UUID;

/**
 * JSONL scenario logger adapted from the public branch.
 * Each line is a standalone JSON object to keep logs grep-friendly and analysis-friendly.
 */
public final class SemScenarioLogger implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final String scenarioId;
    private final String sessionId;
    private final Path logFile;
    private final BufferedWriter writer;

    public SemScenarioLogger(Path minecraftRoot, String scenarioId, String sessionId) throws IOException {
        this.scenarioId = scenarioId;
        this.sessionId = sessionId;
        Path directory = minecraftRoot.resolve("logs").resolve("sem_scenarios").resolve(scenarioId);
        Files.createDirectories(directory);
        this.logFile = directory.resolve(FILE_TIME.format(LocalDateTime.now()) + "_" + sessionId + ".jsonl");
        this.writer = Files.newBufferedWriter(this.logFile, StandardCharsets.UTF_8);
    }

    public Path getLogFile() {
        return this.logFile;
    }

    public void sessionStart(long tick, BlockPos center, int durationTicks, boolean loopMode) {
        JsonObject json = base("SESSION_START", tick);
        json.addProperty("centerX", center.getX());
        json.addProperty("centerY", center.getY());
        json.addProperty("centerZ", center.getZ());
        json.addProperty("durationTicks", durationTicks);
        json.addProperty("loopMode", loopMode);
        json.addProperty("runtime", "private_phase4_decision_spark_loop");
        write(json);
    }

    public void event(String type, long tick) {
        write(base(type, tick));
    }

    public void message(String type, long tick, String message) {
        JsonObject json = base(type, tick);
        json.addProperty("message", message);
        write(json);
    }

    public void summary(long tick, Map<String, ?> values) {
        JsonObject json = base("SESSION_SUMMARY", tick);
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number number) {
                json.addProperty(entry.getKey(), number);
            } else if (value instanceof Boolean bool) {
                json.addProperty(entry.getKey(), bool);
            } else {
                json.addProperty(entry.getKey(), String.valueOf(value));
            }
        }
        write(json);
    }

    public void entity(String type, long tick, Entity entity) {
        JsonObject json = base(type, tick);
        addEntity(json, entity);
        write(json);
    }

    public void entityMessage(String type, long tick, Entity entity, String message) {
        JsonObject json = base(type, tick);
        addEntity(json, entity);
        json.addProperty("message", message);
        write(json);
    }

    public void entityPair(String type, long tick, Entity source, Entity target) {
        JsonObject json = base(type, tick);
        addEntity(json, source);
        json.addProperty("targetUuid", target.getUUID().toString());
        json.addProperty("targetType", entityTypeString(target));
        json.addProperty("targetName", target.getDisplayName().getString());
        json.addProperty("targetX", target.getX());
        json.addProperty("targetY", target.getY());
        json.addProperty("targetZ", target.getZ());
        json.addProperty("targetDistance", Math.sqrt(source.distanceToSqr(target)));
        write(json);
    }

    public void entityStateChange(long tick, Entity entity, String from, String to) {
        JsonObject json = base("STATE_CHANGE", tick);
        addEntity(json, entity);
        json.addProperty("from", from);
        json.addProperty("to", to);
        write(json);
    }

    public void entitySnapshot(long tick, LivingEntity entity) {
        JsonObject json = base("ENTITY_SNAPSHOT", tick);
        addEntity(json, entity);
        json.addProperty("health", entity.getHealth());
        json.addProperty("maxHealth", entity.getMaxHealth());
        if (entity instanceof AbstractUnit unit) {
            json.addProperty("role", unit.getRole().name());
            json.addProperty("soldierState", unit.getSoldierState().name());
            json.addProperty("strategy", unit.getStrategy() != null ? unit.getStrategy().name() : "none");
            json.addProperty("ladderState", LadderRouteNavigator.debugState(unit));
            addBlockPos(json, "coverBlock", unit.getCoverBlock());
            addBlockPos(json, "coverSearchOrigin", unit.getCoverSearchOrigin());
            addVec3(json, "lastKnownTarget", unit.getLastKnownTargetPos());
            json.addProperty("lastSeenTargetTick", unit.getLastSeenTargetTick());
            json.addProperty("genericSuppressionLevel", unit.getGenericSuppressionLevel());
            json.addProperty("heardThreatTicks", unit.getHeardCombatNoiseTicks());
            json.addProperty("heardThreatStrength", unit.getLastHeardEnemyStrength());
            json.addProperty("heardThreatWasGunshot", unit.wasLastHeardThreatGunshot());
            json.addProperty("tacticalCombatState", unit.getTacticalCombatState().name());
            json.addProperty("threatPressure", unit.getThreatPressure());
            json.addProperty("combatConfidence", unit.getCombatConfidence());
            json.addProperty("tacticalSpeedMultiplier", unit.getTacticalSpeedMultiplier());
            if (unit.getSquadContext() != null) {
                json.addProperty("squadNearbyAllies", unit.getSquadContext().nearbyAllies());
                json.addProperty("squadEngagedAllies", unit.getSquadContext().engagedAllies());
                json.addProperty("squadSuppressedAllies", unit.getSquadContext().suppressedAllies());
                json.addProperty("squadNearbyHostiles", unit.getSquadContext().nearbyHostiles());
                json.addProperty("squadAverageAllyPressure", unit.getSquadContext().averageAllyPressure());
                json.addProperty("squadAverageAllyConfidence", unit.getSquadContext().averageAllyConfidence());
                json.addProperty("squadHasSharedEnemyMemory", unit.getSquadContext().hasSharedEnemyMemory());
                json.addProperty("squadSharedEnemyAgeTicks", unit.getSquadContext().sharedEnemyAgeTicks());
                json.addProperty("squadSharedDangerPressure", unit.getSquadContext().sharedDangerPressure());
                json.addProperty("squadMemorySource", unit.getSquadContext().memorySource());
                addVec3(json, "squadSharedEnemy", unit.getSquadContext().sharedEnemyPosition());
            }
            addVec3(json, "lastHeardEnemy", unit.getLastHeardEnemyPosition());
        }

        LivingEntity target = entity instanceof Mob mob ? mob.getTarget() : null;
        if (target != null) {
            json.addProperty("targetUuid", target.getUUID().toString());
            json.addProperty("targetType", entityTypeString(target));
            json.addProperty("targetName", target.getDisplayName().getString());
            json.addProperty("targetDistance", Math.sqrt(entity.distanceToSqr(target)));
        } else {
            json.addProperty("targetType", "none");
        }
        write(json);
    }

    public void blockEvent(String type, long tick, BlockPos pos, String block) {
        JsonObject json = base(type, tick);
        json.addProperty("x", pos.getX());
        json.addProperty("y", pos.getY());
        json.addProperty("z", pos.getZ());
        json.addProperty("block", block);
        write(json);
    }

    public void tickProfile(String type, long tick, long scenarioTick, SemTickProfiler.TickMetrics metrics,
                            int trackedEntities, int aliveEntities, int aliveFriendly, int aliveHostile) {
        JsonObject json = base(type, tick);
        json.addProperty("scenarioTick", scenarioTick);
        json.addProperty("severity", metrics.tickMs >= net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig.semTickSevereThresholdMs() ? "severe" :
                metrics.tickMs >= net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig.semTickWarnThresholdMs() ? "warning" : "sample");
        json.addProperty("tickMs", metrics.tickMs);
        json.addProperty("avg20TickMs", metrics.avg20TickMs);
        json.addProperty("max20TickMs", metrics.max20TickMs);
        json.addProperty("trackedEntities", trackedEntities);
        json.addProperty("aliveEntities", aliveEntities);
        json.addProperty("aliveFriendly", aliveFriendly);
        json.addProperty("aliveHostile", aliveHostile);
        json.addProperty("coverSearchesThisTick", metrics.coverSearchesThisTick);
        json.addProperty("coverCandidatesScoredThisTick", metrics.coverCandidatesScoredThisTick);
        json.addProperty("coverPathChecksThisTick", metrics.coverPathChecksThisTick);
        json.addProperty("coverBudgetDeniedThisTick", metrics.coverBudgetDeniedThisTick);
        json.addProperty("suppressionEventsThisTick", metrics.suppressionEventsThisTick);
        json.addProperty("gunfireEventsThisTick", metrics.gunfireEventsThisTick);
        json.addProperty("soundAwarenessEventsThisTick", metrics.soundAwarenessEventsThisTick);
        write(json);
    }

    private JsonObject base(String type, long tick) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("scenario", this.scenarioId);
        json.addProperty("sessionId", this.sessionId);
        json.addProperty("tick", tick);
        return json;
    }

    private void addEntity(JsonObject json, Entity entity) {
        UUID uuid = entity.getUUID();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("entityType", entityTypeString(entity));
        json.addProperty("name", entity.getDisplayName().getString());
        json.addProperty("scenarioLabel", entity.getPersistentData().getString("SemScenarioLabel"));
        json.addProperty("stableTestId", entity.getPersistentData().getString("SemScenarioStableId"));
        json.addProperty("loadoutProfile", entity.getPersistentData().getString("SemScenarioLoadoutProfile"));
        json.addProperty("armorPreset", entity.getPersistentData().getString("SemScenarioArmorPreset"));
        json.addProperty("x", entity.getX());
        json.addProperty("y", entity.getY());
        json.addProperty("z", entity.getZ());
        json.addProperty("alive", entity.isAlive());
        if (entity instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            json.addProperty("mainHand", mainHand.isEmpty() ? "empty" : mainHand.getItem().toString());
            json.addProperty("helmet", itemName(living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)));
            json.addProperty("chest", itemName(living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)));
            json.addProperty("legs", itemName(living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS)));
            json.addProperty("feet", itemName(living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)));
        }
    }

    private static String itemName(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getItem().toString();
    }

    private static String entityTypeString(Entity entity) {
        return entity.getType().toString();
    }

    private static void addBlockPos(JsonObject json, String prefix, @Nullable BlockPos pos) {
        if (pos == null) {
            json.addProperty(prefix, "none");
            return;
        }
        json.addProperty(prefix + "X", pos.getX());
        json.addProperty(prefix + "Y", pos.getY());
        json.addProperty(prefix + "Z", pos.getZ());
    }

    private static void addVec3(JsonObject json, String prefix, @Nullable net.minecraft.world.phys.Vec3 pos) {
        if (pos == null) {
            json.addProperty(prefix, "none");
            return;
        }
        json.addProperty(prefix + "X", pos.x());
        json.addProperty(prefix + "Y", pos.y());
        json.addProperty(prefix + "Z", pos.z());
    }

    public static synchronized void appendSparkStartLine(Path logFile, String scenarioId, String sessionId, long tick, String profileId) {
        appendSparkLifecycleLine(logFile, "SPARK_START", scenarioId, sessionId, tick, profileId, null);
    }

    public static synchronized void appendSparkStopLine(Path logFile, String scenarioId, String sessionId, long tick, String profileId) {
        appendSparkLifecycleLine(logFile, "SPARK_STOP", scenarioId, sessionId, tick, profileId, null);
    }

    public static synchronized void appendSparkProfileLine(Path logFile, String scenarioId, String sessionId, long tick, String profileId, String url) {
        appendSparkLifecycleLine(logFile, "SPARK_PROFILE", scenarioId, sessionId, tick, profileId, url);
    }

    /**
     * Backward-compatible alias retained for older call sites. Despite the method name, Spark profile
     * metadata is now appended in chronological order instead of being prepended before SESSION_START.
     */
    public static synchronized void prependSparkProfileLine(Path logFile, String scenarioId, String sessionId, long tick, String url) {
        appendSparkProfileLine(logFile, scenarioId, sessionId, tick, scenarioId + "_" + sessionId, url);
    }

    private static synchronized void appendSparkLifecycleLine(Path logFile, String type, String scenarioId, String sessionId, long tick, String profileId, @Nullable String url) {
        if (logFile == null || type == null || type.isBlank()) {
            return;
        }
        if ("SPARK_PROFILE".equals(type) && (url == null || url.isBlank())) {
            return;
        }
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("scenario", scenarioId);
            json.addProperty("sessionId", sessionId);
            json.addProperty("tick", tick);
            json.addProperty("profileId", profileId);
            if (url != null && !url.isBlank()) {
                json.addProperty("url", url);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(GSON.toJson(json));
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to append Spark lifecycle event {} to SEM scenario log {}", type, logFile, e);
        }
    }

    private void write(JsonObject json) {
        try {
            this.writer.write(GSON.toJson(json));
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to write SEM scenario log", e);
        }
    }

    @Override
    public void close() throws IOException {
        this.writer.flush();
        this.writer.close();
    }
}
