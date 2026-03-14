package net.nekoyuni.SimpleEnemyMod.procedural.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.DynamicEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.CaveExtractionEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.CombatEvent;
import net.nekoyuni.SimpleEnemyMod.procedural.events.type.PatrolEvent;
import net.nekoyuni.SimpleEnemyMod.spawn.utils.SpawnHelper;
import net.nekoyuni.SimpleEnemyMod.world.data.EventProbabilityData;

import java.util.*;

public class DynamicEventManager {

    private static final Map<String, DynamicEvent> REGISTERED_EVENTS = new HashMap<>();
    private static final Random RANDOM = new Random();


    public static void registerEvent(DynamicEvent event) {
        REGISTERED_EVENTS.put(event.getId(), event);
    }

    public static Set<String> getEventIds() {
        return REGISTERED_EVENTS.keySet();
    }

    public static void tick(ServerLevel level) {

        if (!level.dimension().equals(Level.OVERWORLD)) return;

        EventProbabilityData data = EventProbabilityData.get(level);
        if (!data.shouldTick()) return;

        if (level.players().isEmpty()) return;


        for (DynamicEvent event : REGISTERED_EVENTS.values()) {
            if (!data.isEventActive(event.getId())) continue;

            tryTryEvent(level, data, event);
        }
    }

    private static void tryTryEvent(ServerLevel level, EventProbabilityData data, DynamicEvent event) {
        ServerPlayer player = level.players().get(RANDOM.nextInt(level.players().size()));

        if (!event.canExecute(level, player)) return;

        double currentChance = data.getChance(event.getId(), event.getBaseChance());

        BlockPos targetPos = SpawnHelper.getRandomPositionNearPlayer(player, event.getMinDistance(), event.getMaxDistance(), level);

        double biomeMod = event.getBiomeModifier(level, targetPos);
        double finalChance = currentChance * biomeMod;

        if (RANDOM.nextDouble() < finalChance) {
            boolean success = event.execute(level, player, targetPos);

            if (success) {
                data.resetChance(event.getId(), event.getBaseChance());
            } else {

            }

        } else {
            double increase = event.getBaseChance() * event.getFailureMultiplier();
            double newChance = Math.min(1.0, currentChance + increase);

            data.setChance(event.getId(), newChance);
        }
    }

    public static boolean forceEvent(ServerLevel level, ServerPlayer player, String eventId) {

        DynamicEvent event = REGISTERED_EVENTS.get(eventId);
        if (event == null) return false;

        BlockPos targetPos = SpawnHelper.getRandomPositionNearPlayer(player, event.getMinDistance(), event.getMaxDistance(), level);

        boolean success = event.execute(level, player, targetPos);
        if (success) {
            EventProbabilityData.get(level).resetChance(eventId, event.getBaseChance());
        }

        return success;
    }

    public static void register() {
        registerEvent(new PatrolEvent());
        registerEvent(new CombatEvent());
        registerEvent(new CaveExtractionEvent());
    }
}
