package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.common.EventSpawnConfig;

public class SpawnEventClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory spawnEvents = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.spawn_event"));

        // EVENT GLOBAL COOLDOWN TICKS
        spawnEvents.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.simpleenemymod.spawn_event.global_cooldown_ticks"),
                        EventSpawnConfig.EVENT_GLOBAL_COOLDOWN_TICKS.get())
                .setDefaultValue(4800)
                .setMin(100)
                .setMax(72000)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.global_cooldown_ticks.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.EVENT_GLOBAL_COOLDOWN_TICKS.set(newValue))
                .build());

        // EVENT TICK WINDOW
        spawnEvents.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.simpleenemymod.spawn_event.event_tick_window"),
                        EventSpawnConfig.EVENT_TICK_WINDOW.get())
                .setDefaultValue(1200)
                .setMin(100)
                .setMax(72000)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.event_tick_window.tooltip"))
                .setSaveConsumer( newValue -> EventSpawnConfig.EVENT_TICK_WINDOW.set(newValue))
                .build());

        // PATROL EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.patrol_base_chance"),
                        EventSpawnConfig.PATROL_BASE_CHANCE.get())
                .setDefaultValue(0.10)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.patrol_base_chance.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.PATROL_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.patrol_failure_multiplier"),
                        EventSpawnConfig.PATROL_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.15)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.patrol_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.PATROL_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // COMBAT EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.combat_base_chance"),
                        EventSpawnConfig.COMBAT_BASE_CHANCE.get())
                .setDefaultValue(0.08)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.combat_base_chance.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.COMBAT_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.combat_failure_multiplier"),
                        EventSpawnConfig.COMBAT_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.12)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.combat_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.COMBAT_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // CAVE EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.cave_base_chance"),
                        EventSpawnConfig.CAVE_BASE_CHANCE.get())
                .setDefaultValue(0.02)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.cave_base_chance.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.CAVE_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.spawn_event.cave_failure_multiplier"),
                        EventSpawnConfig.CAVE_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.20)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.simpleenemymod.spawn_event.cave_failure_multiplier.tooltip"))
                .setSaveConsumer(newValue -> EventSpawnConfig.CAVE_FAILURE_MULTIPLIER.set(newValue))
                .build());

    }

}
