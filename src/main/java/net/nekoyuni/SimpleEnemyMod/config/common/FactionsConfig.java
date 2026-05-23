package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class FactionsConfig {

    public static ForgeConfigSpec.ConfigValue<Boolean> RU_UNITS_FRIENDLY;
    public static ForgeConfigSpec.ConfigValue<Boolean> US_UNITS_FRIENDLY;

    public static void init (ForgeConfigSpec.Builder builder) {
        builder.push("Factions");

        RU_UNITS_FRIENDLY = builder.comment("If true, Ru Units will be friendly with Players and PMC Units")
                .define("ruUnitsFriendly", false);

        US_UNITS_FRIENDLY = builder.comment("If true, Us Units will be friendly with Players and PMC Units")
                .define("usUnitsFriendly", false);

        builder.pop();

    }
}
