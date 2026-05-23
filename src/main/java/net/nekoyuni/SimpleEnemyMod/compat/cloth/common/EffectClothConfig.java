package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.EffectConfig;

public class EffectClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory effect = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.visual_effects"));

        effect.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.visual_effects.enable_suppression"),
                        EffectConfig.ENABLE_SUPPRESSION.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.visual_effects.enable_suppression.tooltip"))
                .setSaveConsumer(newValue -> EffectConfig.ENABLE_SUPPRESSION.set(newValue))
                .build());
    }

}
