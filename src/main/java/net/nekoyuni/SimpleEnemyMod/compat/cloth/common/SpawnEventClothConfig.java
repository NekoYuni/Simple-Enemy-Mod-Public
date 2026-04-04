package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class SpawnEventClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory spawnEvents = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.spawn_event"));

        // PATROL EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.patrol_base_chance"),
                        CommonConfig.PATROL_BASE_CHANCE.get())
                .setDefaultValue(0.10)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.patrol_base_chance.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.PATROL_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.patrol_failure_multiplier"),
                        CommonConfig.PATROL_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.15)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.patrol_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.PATROL_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // COMBAT EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.combat_base_chance"),
                        CommonConfig.COMBAT_BASE_CHANCE.get())
                .setDefaultValue(0.06)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.combat_base_chance.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.COMBAT_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.combat_failure_multiplier"),
                        CommonConfig.COMBAT_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.12)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.combat_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.COMBAT_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // CAVE EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.cave_base_chance"),
                        CommonConfig.CAVE_BASE_CHANCE.get())
                .setDefaultValue(0.02)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.cave_base_chance.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.CAVE_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.cave_failure_multiplier"),
                        CommonConfig.CAVE_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.20)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.cave_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> CommonConfig.CAVE_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // TODO Add EVENT_GLOBAL_COOLDOWN_TICKS
    }

}
