package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.AiDifficultyConfig;


public class AiDifficultyClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory difficulty = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.difficulty"));

        difficulty.addEntry(entryBuilder.startTextDescription(
                Component.translatable("config.simpleenemymod.difficulty.description")
                        .withStyle(ChatFormatting.GRAY)
        ).build());

        difficulty.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("config.simpleenemymod.difficulty"),
                        AiDifficultyConfig.AIDifficulty.class,
                        AiDifficultyConfig.DIFFICULTY.get())
                .setDefaultValue(AiDifficultyConfig.AIDifficulty.NORMAL)
                .setSaveConsumer(newValue -> AiDifficultyConfig.DIFFICULTY.set(newValue))
                .build());

    }

}
