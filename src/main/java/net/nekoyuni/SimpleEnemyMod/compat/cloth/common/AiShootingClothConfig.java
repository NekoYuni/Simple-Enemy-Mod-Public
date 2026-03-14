package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class AiShootingClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory shooting = builder.getOrCreateCategory(Component.literal("AI Shooting"));


        shooting.addEntry(entryBuilder.startDoubleField(Component.literal("Max Shoot Distance"), CommonConfig.MAX_SHOOT_DISTANCE.get())
                .setDefaultValue(90.0)
                .setMin(10.0)
                .setMax(200.0)
                .setTooltip(Component.literal("Maximum shooting distance"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_SHOOT_DISTANCE.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startDoubleField(Component.literal("Base Spread"), CommonConfig.BASE_SPREAD.get())
                .setDefaultValue(1.4)
                .setMin(0.0)
                .setMax(10.0)
                .setTooltip(Component.literal("Base bullet spread (lower = more accurate)"))
                .setSaveConsumer(newValue -> CommonConfig.BASE_SPREAD.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startDoubleField(Component.literal("Spread Increase"), CommonConfig.SPREAD_INCREASE.get())
                .setDefaultValue(0.012)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.literal("Spread increase per block of distance to target (accuracy penalty at range)"))
                .setSaveConsumer(newValue -> CommonConfig.SPREAD_INCREASE.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(Component.literal("Min Burst"), CommonConfig.MIN_BURST.get(), 1, 20)
                .setDefaultValue(3)
                .setTooltip(Component.literal("Minimum shots per burst"))
                .setSaveConsumer(newValue -> CommonConfig.MIN_BURST.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(Component.literal("Max Burst"), CommonConfig.MAX_BURST.get(), 1, 30)
                .setDefaultValue(5)
                .setTooltip(Component.literal("Maximum shots per burst"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_BURST.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(Component.literal("Min Burst Cooldown"), CommonConfig.MIN_BURST_COOLDOWN.get(), 0, 200)
                .setDefaultValue(10)
                .setTooltip(Component.literal("Minimum cooldown between bursts (ticks)"))
                .setSaveConsumer(newValue -> CommonConfig.MIN_BURST_COOLDOWN.set(newValue))
                .build());

        shooting.addEntry(entryBuilder.startIntSlider(Component.literal("Max Burst Cooldown"), CommonConfig.MAX_BURST_COOLDOWN.get(), 0, 400)
                .setDefaultValue(15)
                .setTooltip(Component.literal("Maximum cooldown between bursts (ticks)"))
                .setSaveConsumer(newValue -> CommonConfig.MAX_BURST_COOLDOWN.set(newValue))
                .build());

    }

}
