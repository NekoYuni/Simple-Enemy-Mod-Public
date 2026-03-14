package net.nekoyuni.SimpleEnemyMod.event.client;

import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.gui.screens.CommanderMenuScreen;
import net.nekoyuni.SimpleEnemyMod.client.gui.overlay.CommanderOverlayRenderer;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT)
public class TaczInputHandler {

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {

        if (!event.getLogicalSide().isClient()) return;
        if (event.getShooter() != Minecraft.getInstance().player) return;

        boolean justClosedMenu = CommanderMenuScreen.shouldSuppressFire();
        boolean isSelectingPosition = CommanderOverlayRenderer.isSelectingPosition;
        boolean isSelectingTarget = CommanderOverlayRenderer.isSelectingTarget;

        if (justClosedMenu || isSelectingPosition || isSelectingTarget) {
            event.setCanceled(true);
        }
    }
}
