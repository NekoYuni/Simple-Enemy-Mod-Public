package net.nekoyuni.SimpleEnemyMod.entity.ai.squad;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.decision.CombatDecisionContext;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Generic Phase 4 squad memory.
 *
 * <p>The public branch used heavier squad/scenario objects. This private-branch implementation keeps the architecture
 * tag-based and local: memories are keyed by dimension + faction + coarse combat cell and are updated by any allied unit
 * that sees, hears or is suppressed by a hostile source. Nearby allies then read that memory as shared last-known enemy
 * information.</p>
 */
public final class SquadCombatMemory {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, MemoryEntry> MEMORY_BY_CELL = new HashMap<>();
    private static final Map<UUID, Long> LAST_SHARED_LOG_BY_UNIT = new HashMap<>();
    private static final long SHARED_LOG_COOLDOWN_TICKS = 80L;

    private SquadCombatMemory() {
    }

    public static SquadContext updateAndGetContext(AbstractUnit unit, @Nullable LivingEntity target,
                                                    boolean hasLos, double distance,
                                                    @Nullable CombatDecisionContext baseContext) {
        if (unit == null || unit.level().isClientSide()) {
            return SquadContext.empty();
        }
        if (!TacticalAiConfig.enableSquadContext() && !TacticalAiConfig.enableSquadCombatMemory()) {
            return SquadContext.empty();
        }

        long now = unit.level().getGameTime();
        cleanup(now);

        if (TacticalAiConfig.enableSquadCombatMemory()) {
            recordUnitObservation(unit, target, hasLos, baseContext, now);
        }

        SquadContext context = buildLocalContext(unit, now);
        maybeLogSharedMemoryUse(unit, context, now);
        return context;
    }

    public static void recordSoundMemory(AbstractUnit unit, Vec3 soundPosition, @Nullable Object source, double strength) {
        if (unit == null || soundPosition == null || unit.level().isClientSide() || !TacticalAiConfig.enableSquadCombatMemory()) {
            return;
        }
        float pressure = (float) Math.max(0.12D, Math.min(1.0D, strength));
        updateMemory(unit, soundPosition, null, unit.level().getGameTime(), pressure, "sound");
    }

    public static void recordSuppressionMemory(AbstractUnit unit, Vec3 dangerPosition, @Nullable Object source, float amount) {
        if (unit == null || dangerPosition == null || unit.level().isClientSide() || !TacticalAiConfig.enableSquadCombatMemory()) {
            return;
        }
        float pressure = Math.max(0.18F, Math.min(1.0F, amount * 2.0F));
        updateMemory(unit, dangerPosition, null, unit.level().getGameTime(), pressure, "suppression");
    }

    @Nullable
    public static Vec3 getSharedEnemyPosition(AbstractUnit unit) {
        SquadContext context = unit == null ? SquadContext.empty() : unit.getSquadContext();
        return context != null && context.hasSharedEnemyMemory() ? context.sharedEnemyPosition() : null;
    }

    private static void recordUnitObservation(AbstractUnit unit, @Nullable LivingEntity target,
                                              boolean hasLos, @Nullable CombatDecisionContext baseContext, long now) {
        if (target != null && target.isAlive() && target.level() == unit.level() && hasLos) {
            float pressure = baseContext == null ? 0.35F : Math.max(0.20F, baseContext.threatPressure());
            updateMemory(unit, target.position(), target.getUUID(), now, pressure, "visual_contact");
            return;
        }

        Vec3 lastKnown = unit.getLastKnownTargetPos();
        if (lastKnown != null && now - unit.getLastSeenTargetTick() <= TacticalAiConfig.squadMemoryTicks()) {
            float pressure = baseContext == null ? 0.22F : Math.max(0.12F, baseContext.threatPressure() * 0.75F);
            updateMemory(unit, lastKnown, target != null ? target.getUUID() : null, now, pressure, "last_known_position");
        }
    }

    private static SquadContext buildLocalContext(AbstractUnit unit, long now) {
        double radius = TacticalAiConfig.squadContextRadius();
        AABB box = unit.getBoundingBox().inflate(radius);

        int allies = 0;
        int engagedAllies = 0;
        int suppressedAllies = 0;
        int hostiles = 0;
        float totalPressure = 0.0F;
        float totalConfidence = 0.0F;
        int tacticalAllies = 0;

        for (AbstractUnit candidate : unit.level().getEntitiesOfClass(AbstractUnit.class, box)) {
            if (candidate == unit || !candidate.isAlive()) {
                continue;
            }
            if (UnitFactionTags.isSameTaggedFaction(unit, candidate)) {
                allies++;
                CombatDecisionContext context = candidate.getLastCombatDecisionContext();
                if (context != null) {
                    totalPressure += context.threatPressure();
                    totalConfidence += context.combatConfidence();
                    tacticalAllies++;
                }
                if (candidate.getTarget() != null && candidate.getTarget().isAlive()) {
                    engagedAllies++;
                }
                if (candidate.getGenericSuppressionLevel() >= TacticalAiConfig.suppressionStrongThreshold()) {
                    suppressedAllies++;
                }
            } else {
                hostiles++;
            }
        }

        float avgPressure = tacticalAllies == 0 ? unit.getThreatPressure() : totalPressure / tacticalAllies;
        float avgConfidence = tacticalAllies == 0 ? unit.getCombatConfidence() : totalConfidence / tacticalAllies;

        MemoryEntry memory = findBestMemory(unit, now);
        if (memory == null) {
            return new SquadContext(allies, engagedAllies, suppressedAllies, hostiles, avgPressure, avgConfidence,
                    false, null, Long.MAX_VALUE, 0.0F, "none");
        }

        long age = Math.max(0L, now - memory.lastUpdatedTick);
        return new SquadContext(allies, engagedAllies, suppressedAllies, hostiles, avgPressure, avgConfidence,
                true, memory.enemyPosition, age, memory.dangerPressure, memory.sourceReason);
    }

