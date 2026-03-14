package net.nekoyuni.SimpleEnemyMod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;

@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {

    public static final ForgeConfigSpec SPEC;

    // Drops
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_DROPS;
    public static final ForgeConfigSpec.DoubleValue GUN_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue AMMO_DROP_CHANCE;

    // Factions
    public static final ForgeConfigSpec.ConfigValue<Boolean> RU_UNITS_FRIENDLY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> US_UNITS_FRIENDLY;

    // Village Garrison
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_VILLAGE_GARRISON_CONFIG;
    public static boolean enableVillageGarrison = true;

    // Difficulty
    public static final ForgeConfigSpec.EnumValue<CommonConfig.AIDifficulty> DIFFICULTY;
    public enum AIDifficulty {
        NORMAL, ADVANCED
    }

    // Suppression
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION;

    // Attributes
    public static ForgeConfigSpec.DoubleValue UNIT_HEALTH;
    public static ForgeConfigSpec.DoubleValue UNIT_SPEED;
    public static ForgeConfigSpec.DoubleValue UNIT_DETECTION_RANGE;

    // Shooting
    public static ForgeConfigSpec.DoubleValue MAX_SHOOT_DISTANCE;
    public static ForgeConfigSpec.DoubleValue BASE_SPREAD;
    public static ForgeConfigSpec.DoubleValue SPREAD_INCREASE;

    public static ForgeConfigSpec.IntValue MIN_BURST;
    public static ForgeConfigSpec.IntValue MAX_BURST;

    public static ForgeConfigSpec.IntValue MIN_BURST_COOLDOWN;
    public static ForgeConfigSpec.IntValue MAX_BURST_COOLDOWN;

    // Procedural Events
    public static ForgeConfigSpec.DoubleValue PATROL_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue PATROL_FAILURE_MULTIPLIER;

    public static ForgeConfigSpec.DoubleValue COMBAT_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue COMBAT_FAILURE_MULTIPLIER;

    public static ForgeConfigSpec.DoubleValue CAVE_BASE_CHANCE;
    public static ForgeConfigSpec.DoubleValue CAVE_FAILURE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general_settings");

        ENABLE_VILLAGE_GARRISON_CONFIG = builder
                .comment("When set to true, soldiers will spawn in villages. ",
                        "When set to false, the event is disabled")
                .define("enableVillageGarrison", true);
        builder.pop();


        builder.push("Factions");

        RU_UNITS_FRIENDLY = builder.comment("If true, Ru Units will be friendly with Players and PMC Units")
                .define("ruUnitsFriendly", false);

        US_UNITS_FRIENDLY = builder.comment("If true, Us Units will be friendly with Players and PMC Units")
                .define("usUnitsFriendly", false);

        builder.pop();


        builder.push("ai_difficulty");
        DIFFICULTY = builder
                .comment(
                        "AI Difficulty preset.",
                        "Does NOT affect soldiers accuracy.",
                        "Only affects tactical maneuver timers and movement speeds.",
                        "",
                        "NORMAL: More static soldiers, slower combat pacing.",
                        "ADVANCED: More dynamic and aggressive soldiers."
                )
                .defineEnum("difficulty", CommonConfig.AIDifficulty.NORMAL);

        builder.pop();


        builder.push("unit_attributes");

        UNIT_HEALTH = builder
                .comment("Max Health for All Units")
                .defineInRange("health", 20.0, 1.0, 200.0);
        builder.comment("--------------------------------------------------");

        UNIT_SPEED = builder
                .comment("Walk Speed for All Units")
                .defineInRange("speed", 0.27, 0.05, 1.5);
        builder.comment("--------------------------------------------------");

        UNIT_DETECTION_RANGE = builder
                .comment("Detection Range for All Units")
                .defineInRange("detectionRange", 96.00, 32.00, 192.00);

        builder.pop();


        builder.push("gun_ai_shooting_config");
        builder.comment("--------------------------------------------------");


        MAX_SHOOT_DISTANCE = builder
                .comment("Maximum shooting distance")
                .defineInRange("maxShootDistance", 90.0, 10.0, 200.0);
        builder.comment("--------------------------------------------------");

        BASE_SPREAD = builder
                .comment("Base bullet spread (lower = more accurate)")
                .defineInRange("baseSpread", 1.4, 0.0, 10.0);
        builder.comment("--------------------------------------------------");

        SPREAD_INCREASE = builder
                .comment("Spread increase per block of distance to target (accuracy penalty at range)")
                .defineInRange("spreadIncrease", 0.012, 0.0, 1.0);
        builder.comment("--------------------------------------------------");

        MIN_BURST = builder
                .comment("Minimum shots per burst")
                .defineInRange("minBurst", 3, 1, 20);
        builder.comment("--------------------------------------------------");

        MAX_BURST = builder
                .comment("Maximum shots per burst")
                .defineInRange("maxBurst", 5, 1, 30);
        builder.comment("--------------------------------------------------");

        MIN_BURST_COOLDOWN = builder
                .comment("Minimum cooldown between bursts (ticks)")
                .defineInRange("minBurstCooldown", 10, 0, 200);
        builder.comment("--------------------------------------------------");

        MAX_BURST_COOLDOWN = builder
                .comment("Maximum cooldown between bursts (ticks)")
                .defineInRange("maxBurstCooldown", 15, 0, 400);

        builder.pop();


        builder.push("event_spawn");
        builder.comment("--------------------------------------------------");

        PATROL_BASE_CHANCE = builder
                .comment("Base spawn chance for Patrol Event (per minute)")
                .defineInRange("patrolBaseChance", 0.10, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        PATROL_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Patrol Event")
                .defineInRange("patrolFailureMultiplier", 0.15, 0.01, 0.5);
        builder.comment("--------------------------------------------------");

        COMBAT_BASE_CHANCE = builder
                .comment("Base spawn chance for Combat Event (per minute)")
                .defineInRange("combatBaseChance", 0.06, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        COMBAT_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Combat Event")
                .defineInRange("combatFailureMultiplier", 0.12, 0.01, 0.5);
        builder.comment("--------------------------------------------------");

        CAVE_BASE_CHANCE = builder
                .comment("Base spawn chance for Cave Extraction Event (per minute)")
                .defineInRange("caveBaseChance", 0.08, 0.01, 1.0);
        builder.comment("--------------------------------------------------");

        CAVE_FAILURE_MULTIPLIER = builder
                .comment("Chance accumulation rate on failure for Cave Extraction Event")
                .defineInRange("caveFailureMultiplier", 0.20, 0.01, 0.5);

        builder.pop();


        builder.push("Drops");

        ENABLE_CUSTOM_DROPS = builder
                .comment("Enables or disables modified weapon drops for Units.")
                .define("enableCustomDrops", true);

        GUN_DROP_CHANCE = builder
                .comment("Probability that a Unit will drop its TACZ weapon upon death (0.0 to 1.0)")
                .defineInRange("gunDropChance", 1.0, 0.0, 1.0);

        AMMO_DROP_CHANCE = builder
                .comment("Probability that a Unit will drop extra ammo upon death (0.0 to 1.0).")
                .defineInRange("ammoDropChance", 0.5, 0.0, 1.0);

        builder.pop();

        builder.push("Visual Effects");
        ENABLE_SUPPRESSION = builder
                .comment("Activate or deactivate the visual suppression effect")
                .define("enableSuppression", true);

        builder.pop();


        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            enableVillageGarrison = ENABLE_VILLAGE_GARRISON_CONFIG.get();
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            enableVillageGarrison = ENABLE_VILLAGE_GARRISON_CONFIG.get();
        }
    }



}