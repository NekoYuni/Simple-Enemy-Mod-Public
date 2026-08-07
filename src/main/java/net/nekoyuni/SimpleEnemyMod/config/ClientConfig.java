package net.nekoyuni.SimpleEnemyMod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.IntValue RENDER_DISTANCE;
    public static ForgeConfigSpec.BooleanValue RENDER_MUZZLE_FLASH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("unit_render_distance");

        RENDER_DISTANCE = builder
                .comment(
                        "Max Render Distance (in Blocks) for Units",
                        "Important!!: Render Distance => Detection Range"
                )
                .defineInRange("renderDistance", 128,32,384);

        RENDER_MUZZLE_FLASH = builder
                .comment("Activate or deactivate the visual Muzzle Flash effect")
                .define("enableMuzzleFlash", true);

        builder.pop();

        SPEC = builder.build();
    }

}
