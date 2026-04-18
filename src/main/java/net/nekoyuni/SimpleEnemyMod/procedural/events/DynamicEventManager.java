package net.nekoyuni.SimpleEnemyMod.procedural.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.ChunkSafetyEvaluator;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.DynamicEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.EventCooldownManager;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.CaveExtractionEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.CombatEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.PatrolEvent;
import net.nekoyuni.SimpleEnemyMod.spawn.utils.SpawnHelper;
import net.nekoyuni.SimpleEnemyMod.world.data.EventProbabilityData;

import java.util.*;

public class DynamicEventManager {

    private static final Map<String, DynamicEvent> REGISTERED_EVENTS = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    private static final EventCooldownManager COOLDOWN_MANAGER = new EventCooldownManager();

    public static void registerEvent(DynamicEvent event) {
        REGISTERED_EVENTS.put(event.getId(), event);
    }

    public static Set<String> getEventIds() {
        return REGISTERED_EVENTS.keySet();
    }

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        COOLDOWN_MANAGER.tick(players);

        EventProbabilityData data = EventProbabilityData.get(level);

        for (ServerPlayer player : players) {
            if (COOLDOWN_MANAGER.isOnCooldown(player)) continue;
            if (!data.shouldTickForPlayer(player.getUUID().toString())) continue;

            for (DynamicEvent event : REGISTERED_EVENTS.values()) {
                if (!data.isEventActive(event.getId())) continue;

                boolean triggered = tryEventForPlayer(level, data, event, player);
                if (triggered) {
                    COOLDOWN_MANAGER.triggerCooldown(player);
                    break;
                }
            }
        }
    }

    private static boolean tryEventForPlayer(ServerLevel level, EventProbabilityData data,
                                             DynamicEvent event, ServerPlayer player) {

        if (!event.canExecute(level, player)) return false;

        String playerUUID = player.getUUID().toString();
        double currentChance = data.getChance(event.getId(), playerUUID, event.getBaseChance());

        BlockPos targetPos = SpawnHelper.getRandomPositionNearPlayer(
                player, event.getMinDistance(), event.getMaxDistance(), level
        );

        if (ChunkSafetyEvaluator.isPlayerBase(level, targetPos)) return false;

        double finalChance = currentChance * event.getBiomeModifier(level, targetPos);

        if (RANDOM.nextDouble() < finalChance) {
            boolean success = event.execute(level, player, targetPos);

            if (success) {
                Map<String, Double> bases = new HashMap<>();
                REGISTERED_EVENTS.forEach((id, e) -> bases.put(id, e.getBaseChance()));
                data.resetAllChancesForPlayer(playerUUID, bases);
                return true;
            }

        } else {
            double increase = event.getBaseChance() * event.getFailureMultiplier();
            data.setChance(event.getId(), playerUUID, currentChance + increase);
        }

        return false;
    }

    public static boolean forceEvent(ServerLevel level, ServerPlayer player, String eventId) {
        DynamicEvent event = REGISTERED_EVENTS.get(eventId);
        if (event == null) return false;

        BlockPos targetPos = SpawnHelper.getRandomPositionNearPlayer(
                player, event.getMinDistance(), event.getMaxDistance(), level
        );

        boolean success = event.execute(level, player, targetPos);
        if (success) {
            EventProbabilityData.get(level).resetChance(eventId, player.getUUID().toString(), event.getBaseChance());
        }
        return success;
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        COOLDOWN_MANAGER.onPlayerDisconnect(player);
    }

    public static EventCooldownManager getCooldownManager() {
        return COOLDOWN_MANAGER;
    }

    public static void register() {
        registerEvent(new PatrolEvent());
        registerEvent(new CombatEvent());
        registerEvent(new CaveExtractionEvent());
    }
}
