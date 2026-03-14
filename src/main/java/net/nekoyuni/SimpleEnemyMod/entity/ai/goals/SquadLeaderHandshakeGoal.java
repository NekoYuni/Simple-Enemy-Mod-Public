package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.SquadData;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.TerrainScanner;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.IRoleHolder;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;

import java.util.*;

public class SquadLeaderHandshakeGoal extends Goal{


    private final PathfinderMob leader;
    private final int scanRange;
    private final int squadSearchRange;

    private final int scanInterval = 60;
    private final int handshakeInterval = 20;

    private TerrainScanner.FormationType currentFormation;
    private TerrainScanner.FormationType lastFormation;

    private final Map<Integer, Integer> assignedUnits = new HashMap<>(); // UnitID -> UnitIndex

    public SquadLeaderHandshakeGoal(PathfinderMob leader, int scanRange, int squadSearchRange) {
        this.leader = leader;
        this.scanRange = scanRange;
        this.squadSearchRange = squadSearchRange;
        this.currentFormation = TerrainScanner.FormationType.WEDGE;

        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return isSquadLeader(leader) && !leader.isAggressive() && leader.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return isSquadLeader(leader) && !leader.isAggressive() && leader.getTarget() == null;
    }

    @Override
    public void start() {
        this.lastFormation = null;

        performImmediateHandshake();
    }

    @Override
    public void tick() {
        int offset = leader.getId();

        if ((leader.tickCount + offset) % handshakeInterval == 0) {
            performHandshakeWithNearbyUnits();
            updateNearbyAssignedUnits();
        }


        if ((leader.tickCount + offset) % scanInterval == 0) {
            performTerrainScan();

            if (currentFormation != lastFormation) {
                updateAllAssignedUnits();
                lastFormation = currentFormation;
            }
        }
    }


    private void updateNearbyAssignedUnits() {
        AABB searchBox = new AABB(leader.blockPosition()).inflate(squadSearchRange);

        List<PathfinderMob> nearbyTeam = leader.level().getEntitiesOfClass(PathfinderMob.class, searchBox, entity -> {
            java.util.UUID storedLeaderUUID = SquadData.getLeaderUUID(entity);

            return assignedUnits.containsKey(entity.getId()) &&
                    storedLeaderUUID != null &&
                    storedLeaderUUID.equals(leader.getUUID());
        });

        for (PathfinderMob unit : nearbyTeam) {
            String currentUnitFormation = unit.getPersistentData().getString(SquadData.SQUAD_FORMATION);

            if (!currentUnitFormation.equals(currentFormation.name())) {
                int unitIndex = assignedUnits.get(unit.getId());
                SquadData.setSquadData(unit, leader, currentFormation, unitIndex);
            }
        }
    }


    private void performImmediateHandshake() {
        List<PathfinderMob> nearbyUnits = findNearbyUnassignedUnits();
        for (PathfinderMob unit : nearbyUnits) {
            performHandshakeWithUnit(unit);
        }
    }

    private void performHandshakeWithNearbyUnits() {
        List<PathfinderMob> unassignedUnits = findNearbyUnassignedUnits();

        for (PathfinderMob unit : unassignedUnits) {
            if (canAcceptUnit(unit)) {
                performHandshakeWithUnit(unit);
            }
        }

        cleanupDistantUnits();
    }

    private void performHandshakeWithUnit(PathfinderMob unit) {
        int unitIndex = assignedUnits.size();

        SquadData.setSquadData(unit, leader, currentFormation, unitIndex);
        assignedUnits.put(unit.getId(), unitIndex);
    }

    private List<PathfinderMob> findNearbyUnassignedUnits() {
        AABB searchBox = new AABB(leader.blockPosition()).inflate(squadSearchRange);

        return leader.level().getEntitiesOfClass(PathfinderMob.class, searchBox, entity -> {

            return isSquadUnit(entity) &&
                    isSameFaction(leader, entity) &&
                    !SquadData.hasValidSquadData(entity);
        });
    }

    private boolean canAcceptUnit(PathfinderMob unit) {
        final int MAX_SQUAD_SIZE = 8; // Máximo 8 unidades por squad
        return assignedUnits.size() < MAX_SQUAD_SIZE;
    }

    private void updateAllAssignedUnits() {
        List<PathfinderMob> assignedUnitsList = getAssignedUnits();

        for (PathfinderMob unit : assignedUnitsList) {
            int unitIndex = assignedUnits.get(unit.getId());
            SquadData.setSquadData(unit, leader, currentFormation, unitIndex);
        }

    }

    private List<PathfinderMob> getAssignedUnits() {
        AABB searchBox = new AABB(leader.blockPosition()).inflate(squadSearchRange * 2);

        return leader.level().getEntitiesOfClass(PathfinderMob.class, searchBox, entity -> {

            java.util.UUID storedLeaderUUID = SquadData.getLeaderUUID(entity);

            return assignedUnits.containsKey(entity.getId()) &&
                    storedLeaderUUID != null &&
                    storedLeaderUUID.equals(leader.getUUID());
        });
    }

    private void cleanupDistantUnits() {
        List<Integer> toRemove = new ArrayList<>();

        for (Integer unitId : assignedUnits.keySet()) {
            Entity entity = leader.level().getEntity(unitId);

            if (entity == null || !entity.isAlive() ||
                    leader.distanceToSqr(entity) > (squadSearchRange * squadSearchRange * 4)) {

                toRemove.add(unitId);

                if (entity instanceof PathfinderMob mob) {
                    SquadData.clearSquadData(mob);
                }
            }
        }

        toRemove.forEach(assignedUnits::remove);

        if (!toRemove.isEmpty()) {
            reorganizeUnitIndices();
        }
    }

    private void reorganizeUnitIndices() {
        List<PathfinderMob> currentUnits = getAssignedUnits();
        assignedUnits.clear();

        for (int i = 0; i < currentUnits.size(); i++) {
            PathfinderMob unit = currentUnits.get(i);
            assignedUnits.put(unit.getId(), i);
            SquadData.setSquadData(unit, leader, currentFormation, i);
        }
    }

    private void performTerrainScan() {
        BlockPos leaderPos = leader.blockPosition();

        TerrainScanner.FormationType newFormation = TerrainScanner.getFormationType(
                leader.level(), leaderPos, scanRange
        );
        this.currentFormation = newFormation;
    }

    private boolean isSquadLeader(PathfinderMob entity) {
        if (entity instanceof IRoleHolder roleHolder) {
            return roleHolder.getRole() == UnitRole.SQUAD_LEADER || roleHolder.getRole() == UnitRole.FRIENDLY_SQUAD_LEADER;
        }
        return false;
    }

    private boolean isSquadUnit(PathfinderMob entity) {
        if (entity instanceof IRoleHolder roleHolder) {
            return roleHolder.getRole() == UnitRole.SQUAD_UNIT || roleHolder.getRole() == UnitRole.FRIENDLY_SQUAD_UNIT;
        }
        return false;
    }

    private boolean isSameFaction(PathfinderMob leader, PathfinderMob unit) {
        return leader.getClass().equals(unit.getClass());
    }

    public TerrainScanner.FormationType getCurrentFormation() {
        return currentFormation;
    }

    public int getSquadSize() {
        return assignedUnits.size();
    }
}




