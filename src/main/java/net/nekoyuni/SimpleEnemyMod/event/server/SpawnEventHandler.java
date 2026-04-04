package net.nekoyuni.SimpleEnemyMod.event.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.procedural.events.DynamicEventManager;


@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID)
public class SpawnEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld == null) return;

        DynamicEventManager.tick(overworld);
    }
}
