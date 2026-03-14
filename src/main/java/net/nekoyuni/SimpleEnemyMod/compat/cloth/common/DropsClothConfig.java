package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;


import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

public class DropsClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory drop = builder.getOrCreateCategory(Component.literal("Drops"));

        drop.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Custom Drops"), CommonConfig.ENABLE_CUSTOM_DROPS.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Enables or disables modified weapon drops for Units."))
                .setSaveConsumer(newValue -> CommonConfig.ENABLE_CUSTOM_DROPS.set(newValue))
                .build());

        drop.addEntry(entryBuilder.startDoubleField(Component.literal("Gun Drop Chance"), CommonConfig.GUN_DROP_CHANCE.get())
                .setDefaultValue(1.0)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.literal("Probability that a Unit will drop its TACZ weapon upon death (0.0 to 1.0)"))
                .setSaveConsumer(newValue -> CommonConfig.GUN_DROP_CHANCE.set(newValue))
                .build());

        drop.addEntry(entryBuilder.startDoubleField(Component.literal("Ammo Drop Chance"), CommonConfig.AMMO_DROP_CHANCE.get())
                .setDefaultValue(0.5)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.literal("Probability that a Unit will drop extra ammo upon death (0.0 to 1.0)."))
                .setSaveConsumer(newValue -> CommonConfig.AMMO_DROP_CHANCE.set(newValue))
                .build());

    }

}
