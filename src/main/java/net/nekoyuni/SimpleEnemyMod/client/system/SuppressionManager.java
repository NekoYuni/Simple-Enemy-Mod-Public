package net.nekoyuni.SimpleEnemyMod.client.system;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT)
public class SuppressionManager {

    public static float suppressionLevel = 0.0f;
    private static final float DECAY_RATE = 0.0025f;

    /**
     * Calls this method when a bullet passes close or hits close.
     * @param amount How much stress to add (e.g., 0.1f for a minor scare, 0.4f for a major scare)
     */
    public static void addSuppression(float amount) {
        suppressionLevel += amount;

        if (suppressionLevel > 1.0f) {
            suppressionLevel = 1.0f;
        }
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            if (suppressionLevel > 0) {
                suppressionLevel -= DECAY_RATE;

                if (suppressionLevel < 0) {
                    suppressionLevel = 0.0f;
                }

            }
        }
    }
}
