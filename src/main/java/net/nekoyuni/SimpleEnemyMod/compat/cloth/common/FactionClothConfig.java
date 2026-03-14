package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class FactionClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory faction = builder.getOrCreateCategory(Component.literal("Factions"));

        faction.addEntry(entryBuilder.startBooleanToggle(Component.literal("RU Units Friendly"), CommonConfig.RU_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, Ru Units will be friendly with Players and PMC Units"))
                .setSaveConsumer(newValue -> CommonConfig.RU_UNITS_FRIENDLY.set(newValue))
                .build());

        faction.addEntry(entryBuilder.startBooleanToggle(Component.literal("US Units Friendly"), CommonConfig.US_UNITS_FRIENDLY.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, Us Units will be friendly with Players and PMC Units"))
                .setSaveConsumer(newValue -> CommonConfig.US_UNITS_FRIENDLY.set(newValue))
                .build());

    }
}
