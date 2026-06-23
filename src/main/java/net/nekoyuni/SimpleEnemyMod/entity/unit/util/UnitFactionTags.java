package net.nekoyuni.SimpleEnemyMod.entity.unit.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.config.common.FactionsConfig;

public final class UnitFactionTags {

    public static final TagKey<EntityType<?>> PMC_UNITS = create("pmc_units");
    public static final TagKey<EntityType<?>> RU_UNITS = create("ru_units");
    public static final TagKey<EntityType<?>> US_UNITS = create("us_units");

    private UnitFactionTags() {
    }

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(SimpleEnemyMod.MODID, name));
    }

    public static boolean isPmcUnit(Entity entity) {
        return isIn(entity, PMC_UNITS);
    }

    public static boolean isRuUnit(Entity entity) {
        return isIn(entity, RU_UNITS);
    }

    public static boolean isUsUnit(Entity entity) {
        return isIn(entity, US_UNITS);
    }

    public static boolean isTaggedUnit(Entity entity) {
        return isPmcUnit(entity) || isRuUnit(entity) || isUsUnit(entity);
    }

    public static boolean isSameTaggedFaction(Entity first, Entity second) {
        if (first == null || second == null) {
            return false;
        }

        return (isPmcUnit(first) && isPmcUnit(second))
                || (isRuUnit(first) && isRuUnit(second))
                || (isUsUnit(first) && isUsUnit(second));
    }

    public static boolean isFriendlyFire(Entity victim, Entity attacker) {
        if (victim == null || attacker == null || victim == attacker) {
            return false;
        }

        if (isSameTaggedFaction(victim, attacker)) {
            return true;
        }

        return victim instanceof Player && isPmcUnit(attacker);
    }

    public static boolean isFriendlyToPmc(LivingEntity target) {
        return isPmcUnit(target)
                || (FactionsConfig.RU_UNITS_FRIENDLY.get() && isRuUnit(target))
                || (FactionsConfig.US_UNITS_FRIENDLY.get() && isUsUnit(target));
    }

    public static boolean isFriendlyToRu(LivingEntity target) {
        return isRuUnit(target)
                || (FactionsConfig.RU_UNITS_FRIENDLY.get() && isPmcUnit(target));
    }

    public static boolean isFriendlyToUs(LivingEntity target) {
        return isUsUnit(target)
                || (FactionsConfig.US_UNITS_FRIENDLY.get() && isPmcUnit(target));
    }

    private static boolean isIn(Entity entity, TagKey<EntityType<?>> tag) {
        return entity != null && entity.getType().is(tag);
    }
}
