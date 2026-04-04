package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class SpawnClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory spawns = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.spawns"));

        spawns.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.spawns.enable_village_garrison"),
                        CommonConfig.ENABLE_VILLAGE_GARRISON_CONFIG.get())
                .setDefaultValue(true)
                .setTooltip(
                        Component.translatable("config.simpleenemymod.spawns.enable_village_garrison.tooltip.1"),
                        Component.translatable("config.simpleenemymod.spawns.enable_village_garrison.tooltip.2")
                )
                .setSaveConsumer(newValue -> CommonConfig.ENABLE_VILLAGE_GARRISON_CONFIG.set(newValue))
                .build());




    }


}
