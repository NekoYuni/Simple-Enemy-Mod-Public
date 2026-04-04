package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;


public class AiDifficultyClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory difficulty = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.difficulty"));

        difficulty.addEntry(entryBuilder.startTextDescription(
                Component.translatable("config.simpleenemymod.difficulty.description")
                        .withStyle(ChatFormatting.GRAY)
        ).build());

        difficulty.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("config.simpleenemymod.difficulty"),
                        CommonConfig.AIDifficulty.class,
                        CommonConfig.DIFFICULTY.get())
                .setDefaultValue(CommonConfig.AIDifficulty.NORMAL)
                .setSaveConsumer(newValue -> CommonConfig.DIFFICULTY.set(newValue))
                .build());

    }

}
