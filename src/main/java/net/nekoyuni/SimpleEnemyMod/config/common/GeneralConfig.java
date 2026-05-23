package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GeneralConfig {

    public static ForgeConfigSpec.ConfigValue<Boolean> ENABLE_VILLAGE_GARRISON_CONFIG;
    public static boolean enableVillageGarrison = true;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("general_settings");

        ENABLE_VILLAGE_GARRISON_CONFIG = builder
                .comment("When set to true, soldiers will spawn in villages. When set to false, the event is disabled")
                .define("enableVillageGarrison", true);

        builder.pop();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) {
            enableVillageGarrison = ENABLE_VILLAGE_GARRISON_CONFIG.get();
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) {
            enableVillageGarrison = ENABLE_VILLAGE_GARRISON_CONFIG.get();
        }
    }
}


