package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class AiShootingConfig {

    public static ForgeConfigSpec.DoubleValue MAX_SHOOT_DISTANCE;
    public static ForgeConfigSpec.DoubleValue BASE_SPREAD;
    public static ForgeConfigSpec.DoubleValue SPREAD_INCREASE;
    public static ForgeConfigSpec.IntValue MIN_BURST;
    public static ForgeConfigSpec.IntValue MAX_BURST;
    public static ForgeConfigSpec.IntValue MIN_BURST_COOLDOWN;
    public static ForgeConfigSpec.IntValue MAX_BURST_COOLDOWN;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("gun_ai_shooting_config");

        MAX_SHOOT_DISTANCE = builder
                .comment("Maximum shooting distance")
                .defineInRange("maxShootDistance", 90.0, 10.0, 200.0);

        BASE_SPREAD = builder
                .comment("Base bullet spread (lower = more accurate)")
                .defineInRange("baseSpread", 1.4, 0.0, 10.0);

        SPREAD_INCREASE = builder
                .comment("Spread increase per block of distance to target (accuracy penalty at range)")
                .defineInRange("spreadIncrease", 0.012, 0.0, 1.0);

        MIN_BURST = builder
                .comment("Minimum shots per burst")
                .defineInRange("minBurst", 3, 1, 20);

        MAX_BURST = builder
                .comment("Maximum shots per burst")
                .defineInRange("maxBurst", 5, 1, 30);

        MIN_BURST_COOLDOWN = builder
                .comment("Minimum cooldown between bursts (ticks)")
                .defineInRange("minBurstCooldown", 10, 0, 200);

        MAX_BURST_COOLDOWN = builder
                .comment("Maximum cooldown between bursts (ticks)")
                .defineInRange("maxBurstCooldown", 15, 0, 400);

        builder.pop();
    }
}
