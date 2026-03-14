package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class SpawnEventClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory spawnEvents = builder.getOrCreateCategory(Component.literal("Spawn Events"));

        // PATROL EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Patrol Base Chance"), CommonConfig.PATROL_BASE_CHANCE.get())
                .setDefaultValue(0.10)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.literal("Base spawn chance for Patrol Event (per minute)"))
                .setSaveConsumer(newValue -> CommonConfig.PATROL_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Patrol Failure Multiplier"), CommonConfig.PATROL_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.15)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.literal("Chance accumulation rate on failure for Patrol Event"))
                .setSaveConsumer(newValue -> CommonConfig.PATROL_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // COMBAT EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Combat Base Chance"), CommonConfig.COMBAT_BASE_CHANCE.get())
                .setDefaultValue(0.06)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.literal("Base spawn chance for Combat Event (per minute)"))
                .setSaveConsumer(newValue -> CommonConfig.COMBAT_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Combat Failure Multiplier"), CommonConfig.COMBAT_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.12)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.literal("Chance accumulation rate on failure for Combat Event"))
                .setSaveConsumer(newValue -> CommonConfig.COMBAT_FAILURE_MULTIPLIER.set(newValue))
                .build());

        // CAVE EXTRACTION EVENT
        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Cave Base Chance"), CommonConfig.CAVE_BASE_CHANCE.get())
                .setDefaultValue(0.08)
                .setMin(0.01)
                .setMax(1.0)
                .setTooltip(Component.literal("Base spawn chance for Cave Extraction Event (per minute)"))
                .setSaveConsumer(newValue -> CommonConfig.CAVE_BASE_CHANCE.set(newValue))
                .build());

        spawnEvents.addEntry(entryBuilder.startDoubleField(Component.literal("Cave Failure Multiplier"), CommonConfig.CAVE_FAILURE_MULTIPLIER.get())
                .setDefaultValue(0.20)
                .setMin(0.01)
                .setMax(0.5)
                .setTooltip(Component.literal("Chance accumulation rate on failure for Cave Extraction Event"))
                .setSaveConsumer(newValue -> CommonConfig.CAVE_FAILURE_MULTIPLIER.set(newValue))
                .build());
    }

}
