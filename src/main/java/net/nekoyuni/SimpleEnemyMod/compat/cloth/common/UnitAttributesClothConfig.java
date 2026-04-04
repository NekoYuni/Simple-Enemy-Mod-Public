package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class UnitAttributesClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory attributes = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.unit_attributes"));

        attributes.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.unit_attributes.max_health"),
                        CommonConfig.UNIT_HEALTH.get())
                .setDefaultValue(20.0)
                .setMin(1.0)
                .setMax(200.0)
                .setTooltip(Component.translatable("config.simpleenemymod.unit_attributes.max_health.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.UNIT_HEALTH.set(newValue))
                .build());

        attributes.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.unit_attributes.walk_speed"),
                        CommonConfig.UNIT_SPEED.get())
                .setDefaultValue(0.27)
                .setMin(0.05)
                .setMax(1.5)
                .setTooltip(Component.translatable("config.simpleenemymod.unit_attributes.walk_speed.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.UNIT_SPEED.set(newValue))
                .build());

        attributes.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.unit_attributes.detection_range"),
                        CommonConfig.UNIT_DETECTION_RANGE.get())
                .setDefaultValue(96.0)
                .setMin(32.0)
                .setMax(192.0)
                .setTooltip(Component.translatable("config.simpleenemymod.unit_attributes.detection_range.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.UNIT_DETECTION_RANGE.set(newValue))
                .build());

    }
}
