package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;


public class AiDifficultyClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory difficulty = builder.getOrCreateCategory(Component.literal("Difficulty"));

        difficulty.addEntry(entryBuilder.startTextDescription(
                Component.literal("AI Difficulty preset.\nDoes NOT affect soldiers accuracy. " +
                                "Only affects tactical maneuver timers and movement speeds.\n\nNORMAL:" +
                                " More static soldiers, slower combat pacing.\nADVANCED: More dynamic and aggressive soldiers.")
                        .withStyle(ChatFormatting.GRAY)
        ).build());

        difficulty.addEntry(entryBuilder.startEnumSelector(
                        Component.literal("AI Difficulty"),
                        CommonConfig.AIDifficulty.class,
                        CommonConfig.DIFFICULTY.get())
                .setDefaultValue(CommonConfig.AIDifficulty.NORMAL)
                .setSaveConsumer(newValue -> CommonConfig.DIFFICULTY.set(newValue)) // ESCRITURA
                .build());

    }

}
