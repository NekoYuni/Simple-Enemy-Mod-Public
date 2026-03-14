package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class SpawnClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory spawns = builder.getOrCreateCategory(Component.literal("Spawns"));

        spawns.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Enable Village Garrison"),
                        CommonConfig.ENABLE_VILLAGE_GARRISON_CONFIG.get())
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("When set to true, soldiers will spawn in villages."),
                        Component.literal("When set to false, the event is disabled")
                )
                .setSaveConsumer(newValue -> CommonConfig.ENABLE_VILLAGE_GARRISON_CONFIG.set(newValue))
                .build());




    }


}
