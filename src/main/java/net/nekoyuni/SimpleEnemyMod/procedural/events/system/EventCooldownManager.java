package net.nekoyuni.SimpleEnemyMod.procedural.events.system;

import net.minecraft.server.level.ServerPlayer;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventCooldownManager {

    private static final int DEFAULT_COOLDOWN_TICKS = 600;
    private final Map<UUID, Integer> playerCooldowns = new HashMap<>();


    public void tick(Iterable<ServerPlayer> onlinePlayers) {

        for (ServerPlayer player : onlinePlayers) {
            UUID uuid = player.getUUID();

            int current = playerCooldowns.getOrDefault(uuid, 0);

            if (current > 0) {
                playerCooldowns.put(uuid, current - 1);
            }
        }
    }

    public boolean isOnCooldown(ServerPlayer player) {
        return playerCooldowns.getOrDefault(player.getUUID(), 0) > 0;
    }

    public void triggerCooldown(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), getCooldownTicks());
    }

    public void resetCooldown(ServerPlayer player) {
        playerCooldowns.remove(player.getUUID());
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        playerCooldowns.remove(player.getUUID());
    }

    public int getRemainingTicks(ServerPlayer player) {
        return playerCooldowns.getOrDefault(player.getUUID(), 0);
    }

    private int getCooldownTicks() {
        try {
            return CommonConfig.EVENT_GLOBAL_COOLDOWN_TICKS.get();
        } catch (Exception e) {
            return DEFAULT_COOLDOWN_TICKS;
        }
    }
}