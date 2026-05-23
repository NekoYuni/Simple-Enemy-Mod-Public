package net.nekoyuni.SimpleEnemyMod.compat.cloth.common;


import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.common.DropsConfig;

public class DropsClothConfig {

    public static void setup (ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {

        ConfigCategory drop = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.drops"));

        drop.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.drops.enable_custom_drops"),
                        DropsConfig.ENABLE_CUSTOM_DROPS.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.drops.enable_custom_drops.tooltip"))
                .setSaveConsumer(newValue -> DropsConfig.ENABLE_CUSTOM_DROPS.set(newValue))
                .build());

        drop.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.drops.gun_drop_chance"),
                        DropsConfig.GUN_DROP_CHANCE.get())
                .setDefaultValue(1.0)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.drops.gun_drop_chance.tooltip"))
                .setSaveConsumer(newValue -> DropsConfig.GUN_DROP_CHANCE.set(newValue))
                .build());

        drop.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.simpleenemymod.drops.ammo_drop_chance"),
                        DropsConfig.AMMO_DROP_CHANCE.get())
                .setDefaultValue(0.5)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.simpleenemymod.drops.ammo_drop_chance.tooltip"))
                .setSaveConsumer(newValue -> DropsConfig.AMMO_DROP_CHANCE.set(newValue))
                .build());

    }

}
