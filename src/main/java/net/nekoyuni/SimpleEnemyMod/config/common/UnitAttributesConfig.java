package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class UnitAttributesConfig {

    public static ForgeConfigSpec.DoubleValue UNIT_HEALTH;
    public static ForgeConfigSpec.DoubleValue UNIT_SPEED;
    public static ForgeConfigSpec.DoubleValue UNIT_DETECTION_RANGE;

    public static void init(ForgeConfigSpec.Builder builder){
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

    }

}
