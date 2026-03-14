package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.SquadData;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.TerrainScanner;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.IRoleHolder;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;

import java.util.EnumSet;

public class SquadUnitHandshakeFollowGoal extends Goal {

    private final PathfinderMob unit;
    private final double movementSpeed;
    private final int maxLeaderDistance;

    private PathfinderMob squadLeader;
    private TerrainScanner.FormationType currentFormation;
    private Vec3 targetPosition;
    private int unitIndex;

    private int validationTimer;
    private final int validationInterval = 40;

    public SquadUnitHandshakeFollowGoal(PathfinderMob unit, double movementSpeed, int maxLeaderDistance) {
        this.unit = unit;
        this.movementSpeed = movementSpeed;
        this.maxLeaderDistance = maxLeaderDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !unit.isAggressive() &&
                unit.getTarget() == null &&
                SquadData.hasValidSquadData(unit) &&
                findLeaderFromNBT() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (unit.isAggressive() || unit.getTarget() != null) {
            return false;
        }

        if (squadLeader == null || !squadLeader.isAlive()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.validationTimer = 0;
        loadSquadDataFromNBT();
    }

    @Override
    public void tick() {
        validationTimer++;

        if (validationTimer >= validationInterval) {
            if (!validateAndUpdateSquadData()) {
                return;
            }
            validationTimer = 0;
        }

        if (squadLeader != null && currentFormation != null) {
            calculateTargetPosition();
            moveToFormationPosition();
        }
    }

    @Override
    public void stop() {
        this.targetPosition = null;
    }


    private PathfinderMob findLeaderFromNBT() {

        if (!SquadData.hasValidSquadData(unit)) {
            return null;
        }

        java.util.UUID leaderUUID = SquadData.getLeaderUUID(unit);
        if (leaderUUID == null) return null;

        if (unit.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity leaderEntity = serverLevel.getEntity(leaderUUID);

            if (leaderEntity instanceof PathfinderMob mob &&
                    isSquadLeader(mob) &&
                    isSameFaction(unit, mob)) {
                return mob;
            }
        }

        return null;
    }

    private void loadSquadDataFromNBT() {
        squadLeader = findLeaderFromNBT();

        if (squadLeader != null) {
            CompoundTag data = unit.getPersistentData();

            String formationName = data.getString(SquadData.SQUAD_FORMATION);
            try {
                this.currentFormation = TerrainScanner.FormationType.valueOf(formationName);
            } catch (IllegalArgumentException e) {
                this.currentFormation = TerrainScanner.FormationType.WEDGE;
            }

            this.unitIndex = data.getInt(SquadData.UNIT_INDEX);
        }
    }

    private boolean validateAndUpdateSquadData() {
        if (!SquadData.hasValidSquadData(unit)) {
            return false;
        }

        PathfinderMob newLeader = findLeaderFromNBT();
        if (newLeader == null) {
            SquadData.clearSquadData(unit);
            return false;
        }

        squadLeader = newLeader;
        loadSquadDataFromNBT();

        return true;
    }

    private void calculateTargetPosition() {
        if (squadLeader == null || currentFormation == null) {
            return;
        }

        Vec3 leaderPos = squadLeader.position();
        Vec3 leaderLook = squadLeader.getLookAngle();

        switch (currentFormation) {
            case COLUMN -> targetPosition = calculateColumnPosition(leaderPos, leaderLook);
            case WEDGE -> targetPosition = calculateWedgePosition(leaderPos, leaderLook);
            default -> targetPosition = leaderPos;
        }
    }

    private Vec3 calculateColumnPosition(Vec3 leaderPos, Vec3 leaderLook) {
        double spacing = 2.0;
        double behindDistance = 2.0 + (unitIndex * spacing);
        Vec3 behind = leaderLook.scale(-behindDistance);

        return leaderPos.add(behind);
    }

    private Vec3 calculateWedgePosition(Vec3 leaderPos, Vec3 leaderLook) {

        double spacing = 2.5;
        double behindDistance = 1.5 + (Math.abs(unitIndex) * 0.8);

        Vec3 right = new Vec3(-leaderLook.z, 0, leaderLook.x).normalize();
        double side = (unitIndex % 2 == 0) ? 1.0 : -1.0;
        double sideDistance = spacing * ((unitIndex / 2) + 1);

        Vec3 behind = leaderLook.scale(-behindDistance);
        Vec3 sideways = right.scale(side * sideDistance);

        return leaderPos.add(behind).add(sideways);
    }

    private int pathRecalcDelay = 0;
    private Vec3 lastTargetPos = Vec3.ZERO;

    private void moveToFormationPosition() {
        if (targetPosition == null) return;

        double distanceToTarget = unit.position().distanceTo(targetPosition);

        if (distanceToTarget <= 2.2) {
            if (!unit.getNavigation().isDone()) {
                unit.getNavigation().stop();
            }
            return;
        }

        boolean targetMovedSignificantly = lastTargetPos.distanceToSqr(targetPosition) > 1.0;

        if (this.pathRecalcDelay > 0 && !targetMovedSignificantly) {
            this.pathRecalcDelay--;
            return;
        }

        this.pathRecalcDelay = 10 + unit.getRandom().nextInt(10);
        this.lastTargetPos = targetPosition;

        unit.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, movementSpeed);
    }

    private boolean isSquadLeader(PathfinderMob entity) {
        if (entity instanceof IRoleHolder roleHolder) {
            return roleHolder.getRole() == UnitRole.SQUAD_LEADER || roleHolder.getRole() == UnitRole.FRIENDLY_SQUAD_LEADER;
        }
        return false;
    }

    private boolean isSameFaction(PathfinderMob unit1, PathfinderMob unit2) {
        return unit1.getClass().equals(unit2.getClass());
    }

    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    public TerrainScanner.FormationType getCurrentFormation() {
        return currentFormation;
    }

    public PathfinderMob getSquadLeader() {
        return squadLeader;
    }
}
