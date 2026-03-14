package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class EffectClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory effect = builder.getOrCreateCategory(Component.literal("Visual Effects"));

        effect.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Player Suppression"), CommonConfig.ENABLE_SUPPRESSION.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Activate or deactivate the visual suppression effect"))
                .setSaveConsumer(newValue -> CommonConfig.ENABLE_SUPPRESSION.set(newValue))
                .build());
    }

}
