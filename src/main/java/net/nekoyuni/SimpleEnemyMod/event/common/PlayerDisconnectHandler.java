package net.nekoyuni.SimpleEnemyMod.event.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.procedural.events.DynamicEventManager;

@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDisconnectHandler {

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DynamicEventManager.onPlayerDisconnect(serverPlayer);
        }
    }

}
