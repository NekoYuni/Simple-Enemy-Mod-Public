package net.nekoyuni.SimpleEnemyMod.event.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.system.SuppressionManager;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT)
public class ClientCameraHandler {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        float level = SuppressionManager.suppressionLevel;

        if (level > 0.1f) {

            double time = (Minecraft.getInstance().level.getGameTime() + event.getPartialTick()) * 0.05;

            float rollAmount = (float) Math.sin(time) * 2.0f * level;
            event.setRoll(event.getRoll() + rollAmount);

            float shakeAmount = (float) Math.cos(time * 2.5) * 0.5f * level;

            event.setYaw(event.getYaw() + shakeAmount);
            event.setPitch(event.getPitch() + (shakeAmount * 0.5f));
        }
    }

}
