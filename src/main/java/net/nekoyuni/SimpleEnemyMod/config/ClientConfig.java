package net.nekoyuni.SimpleEnemyMod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.IntValue RENDER_DISTANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("unit_render_distance");

        RENDER_DISTANCE = builder
                .comment(
                        "Max Render Distance (in Blocks) for Units",
                        "Important!!: Render Distance => Detection Range"
                )
                .defineInRange("renderDistance", 128,32,192);

        builder.pop();

        SPEC = builder.build();
    }

}
