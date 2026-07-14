package net.nekoyuni.SimpleEnemyMod.entity.ai.decision;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.nekoyuni.SimpleEnemyMod.entity.ai.profile.role.RoleBehaviorProfile;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.ai.squad.SquadContext;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Compact Phase 4 tactical confidence/threat model.
 *
 * <p>This class deliberately reads only generic private-branch state: target, LoS, distance, health, suppression,
 * nearby tagged units and inferred weapon archetype.</p>
 */
public record CombatDecisionContext(
        float threatPressure,
        float combatConfidence,
        boolean shouldAdvance,
        boolean shouldHoldAmbush,
        boolean cqbMode,
        boolean killzone,
        RoleBehaviorProfile roleProfile,
        @Nullable SquadContext squadContext,
        String reason
) {
    public static CombatDecisionContext empty(AbstractUnit unit) {
        RoleBehaviorProfile profile = RoleBehaviorProfile.forUnit(unit);
        return new CombatDecisionContext(0.0F, 0.50F, false, false, false, false, profile, SquadContext.empty(), "no_target");
    }

    public static CombatDecisionContext evaluate(AbstractUnit unit, LivingEntity target, boolean hasLos, double distance) {
        if (unit == null || target == null || !target.isAlive()) {
            return empty(unit);
        }

        RoleBehaviorProfile profile = RoleBehaviorProfile.forUnit(unit);
        float healthRatio = unit.getHealth() / Math.max(1.0F, unit.getMaxHealth());
        boolean targetCanSeeUnit = target.hasLineOfSight(unit);
        boolean cqb = isCqb(distance, hasLos, unit, target);
        int nearbyAllies = countNearbyAllies(unit, 14.0D);
        int nearbyHostiles = countNearbyHostiles(unit, 18.0D);
        float suppression = unit.getGenericSuppressionLevel();

        float pressure = 0.12F;
        pressure += (1.0F - healthRatio) * 0.38F;
        pressure += distancePressure(distance);
        pressure += suppression * 0.16F * profile.suppressionSensitivity();
        if (target instanceof Player) pressure += 0.12F;
        if (hasLos) pressure += 0.08F;
        if (targetCanSeeUnit) pressure += 0.10F;
        if (cqb) pressure += cqbPressure(profile);
        pressure += weaponThreat(target) * 0.18F;
        pressure += Math.max(0, nearbyHostiles - nearbyAllies) * 0.08F;
        pressure = clamp01(pressure * profile.threatMultiplier());

        float confidence = 0.52F;
        confidence += healthRatio * 0.28F;
        confidence += Math.min(nearbyAllies, 4) * 0.06F;
        confidence -= Math.min(nearbyHostiles, 5) * 0.035F;
        confidence -= pressure * 0.42F;
        confidence += profile.advanceBias();
        confidence *= profile.confidenceMultiplier();
        if (!hasLos && distance <= 10.0D) confidence -= 0.10F;
        if (cqb) confidence += cqbConfidenceModifier(profile, hasLos);
        if (distance < profile.preferredMinRange()) confidence -= cqb ? 0.14F : 0.09F;
        if (distance > profile.preferredMaxRange()) confidence -= profile.frontline() ? 0.04F : 0.13F;
        confidence = clamp01(confidence);

        boolean killzone = pressure >= 0.88F && healthRatio <= 0.55F && hasLos && targetCanSeeUnit;
        boolean closePush = cqb
                && hasLos
                && (profile.archetype() == RoleBehaviorProfile.Archetype.SHOTGUN
                || profile.archetype() == RoleBehaviorProfile.Archetype.SMG
                || profile.archetype() == RoleBehaviorProfile.Archetype.PISTOL)
                && confidence >= 0.50F;
        boolean rangePush = hasLos && distance > profile.preferredMaxRange() && profile.frontline() && confidence >= 0.55F && pressure < 0.72F;
        boolean shouldAdvance = closePush || rangePush || (confidence >= 0.74F && pressure < 0.64F);
        boolean shouldHoldAmbush = (!hasLos && distance <= 14.0D && profile.prefersAmbush() && confidence < 0.68F)
                || (!hasLos && cqb && confidence < 0.52F);

        String reason = "hp=" + fmt(healthRatio)
                + " dist=" + fmt((float) distance)
                + " los=" + hasLos
                + " allies=" + nearbyAllies
                + " hostiles=" + nearbyHostiles
                + " suppression=" + fmt(suppression)
                + " archetype=" + profile.archetype().name()
                + " cqb=" + cqb
                + " killzone=" + killzone;

        return new CombatDecisionContext(pressure, confidence, shouldAdvance, shouldHoldAmbush, cqb, killzone, profile, SquadContext.empty(), reason);
    }

    public CombatDecisionContext withSquadContext(SquadContext squadContext) {
        if (squadContext == null) {
            squadContext = SquadContext.empty();
        }

        float adjustedPressure = this.threatPressure;
        float adjustedConfidence = this.combatConfidence;

        if (squadContext.hasFireSupport()) {
            adjustedConfidence += Math.min(0.16F, squadContext.engagedAllies() * 0.035F);
            adjustedPressure -= Math.min(0.08F, squadContext.engagedAllies() * 0.018F);
        }
        if (squadContext.isLocallyOutnumbered()) {
            adjustedPressure += Math.min(0.14F, (squadContext.nearbyHostiles() - squadContext.localFireteamSize()) * 0.035F);
            adjustedConfidence -= Math.min(0.10F, (squadContext.nearbyHostiles() - squadContext.localFireteamSize()) * 0.025F);
        }
        if (squadContext.suppressedAllies() > 0) {
            adjustedPressure += Math.min(0.12F, squadContext.suppressedAllies() * 0.04F);
        }
        if (squadContext.hasSharedEnemyMemory() && squadContext.sharedEnemyAgeTicks() <= 120L) {
            adjustedPressure += Math.min(0.10F, 0.04F + squadContext.sharedDangerPressure() * 0.05F);
            if (!this.cqbMode && !this.shouldAdvance) {
                adjustedConfidence += Math.min(0.05F, squadContext.engagedAllies() * 0.015F);
            }
        }

        adjustedPressure = clamp01(adjustedPressure);
        adjustedConfidence = clamp01(adjustedConfidence);

        boolean adjustedAdvance = this.shouldAdvance && adjustedConfidence >= 0.50F && adjustedPressure < 0.78F;
        boolean adjustedHoldAmbush = this.shouldHoldAmbush
                || (squadContext.hasSharedEnemyMemory() && !this.cqbMode && adjustedPressure >= 0.55F && adjustedConfidence < 0.68F);
        boolean adjustedKillzone = this.killzone || (adjustedPressure >= 0.92F && adjustedConfidence <= 0.34F);

        String adjustedReason = this.reason
                + " squadAllies=" + squadContext.nearbyAllies()
                + " squadEngaged=" + squadContext.engagedAllies()
                + " squadSuppressed=" + squadContext.suppressedAllies()
                + " sharedMemory=" + squadContext.hasSharedEnemyMemory()
                + " sharedAge=" + squadContext.sharedEnemyAgeTicks();

        return new CombatDecisionContext(
                adjustedPressure,
                adjustedConfidence,
                adjustedAdvance,
                adjustedHoldAmbush,
                this.cqbMode,
                adjustedKillzone,
                this.roleProfile,
                squadContext,
                adjustedReason
        );
    }

    private static boolean isCqb(double distance, boolean hasLos, AbstractUnit unit, LivingEntity target) {
        if (distance <= 7.0D) return true;
        if (distance <= 11.0D && !hasLos) return true;
        return distance <= 12.0D && Math.abs(unit.getY() - target.getY()) <= 3.0D;
    }

    private static float cqbPressure(RoleBehaviorProfile profile) {
        return switch (profile.archetype()) {
            case SHOTGUN -> 0.04F;
            case SMG -> 0.08F;
            case PISTOL -> 0.12F;
            case DMR -> 0.24F;
            case MG -> 0.16F;
            case SUPPORT -> 0.22F;
            default -> 0.14F;
        };
    }

    private static float cqbConfidenceModifier(RoleBehaviorProfile profile, boolean hasLos) {
        float losPenalty = hasLos ? 0.0F : -0.05F;
        return switch (profile.archetype()) {
            case SHOTGUN -> 0.14F + losPenalty;
            case SMG -> 0.06F + losPenalty;
            case PISTOL -> 0.02F + losPenalty;
            case DMR -> -0.22F;
            case MG -> -0.12F;
            case SUPPORT -> -0.18F;
            default -> -0.05F + losPenalty;
        };
    }

    private static float distancePressure(double distance) {
        if (distance <= 5.0D) return 0.24F;
        if (distance <= 10.0D) return 0.18F;
        if (distance <= 20.0D) return 0.10F;
        return 0.04F;
    }

    private static int countNearbyAllies(AbstractUnit unit, double radius) {
        return unit.level().getEntitiesOfClass(AbstractUnit.class, unit.getBoundingBox().inflate(radius), candidate ->
                candidate != unit && candidate.isAlive() && UnitFactionTags.isSameTaggedFaction(unit, candidate)
        ).size();
    }

    private static int countNearbyHostiles(AbstractUnit unit, double radius) {
        return unit.level().getEntitiesOfClass(AbstractUnit.class, unit.getBoundingBox().inflate(radius), candidate ->
                candidate != unit && candidate.isAlive() && !UnitFactionTags.isSameTaggedFaction(unit, candidate)
        ).size();
    }

    private static float weaponThreat(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        if (main.isEmpty()) return 0.05F;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(main.getItem());
        String path = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
        if (path.contains("m249") || path.contains("rpk") || path.contains("lmg") || path.contains("mg")) return 1.0F;
        if (path.contains("shotgun") || path.contains("m1014") || path.contains("aa12") || path.contains("m870") || path.contains("db_short")) return 0.92F;
        if (path.contains("mk14") || path.contains("msks") || path.contains("dmr")) return 0.78F;
        if (path.contains("rifle") || path.contains("ak") || path.contains("m16") || path.contains("m4")) return 0.68F;
        if (path.contains("smg") || path.contains("mp5") || path.contains("ump") || path.contains("p90")) return 0.58F;
        if (path.contains("pistol") || path.contains("glock") || path.contains("m1911")) return 0.42F;
        return 0.35F;
    }

    private static float clamp01(float value) {
        if (value < 0.0F) return 0.0F;
        if (value > 1.0F) return 1.0F;
        return value;
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
