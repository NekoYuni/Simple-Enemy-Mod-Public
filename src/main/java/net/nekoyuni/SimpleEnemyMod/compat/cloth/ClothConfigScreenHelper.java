package net.nekoyuni.SimpleEnemyMod.compat.cloth;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.nekoyuni.SimpleEnemyMod.compat.cloth.client.RenderClothConfig;
import net.nekoyuni.SimpleEnemyMod.compat.cloth.common.*;

public class ClothConfigScreenHelper {

    public static Screen createConfigScreen(Screen parentScreen) {

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(Component.literal("Simple Enemy Mod Config"));

        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // CONFIGS
        AiDifficultyClothConfig.setup(builder, entryBuilder);
        FactionClothConfig.setup(builder, entryBuilder);
        AiShootingClothConfig.setup(builder, entryBuilder);

        SpawnClothConfig.setup(builder, entryBuilder);
        SpawnEventClothConfig.setup(builder, entryBuilder);

        UnitAttributesClothConfig.setup(builder, entryBuilder);
        DropsClothConfig.setup(builder, entryBuilder);

        EffectClothConfig.setup(builder, entryBuilder);
        RenderClothConfig.setup(builder, entryBuilder);

        return builder.build();
    }
}

