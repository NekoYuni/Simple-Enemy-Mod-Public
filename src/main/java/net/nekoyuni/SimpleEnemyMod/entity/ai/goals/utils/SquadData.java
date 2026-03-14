package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;

import java.util.UUID;

public class SquadData {

    public static final String SQUAD_LEADER_UUID = "SquadLeaderUUID";
    public static final String SQUAD_FORMATION = "SquadFormation";
    public static final String UNIT_INDEX = "UnitIndex";
    public static final String FORMATION_TIMESTAMP = "FormationTimestamp";
    public static final String HANDSHAKE_COMPLETE = "HandshakeComplete";


    public static void setSquadData(PathfinderMob unit, PathfinderMob leader,
                                    TerrainScanner.FormationType formation, int unitIndex) {
        CompoundTag data = unit.getPersistentData();

        data.putUUID(SQUAD_LEADER_UUID, leader.getUUID());
        data.putString(SQUAD_FORMATION, formation.name());
        data.putInt(UNIT_INDEX, unitIndex);
        data.putLong(FORMATION_TIMESTAMP, unit.level().getGameTime());
        data.putBoolean(HANDSHAKE_COMPLETE, true);
    }

    public static UUID getLeaderUUID(PathfinderMob unit) {
        CompoundTag data = unit.getPersistentData();

        if (data.hasUUID(SQUAD_LEADER_UUID)) {
            return data.getUUID(SQUAD_LEADER_UUID);
        }
        return null;
    }

    public static boolean hasValidSquadData(PathfinderMob unit) {
        CompoundTag data = unit.getPersistentData();

        return data.hasUUID(SQUAD_LEADER_UUID) &&
                data.contains(SQUAD_FORMATION) &&
                data.getBoolean(HANDSHAKE_COMPLETE);
    }

    public static void clearSquadData(PathfinderMob unit) {
        CompoundTag data = unit.getPersistentData();
        data.remove(SQUAD_LEADER_ID);
        data.remove(SQUAD_LEADER_UUID);
        data.remove(SQUAD_FORMATION);
        data.remove(UNIT_INDEX);
        data.remove(FORMATION_TIMESTAMP);
        data.remove(HANDSHAKE_COMPLETE);
    }

    private static final String SQUAD_LEADER_ID = "SquadLeaderID";

}
