package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class AiShootingClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory shooting = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.ai_shooting"));


        shooting.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.ai_shooting.max_shoot_distance"),
                        CommonConfig.MAX_SHOOT_DISTANCE.get())
                .setDefaultValue(90.0)
                .setMin(10.0)
                .setMax(200.0)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.max_shoot_distance.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_SHOOT_DISTANCE.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.ai_shooting.base_spread"),
                        CommonConfig.BASE_SPREAD.get())
                .setDefaultValue(1.4)
                .setMin(0.0)
                .setMax(10.0)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.base_spread.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.BASE_SPREAD.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.ai_shooting.spread_increase"),
                        CommonConfig.SPREAD_INCREASE.get())
                .setDefaultValue(0.012)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.spread_increase.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.SPREAD_INCREASE.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.simpleenemymod.ai_shooting.min_burst"),
                        CommonConfig.MIN_BURST.get(), 1, 20)
                .setDefaultValue(3)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.min_burst.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.MIN_BURST.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.simpleenemymod.ai_shooting.max_burst"),
                        CommonConfig.MAX_BURST.get(), 1, 30)
                .setDefaultValue(5)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.max_burst.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_BURST.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.simpleenemymod.ai_shooting.min_burst_cooldown"),
                        CommonConfig.MIN_BURST_COOLDOWN.get(), 0, 200)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.min_burst_cooldown.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.MIN_BURST_COOLDOWN.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.simpleenemymod.ai_shooting.max_burst_cooldown"),
                        CommonConfig.MAX_BURST_COOLDOWN.get(), 0, 400)
                .setDefaultValue(15)
                .setTooltip(Component.translatable("config.simpleenemymod.ai_shooting.max_burst_cooldown.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_BURST_COOLDOWN.set(newValue))
                .build());

    }

}
