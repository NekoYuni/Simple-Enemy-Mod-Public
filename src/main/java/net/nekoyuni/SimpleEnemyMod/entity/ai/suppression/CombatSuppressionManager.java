package net.nekoyuni.SimpleEnemyMod.entity.ai.suppression;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;
import net.nekoyuni.SimpleEnemyMod.scenario.SemTickProfiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** Generic Phase 3 suppression pressure for SEM units, adapted from the public AI branch. */
public final class CombatSuppressionManager {
    private static final long SUPPRESSION_LOG_COOLDOWN_TICKS = 80L;
    private static final float SUPPRESSION_LOG_MIN_AMOUNT = 0.30F;
    private static final Map<String, Long> LAST_APPLIED_LOG_BY_PAIR = new HashMap<>();

    private CombatSuppressionManager() {
    }

    public static void emitGunfireSuppression(Level level, Entity shooter, Vec3 impactOrTargetPos, double radius) {
        if (!TacticalAiConfig.enableTacticalSuppression()) {
            return;
        }
        if (level == null || level.isClientSide() || shooter == null || impactOrTargetPos == null || radius <= 0.0D) {
            return;
        }

        AABB box = new AABB(impactOrTargetPos, impactOrTargetPos).inflate(radius);
        for (AbstractUnit unit : level.getEntitiesOfClass(AbstractUnit.class, box)) {
            if (unit == shooter || !unit.isAlive()) {
                continue;
            }
            if (UnitFactionTags.isFriendlyFire(unit, shooter) || UnitFactionTags.isSameTaggedFaction(unit, shooter)) {
                continue;
            }

            double distance = unit.position().distanceTo(impactOrTargetPos);
            double strength = Math.max(0.0D, 1.0D - distance / radius);
            if (strength <= 0.05D) {
                continue;
            }

            float amount = (float) (TacticalAiConfig.suppressionImpactAmount() * strength);
            if (!unit.addGenericSuppression(shooter, impactOrTargetPos, amount)) {
                continue;
            }

            SemTickProfiler.recordSuppressionEvent();

            if (shouldLogSuppressionApplied(level, shooter, unit, amount)) {
                SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                        "SUPPRESSION_APPLIED",
                        unit,
                        "source=" + shooter.getUUID()
                                + " amount=" + String.format(Locale.ROOT, "%.2f", amount)
                                + " level=" + String.format(Locale.ROOT, "%.2f", unit.getGenericSuppressionLevel())
                                + " pos=" + fmt(impactOrTargetPos)
                );
            }
        }
    }

    private static boolean shouldLogSuppressionApplied(Level level, Entity shooter, AbstractUnit unit, float amount) {
        long now = level.getGameTime();
        if (LAST_APPLIED_LOG_BY_PAIR.size() > 512) {
            Iterator<Map.Entry<String, Long>> iterator = LAST_APPLIED_LOG_BY_PAIR.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() > 20L * 20L) {
                    iterator.remove();
                }
            }
        }

        String key = shooter.getUUID() + ":" + unit.getUUID();
        Long last = LAST_APPLIED_LOG_BY_PAIR.get(key);
        float currentLevel = unit.getGenericSuppressionLevel();
        boolean strongEnough = amount >= SUPPRESSION_LOG_MIN_AMOUNT
                || currentLevel >= TacticalAiConfig.suppressionStrongThreshold() * 1.20F;
        if (!strongEnough) {
            return false;
        }
        if (last != null && now - last < SUPPRESSION_LOG_COOLDOWN_TICKS) {
            return false;
        }
        LAST_APPLIED_LOG_BY_PAIR.put(key, now);
        return true;
    }

    private static String fmt(Vec3 vec) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vec.x, vec.y, vec.z);
    }
}
