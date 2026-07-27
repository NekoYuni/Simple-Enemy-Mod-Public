package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;

public class AiTacticalClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory tactical = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.tactical"));

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_soft_obstacle_breaking"),
                        TacticalAiConfig.ENABLE_SOFT_OBSTACLE_BREAKING.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_soft_obstacle_breaking.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_SOFT_OBSTACLE_BREAKING.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_precise_foliage_los"),
                        TacticalAiConfig.ENABLE_PRECISE_FOLIAGE_LOS.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_precise_foliage_los.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_PRECISE_FOLIAGE_LOS.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_tactical_hearing"),
                        TacticalAiConfig.ENABLE_TACTICAL_HEARING.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_tactical_hearing.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_TACTICAL_HEARING.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_smart_cover_search"),
                        TacticalAiConfig.ENABLE_SMART_COVER_SEARCH.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_smart_cover_search.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_SMART_COVER_SEARCH.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_squad_combat_memory"),
                        TacticalAiConfig.ENABLE_SQUAD_COMBAT_MEMORY.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_squad_combat_memory.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_SQUAD_COMBAT_MEMORY.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_tactical_suppression"),
                        TacticalAiConfig.ENABLE_TACTICAL_SUPPRESSION.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_tactical_suppression.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_TACTICAL_SUPPRESSION.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.tactical.suppression_accuracy_spread_penalty"),
                        TacticalAiConfig.SUPPRESSION_ACCURACY_SPREAD_PENALTY.get())
                .setDefaultValue(2.0D)
                .setMin(0.0D)
                .setMax(20.0D)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.suppression_accuracy_spread_penalty.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.SUPPRESSION_ACCURACY_SPREAD_PENALTY.set(newValue))
                .build());

        tactical.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.tactical.enable_distance_target_priority"),
                        TacticalAiConfig.ENABLE_DISTANCE_TARGET_PRIORITY.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.tactical.enable_distance_target_priority.tooltip"))
                .setSaveConsumer(newValue -> TacticalAiConfig.ENABLE_DISTANCE_TARGET_PRIORITY.set(newValue))
                .build());

    }
}
