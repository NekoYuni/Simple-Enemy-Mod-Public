package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class FactionClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory faction = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.factions"));

        faction.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.factions.ru_units_friendly"),
                        CommonConfig.RU_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.factions.ru_units_friendly.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.RU_UNITS_FRIENDLY.set(newValue))
                .build());

        faction.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.factions.us_units_friendly"),
                        CommonConfig.US_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.factions.us_units_friendly.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.US_UNITS_FRIENDLY.set(newValue))
                .build());

    }
}