    @Nullable
    private static MemoryEntry findBestMemory(AbstractUnit unit, long now) {
        if (!TacticalAiConfig.enableSquadCombatMemory()) {
            return null;
        }
        String faction = factionKey(unit);
        ResourceLocation dimension = unit.level().dimension().location();
        double maxDistanceSqr = TacticalAiConfig.squadMemoryShareRadius() * TacticalAiConfig.squadMemoryShareRadius();
        int maxAge = TacticalAiConfig.squadMemoryTicks();

        MemoryEntry best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MemoryEntry entry : MEMORY_BY_CELL.values()) {
            if (!entry.dimension.equals(dimension) || !entry.faction.equals(faction)) {
                continue;
            }
            long age = now - entry.lastUpdatedTick;
            if (age < 0L || age > maxAge) {
                continue;
            }
            double distSqr = unit.position().distanceToSqr(entry.enemyPosition);
            if (distSqr > maxDistanceSqr) {
                continue;
            }
            double agePenalty = age / (double) Math.max(1, maxAge);
            double distancePenalty = Math.sqrt(distSqr) / Math.max(1.0D, TacticalAiConfig.squadMemoryShareRadius());
            double score = entry.dangerPressure * 2.0D - agePenalty - distancePenalty;
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        return best;
    }

    private static void updateMemory(AbstractUnit observer, Vec3 position, @Nullable UUID targetUuid,
                                     long now, float pressure, String reason) {
        if (observer == null || position == null) {
            return;
        }
        String key = memoryKey(observer.level(), observer, position);
        MemoryEntry previous = MEMORY_BY_CELL.get(key);
        float blendedPressure = previous == null
                ? pressure
                : Math.max(pressure, previous.dangerPressure * 0.88F);
        MemoryEntry updated = new MemoryEntry(
                observer.level().dimension().location(),
                factionKey(observer),
                position,
                targetUuid,
                observer.getUUID(),
                now,
                Math.max(0.0F, Math.min(1.0F, blendedPressure)),
                reason
        );
        MEMORY_BY_CELL.put(key, updated);
    }

    private static void maybeLogSharedMemoryUse(AbstractUnit unit, SquadContext context, long now) {
        if (context == null || !context.hasSharedEnemyMemory() || context.sharedEnemyPosition() == null) {
            return;
        }
        Long last = LAST_SHARED_LOG_BY_UNIT.get(unit.getUUID());
        if (last != null && now - last < SHARED_LOG_COOLDOWN_TICKS) {
            return;
        }
        LAST_SHARED_LOG_BY_UNIT.put(unit.getUUID(), now);
        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                "SQUAD_MEMORY_SHARED",
                unit,
                "source=" + context.memorySource()
                        + " age=" + context.sharedEnemyAgeTicks()
                        + " danger=" + String.format(Locale.ROOT, "%.2f", context.sharedDangerPressure())
                        + " pos=" + formatVec(context.sharedEnemyPosition())
                        + " allies=" + context.nearbyAllies()
                        + " engagedAllies=" + context.engagedAllies()
        );
    }

    private static void cleanup(long now) {
        int ttl = TacticalAiConfig.squadMemoryTicks() * 2;
        Iterator<Map.Entry<String, MemoryEntry>> iterator = MEMORY_BY_CELL.entrySet().iterator();
        while (iterator.hasNext()) {
            MemoryEntry entry = iterator.next().getValue();
            if (now - entry.lastUpdatedTick > ttl) {
                iterator.remove();
            }
        }
        if (LAST_SHARED_LOG_BY_UNIT.size() > 512) {
            LAST_SHARED_LOG_BY_UNIT.entrySet().removeIf(entry -> now - entry.getValue() > ttl);
        }
    }

    private static String memoryKey(Level level, AbstractUnit unit, Vec3 pos) {
        int grid = Math.max(8, TacticalAiConfig.squadMemoryGridSize());
        int cx = (int) Math.floor(pos.x / grid);
        int cz = (int) Math.floor(pos.z / grid);
        int cy = (int) Math.floor(pos.y / Math.max(4, grid / 2));
        return level.dimension().location() + ":" + factionKey(unit) + ":" + cx + ":" + cy + ":" + cz;
    }

    private static String factionKey(AbstractUnit unit) {
        if (UnitFactionTags.isPmcUnit(unit)) return "PMC";
        if (UnitFactionTags.isRuUnit(unit)) return "RU";
        if (UnitFactionTags.isUsUnit(unit)) return "US";
        return "UNKNOWN";
    }

    private static String formatVec(Vec3 vec) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vec.x, vec.y, vec.z);
    }

    private record MemoryEntry(ResourceLocation dimension,
                               String faction,
                               Vec3 enemyPosition,
                               @Nullable UUID targetUuid,
                               UUID observerUuid,
                               long lastUpdatedTick,
                               float dangerPressure,
                               String sourceReason) {
    }
}
