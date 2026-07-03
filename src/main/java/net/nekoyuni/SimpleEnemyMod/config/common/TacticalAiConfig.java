package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Neutral tactical AI switches migrated from the public AI branch.
 *
 * Keep this file focused on generic NPC behavior; suppression, threat scoring,
 * loot, bandits and scenario systems belong to later migration phases.
 */
public class TacticalAiConfig {

    public static ForgeConfigSpec.BooleanValue ENABLE_SOFT_OBSTACLE_BREAKING;
    public static ForgeConfigSpec.BooleanValue ENABLE_LADDER_FALLBACK;
    public static ForgeConfigSpec.BooleanValue ENABLE_PRECISE_FOLIAGE_LOS;
    public static ForgeConfigSpec.BooleanValue ENABLE_HAZARDOUS_COVER_REJECTION;
    public static ForgeConfigSpec.IntValue AI_LOS_CACHE_TICKS;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("tactical_ai_phase_1");

        ENABLE_SOFT_OBSTACLE_BREAKING = builder
                .comment("Allow NPCs to break navigation clutter such as grass, leaves and glass while following a path.")
                .define("enableSoftObstacleBreaking", true);

        ENABLE_LADDER_FALLBACK = builder
                .comment("Allow NPCs to use a ladder-aware fallback route when vanilla navigation cannot resolve vertical movement.")
                .define("enableLadderFallback", true);

        ENABLE_PRECISE_FOLIAGE_LOS = builder
                .comment("Use tactical line-of-sight checks so foliage, bushes, glass and other soft visual clutter do not block target acquisition or firing.")
                .define("enablePreciseFoliageLos", true);

        ENABLE_HAZARDOUS_COVER_REJECTION = builder
                .comment("Reject tactical movement and cover positions that require standing in dangerous blocks such as lava, fire, cactus or magma.")
                .define("enableHazardousCoverRejection", true);

        AI_LOS_CACHE_TICKS = builder
                .comment("Minimum ticks between repeated line-of-sight checks against the same target.")
                .defineInRange("aiLosCacheTicks", 4, 1, 40);

        builder.pop();
    }

    public static boolean enableSoftObstacleBreaking() {
        return ENABLE_SOFT_OBSTACLE_BREAKING == null || ENABLE_SOFT_OBSTACLE_BREAKING.get();
    }

    public static boolean enableLadderFallback() {
        return ENABLE_LADDER_FALLBACK == null || ENABLE_LADDER_FALLBACK.get();
    }

    public static boolean enablePreciseFoliageLos() {
        return ENABLE_PRECISE_FOLIAGE_LOS == null || ENABLE_PRECISE_FOLIAGE_LOS.get();
    }

    public static boolean enableHazardousCoverRejection() {
        return ENABLE_HAZARDOUS_COVER_REJECTION == null || ENABLE_HAZARDOUS_COVER_REJECTION.get();
    }

    public static int aiLosCacheTicks() {
        return AI_LOS_CACHE_TICKS == null ? 4 : AI_LOS_CACHE_TICKS.get();
    }
}
