package net.nekoyuni.SimpleEnemyMod.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ModConfigs {

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "sem-client.toml");
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "sem-common.toml");
    }
}
