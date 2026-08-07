package net.nekoyuni.SimpleEnemyMod.compat.cloth.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.ClientConfig;

public class RenderClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder){
        ConfigCategory render = builder.getOrCreateCategory(Component.translatable("config.simpleenemymod.cat.render_distance"));

        render.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.simpleenemymod.render_muzzle_flash"),
                        ClientConfig.RENDER_MUZZLE_FLASH.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.simpleenemymod.render_muzzle_flash.tooltip"))
                .setSaveConsumer(newValue -> ClientConfig.RENDER_MUZZLE_FLASH.set(newValue))
                .build());

        render.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.simpleenemymod.render_distance"), ClientConfig.RENDER_DISTANCE.get(), 32, 384)
                .setDefaultValue(128)
                .setTooltip(
                        Component.translatable("config.simpleenemymod.render_distance.tooltip.1"),
                        Component.translatable("config.simpleenemymod.render_distance.tooltip.2")
                )
                .setSaveConsumer(newValue -> ClientConfig.RENDER_DISTANCE.set(newValue))
                .build());




    }

}
