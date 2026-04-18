package net.nekoyuni.SimpleEnemyMod.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class EventProbabilityData extends SavedData {

    private final Map<String, Map<String, Double>> playerChances = new HashMap<>();
    private final Map<String, Boolean> eventActiveStates = new HashMap<>();
    private final Map<String, Integer> playerTickCounters = new HashMap<>();

    public boolean shouldTickForPlayer(String playerUUID) {
        int count = playerTickCounters.getOrDefault(playerUUID, 0) + 1;
        if (count >= 2400) {
            playerTickCounters.put(playerUUID, 0);
            return true;
        }
        playerTickCounters.put(playerUUID, count);
        return false;
    }

    public double getChance(String eventId, String playerUUID, double defaultBase) {
        return Math.min(1.0, playerChances
                .getOrDefault(eventId, new HashMap<>())
                .getOrDefault(playerUUID, defaultBase));
    }

    public void setChance(String eventId, String playerUUID, double chance) {
        playerChances.computeIfAbsent(eventId, k -> new HashMap<>())
                .put(playerUUID, Math.min(1.0, chance));
        setDirty();
    }

    public void resetChance(String eventId, String playerUUID, double baseChance) {
        playerChances.computeIfAbsent(eventId, k -> new HashMap<>())
                .put(playerUUID, baseChance);
        setDirty();
    }

    public void resetAllChancesForPlayer(String playerUUID, Map<String, Double> baseChances) {
        baseChances.forEach((eventId, base) -> resetChance(eventId, playerUUID, base));
    }

    public boolean isEventActive(String eventId) {
        return eventActiveStates.getOrDefault(eventId, true);
    }

    public void setEventActive(String eventId, boolean active) {
        eventActiveStates.put(eventId, active);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag eventList = new ListTag();
        playerChances.forEach((eventId, perPlayer) -> {
            CompoundTag eventEntry = new CompoundTag();
            eventEntry.putString("eventId", eventId);

            ListTag playerList = new ListTag();
            perPlayer.forEach((uuid, chance) -> {
                CompoundTag playerEntry = new CompoundTag();
                playerEntry.putString("uuid", uuid);
                playerEntry.putDouble("chance", chance);
                playerList.add(playerEntry);
            });
            eventEntry.put("players", playerList);
            eventList.add(eventEntry);
        });
        tag.put("PlayerChances", eventList);

        ListTag stateList = new ListTag();
        eventActiveStates.forEach((id, active) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putBoolean("active", active);
            stateList.add(entry);
        });
        tag.put("EventStates", stateList);

        return tag;
    }

    public static EventProbabilityData load(CompoundTag tag) {
        EventProbabilityData data = new EventProbabilityData();

        if (tag.contains("PlayerChances")) {
            ListTag eventList = tag.getList("PlayerChances", Tag.TAG_COMPOUND);
            for (int i = 0; i < eventList.size(); i++) {
                CompoundTag eventEntry = eventList.getCompound(i);
                String eventId = eventEntry.getString("eventId");
                if (eventId.isEmpty()) continue;

                ListTag playerList = eventEntry.getList("players", Tag.TAG_COMPOUND);
                Map<String, Double> perPlayer = new HashMap<>();
                for (int j = 0; j < playerList.size(); j++) {
                    CompoundTag playerEntry = playerList.getCompound(j);
                    String uuid = playerEntry.getString("uuid");
                    if (!uuid.isEmpty()) {
                        perPlayer.put(uuid, playerEntry.getDouble("chance"));
                    }
                }
                data.playerChances.put(eventId, perPlayer);
            }
        }

        if (tag.contains("EventStates")) {
            ListTag list = tag.getList("EventStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                if (!id.isEmpty()) {
                    data.eventActiveStates.put(id, entry.getBoolean("active"));
                }
            }
        }

        return data;
    }

    public static EventProbabilityData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(
                EventProbabilityData::load,
                EventProbabilityData::new,
                "sem_event_data"
        );
    }
}