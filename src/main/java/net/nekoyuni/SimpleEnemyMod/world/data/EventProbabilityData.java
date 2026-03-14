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

    private final Map<String, Double> currentChances = new HashMap<>();
    private final Map<String, Boolean> eventActiveStates = new HashMap<>();

    private int tickCounter = 0;

    public boolean shouldTick() {
        tickCounter++;
        if (tickCounter >= 1200) {
            tickCounter = 0;
            return true;
        }
        return false;
    }

    public double getChance(String eventId, double defaultBase) {
        return Math.min(1.0, currentChances.getOrDefault(eventId, defaultBase));
    }

    public void setChance(String eventId, double chance) {
        currentChances.put(eventId, chance);
        setDirty();
    }

    public void resetChance(String eventId, double baseChance) {
        currentChances.put(eventId, baseChance);
        setDirty();
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
        ListTag chanceList = new ListTag();
        currentChances.forEach((id, chance) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putDouble("chance", chance);
            chanceList.add(entry);
        });
        tag.put("EventChances", chanceList);

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

        if (tag.contains("EventChances")) {
            ListTag list = tag.getList("EventChances", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                if (!id.isEmpty()) {
                    data.currentChances.put(entry.getString("id"), entry.getDouble("chance"));
                }
            }
        }

        if (tag.contains("EventStates")) {
            ListTag list = tag.getList("EventStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                if (!id.isEmpty()) {
                    data.eventActiveStates.put(entry.getString("id"), entry.getBoolean("active"));
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