package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class DropsConfig {

    public static ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_DROPS;
    public static ForgeConfigSpec.DoubleValue GUN_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue AMMO_DROP_CHANCE;

    public static void init(ForgeConfigSpec.Builder builder){
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

    }
}
