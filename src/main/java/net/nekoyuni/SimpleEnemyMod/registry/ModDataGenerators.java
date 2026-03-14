package net.nekoyuni.SimpleEnemyMod.registry;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.compat.curios.CuriosCompat;
import net.nekoyuni.SimpleEnemyMod.compat.curios.SimpleDataGenerators;

@Mod.EventBusSubscriber(modid = "simpleenemymod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {

        if (CuriosCompat.LOADED) {
            SimpleDataGenerators.onGatherData(event);
        }

    }
}
