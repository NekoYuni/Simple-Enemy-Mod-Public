package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.FactionsConfig;

public class FactionClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory faction = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.factions"));

        faction.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.factions.ru_units_friendly"),
                        FactionsConfig.RU_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.factions.ru_units_friendly.tooltip"))
                .setSaveConsumer(newValue -> FactionsConfig.RU_UNITS_FRIENDLY.set(newValue))
                .build());

        faction.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.factions.us_units_friendly"),
                        FactionsConfig.US_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.factions.us_units_friendly.tooltip"))
                .setSaveConsumer(newValue -> FactionsConfig.US_UNITS_FRIENDLY.set(newValue))
                .build());

    }
}
