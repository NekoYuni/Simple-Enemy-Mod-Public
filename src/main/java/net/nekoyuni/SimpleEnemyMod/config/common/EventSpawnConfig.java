package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class EventSpawnConfig {

    public static ForgeConfigSpec.DoubleValue PATROL_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue PATROL_FAILURE_MULTIPLIER;

    public static ForgeConfigSpec.DoubleValue COMBAT_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue COMBAT_FAILURE_MULTIPLIER;

    public static ForgeConfigSpec.DoubleValue CAVE_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue CAVE_FAILURE_MULTIPLIER;

    public static ForgeConfigSpec.IntValue EVENT_GLOBAL_COOLDOWN_TICKS;
    public static ForgeConfigSpec.IntValue EVENT_TICK_WINDOW;

    public static ForgeConfigSpec.IntValue PATROL_MIN_SIZE;
    public static ForgeConfigSpec.IntValue PATROL_MAX_SIZE;

    public static ForgeConfigSpec.IntValue COMBAT_MIN_SIZE;
    public static ForgeConfigSpec.IntValue COMBAT_MAX_SIZE;

    public static ForgeConfigSpec.IntValue CAVE_MIN_SIZE;
    public static ForgeConfigSpec.IntValue CAVE_MAX_SIZE;

    public static void init(ForgeConfigSpec.Builder builder){

        builder.push("event_spawn");
        builder.comment("--------------------------------------------------");

        EVENT_GLOBAL_COOLDOWN_TICKS = builder
                .comment("Global cooldown ticks between events. 20 ticks = 1 second. 4800 = 4 minutes")
                .defineInRange("eventGlobalCooldownTicks", 4800, 100, 72000);

        EVENT_TICK_WINDOW = builder
                .comment("Ticks between event evaluation attempts per player. 20 ticks = 1 second. Default: 3600 = 1 minute")
                .defineInRange("eventTickWindow", 1200, 100, 72000);

        // PATROL EVENT
        PATROL_BASE_CHANCE = builder
                .comment("Base spawn chance for Patrol Event (per minute)")
                .defineInRange("patrolBaseChance", 0.10, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        PATROL_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Patrol Event")
                .defineInRange("patrolFailureMultiplier", 0.15, 0.01, 0.5);
        builder.comment("--------------------------------------------------");

        PATROL_MIN_SIZE = builder
                .comment("Minimum squad size for Patrol Event")
                .defineInRange("patrolMinSize", 3, 1, 10);

        PATROL_MAX_SIZE = builder
                .comment("Maximum squad size for Patrol Event")
                .defineInRange("patrolMaxSize", 4, 1, 20);


        // COMBAT CONFIG
        COMBAT_BASE_CHANCE = builder
                .comment("Base spawn chance for Combat Event (per minute)")
                .defineInRange("combatBaseChance", 0.08, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        COMBAT_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Combat Event")
                .defineInRange("combatFailureMultiplier", 0.12, 0.01, 0.5);
        builder.comment("--------------------------------------------------");

        COMBAT_MIN_SIZE = builder
                .comment("Minimum squad size for Combat Event (per faction)")
                .defineInRange("combatMinSize", 3, 1, 10);

        COMBAT_MAX_SIZE = builder
                .comment("Maximum squad size for Combat Event (per faction)")
                .defineInRange("combatMaxSize", 5, 1, 20);


        // CAVE EVENT
        CAVE_BASE_CHANCE = builder
                .comment("Base spawn chance for Cave Extraction Event (per minute)")
                .defineInRange("caveBaseChance", 0.02, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        CAVE_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Cave Extraction Event")
                .defineInRange("caveFailureMultiplier", 0.20, 0.01, 0.5);
        builder.comment("--------------------------------------------------");

        CAVE_MIN_SIZE = builder
                .comment("Minimum squad size for Cave Extraction Event")
                .defineInRange("caveMinSize", 2, 1, 10);
        builder.comment("--------------------------------------------------");

        CAVE_MAX_SIZE = builder
                .comment("Maximum squad size for Cave Extraction Event")
                .defineInRange("caveMaxSize", 4, 1, 20);

        builder.pop();

    }

}
