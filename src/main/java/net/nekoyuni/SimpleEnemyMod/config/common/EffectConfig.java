package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class EffectConfig {

    public static ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION;

    public static void init (ForgeConfigSpec.Builder builder) {
        builder.push("Visual Effects");

        ENABLE_SUPPRESSION = builder
                .comment("Activate or deactivate the visual suppression effect")
                .define("enableSuppression", true);

        builder.pop();

    }

}
