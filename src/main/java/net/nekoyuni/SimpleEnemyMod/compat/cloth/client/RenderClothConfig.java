package net.nekoyuni.SimpleEnemyMod.compat.cloth.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.config.ClientConfig;

public class RenderClothConfig {

    public static void setup(ConfigBuilder builder, ConfigEntryBuilder entryBuilder){
        ConfigCategory render = builder.getOrCreateCategory(Component.literal("Render"));

        render.addEntry(entryBuilder.startIntSlider(
                        Component.literal("Render Distance"), ClientConfig.RENDER_DISTANCE.get(), 32, 192)
                .setDefaultValue(128)
                .setTooltip(
                        Component.literal("Max Render Distance (in Blocks) for Units"),
                        Component.literal("Important!!: Render Distance > Detection Range")
                )
                .setSaveConsumer(newValue -> ClientConfig.RENDER_DISTANCE.set(newValue))
                .build());


    }

}
