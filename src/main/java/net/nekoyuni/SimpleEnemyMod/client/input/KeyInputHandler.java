package net.nekoyuni.SimpleEnemyMod.client.input;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.gui.screens.CommanderMenuScreen;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {

        if (KeyBindings.COMMANDER_MENU_KEY.consumeClick()) {

             if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
                 Minecraft.getInstance().setScreen(new CommanderMenuScreen());
             }
        }
    }
}
