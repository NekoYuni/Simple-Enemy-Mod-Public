package net.nekoyuni.SimpleEnemyMod.entity.ai.awareness;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;
import net.nekoyuni.SimpleEnemyMod.scenario.SemTickProfiler;

import java.util.Locale;

/** Generic combat-noise awareness for SEM units, without public-branch Bandit dependencies. */
public final class CombatSoundAwarenessManager {
    private CombatSoundAwarenessManager() {
    }

    public static void emitGunshot(Level level, Entity source, Vec3 position, double radius) {
        emitSound(level, source, position, radius, true);
    }

    private static void emitSound(Level level, Entity source, Vec3 position, double radius, boolean gunshot) {
        if (!TacticalAiConfig.enableTacticalHearing()) {
            return;
        }
        if (level == null || level.isClientSide() || source == null || position == null || radius <= 0.0D) {
            return;
        }

        if (gunshot) {
            SemTickProfiler.recordGunfireEvent();
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "GUNFIRE_BROADCAST",
                    source,
                    "radius=" + String.format(Locale.ROOT, "%.2f", radius)
                            + " pos=" + formatVec(position)
            );
        }

        AABB searchBox = new AABB(position, position).inflate(radius);
        for (AbstractUnit unit : level.getEntitiesOfClass(AbstractUnit.class, searchBox)) {
            if (!unit.isAlive() || unit == source) {
                continue;
            }
            if (!isHostileSoundFor(unit, source)) {
                continue;
            }
            double distance = unit.position().distanceTo(position);
            double blockedPenalty = source instanceof LivingEntity livingSource && unit.hasLineOfSight(livingSource)
                    ? 1.0D
                    : 0.55D;
            double strength = Math.max(0.0D, 1.0D - distance / radius) * blockedPenalty;

            if (gunshot && strength >= TacticalAiConfig.soundMinStrength()
                    && !unit.shouldAcceptCombatNoiseFrom(source, true)) {
                continue;
            }

            if (gunshot && strength >= TacticalAiConfig.soundMinStrength()) {
                SemTickProfiler.recordSoundAwarenessEvent();
                SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                        "GUNFIRE_HEARD",
                        unit,
                        "source=" + source.getUUID()
                                + " strength=" + String.format(Locale.ROOT, "%.2f", strength)
                                + " pos=" + formatVec(position)
                );
            }

            unit.hearCombatNoise(position, source, gunshot, strength);
        }
    }

    private static boolean isHostileSoundFor(AbstractUnit listener, Entity source) {
        if (source == null || source == listener) {
            return false;
        }
        if (UnitFactionTags.isSameTaggedFaction(listener, source) || UnitFactionTags.isFriendlyFire(listener, source)) {
            return false;
        }
        if (source instanceof Player && UnitFactionTags.isPmcUnit(listener)) {
            return false;
        }
        if (source instanceof LivingEntity livingSource) {
            return listener.canAttack(livingSource);
        }
        return UnitFactionTags.isTaggedUnit(source);
    }

    private static String formatVec(Vec3 vec) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vec.x, vec.y, vec.z);
    }
}
