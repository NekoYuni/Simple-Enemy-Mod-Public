package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.AIDifficultySettings;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class TacticalManeuverGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger(TacticalManeuverGoal.class);
    private static final boolean isDebug = false;

    private final AbstractUnit unit;
    private static final int MOVE_COOLDOWN = 10;

    private int lastSeenTick = 0;
    private static final int ATTENTION_SPAN = 40;
    private int pauseTicks = 0;

    private Vec3 lastFlankTarget = Vec3.ZERO;
    private int stuckCounter = 0;
    private static final int MAX_STUCK_CHECKS = 3;

    private final double FLANK_SPEED;
    private final double MID_RANGE_SPEED;

    // Performance Patch
    private int pathRecalcCooldown = 0;
    private static final int PATH_RECALC_INTERVAL = 20;

    private int flankCalcCooldown = 0;
    private static final int FLANK_CALC_INTERVAL = 15;

    private Vec3 cachedFlankPos = null;


    public TacticalManeuverGoal(AbstractUnit unit) {
        this.unit = unit;
        this.setFlags(EnumSet.of(Flag.MOVE));

        AIDifficultySettings settings = AIDifficultySettings.fromConfig();
        this.FLANK_SPEED = settings.flankSpeed;
        this.MID_RANGE_SPEED = settings.midRangeSpeed;
    }

    @Override
    public boolean canUse() {
        SoldierState state = unit.getSoldierState();
        return state == SoldierState.ENGAGE || state == SoldierState.TACTICAL_MOV;
    }

    @Override
    public boolean canContinueToUse() {
        if (unit.isMovementLockedByManager()) {
            return false;
        }

        SoldierState state = unit.getSoldierState();
        return state == SoldierState.ENGAGE || state == SoldierState.TACTICAL_MOV;
    }

    @Override
    public void tick() {
        LivingEntity target = unit.getTarget();
        if (target == null) return;

        // LOCK TEST
        if (unit.isMovementLockedByManager()) {
            return;
        }

        int staggerOffset = unit.getId() % 5;
        long currentTime = unit.level().getGameTime();
        boolean isMyTurn = (currentTime % 5) == staggerOffset;

        if (!isMyTurn) {
            if (unit.hasLineOfSight(target)) {
                unit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            return;
        }

        SoldierState state = unit.getSoldierState();
        boolean hasLOS = unit.hasLineOfSight(target);

        // HOLD ANGLE
        if (pauseTicks > 0) {
            pauseTicks--;
            unit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            unit.getNavigation().stop();
            return;
        }

        // VISUAL MEMORY
        if (hasLOS) {
            lastSeenTick = (int) currentTime;
        }

        boolean shouldLockOnTarget = hasLOS || (currentTime - lastSeenTick < ATTENTION_SPAN);
        if (shouldLockOnTarget) {
            unit.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (state == SoldierState.TACTICAL_MOV &&
                unit.isFlankingActive() &&
                unit.getStrategy() != StrategyType.CLOSE_RANGE) {
            handleFlankingMovement(target);
            return;
        }

        // Tactical move without Flanking
        if (state == SoldierState.TACTICAL_MOV) {
            unit.getNavigation().moveTo(target, 1.25);
            return;
        }

        // Combat Logic
        if (unit.level().getGameTime() - unit.getLastMicroMoveTick() < MOVE_COOLDOWN) {
            return;
        }

        if (unit.getStrategy() != StrategyType.CLOSE_RANGE && unit.getRandom().nextFloat() < 0.02) {
            pauseTicks = 20 + unit.getRandom().nextInt(20);
            return;
        }

        StrategyType strategy = unit.getStrategy();
        if (strategy == null) strategy = StrategyType.MID_RANGE;

        Vec3 movement = null;
        double speed = 1.0;

        // TODO add to the config File these values
        switch (strategy) {
            case LONG_RANGE -> {
                movement = suppressionHold(target);
                speed = 1.15;
            }
            case MID_RANGE -> {
                if (hasLOS) {
                    movement = basicStrafe(target);
                    speed = MID_RANGE_SPEED;
                } else {
                    movement = null;
                    speed = MID_RANGE_SPEED;
                }
            }
            case CLOSE_RANGE -> {
                movement = aggressiveCQB(target);
                speed = 1.2;
            }
        }

        if (movement != null) {
            if (strategy != StrategyType.LONG_RANGE) {
                movement = applyWallBias(movement);
            }

            unit.getNavigation().moveTo(movement.x, movement.y, movement.z, speed);
            unit.setLastMicroMoveTick(unit.level().getGameTime());
        }
    }

    // Patch for better performance
    private void handleFlankingMovement(LivingEntity target) {

        if (flankCalcCooldown <= 0 || cachedFlankPos == null) {
            cachedFlankPos = calculateCircularFlankPosition(target);
            flankCalcCooldown = FLANK_CALC_INTERVAL;
        } else {
            flankCalcCooldown--;
        }

        Vec3 flankPos = cachedFlankPos;

        double distToFlankPos = unit.position().distanceTo(flankPos);
        double distToLastTarget = unit.position().distanceTo(lastFlankTarget);

        if (!lastFlankTarget.equals(Vec3.ZERO) && flankPos.distanceTo(lastFlankTarget) < 3.0) {
            stuckCounter++;

            if (stuckCounter >= MAX_STUCK_CHECKS) {
                float currentAngle = unit.getFlankingAngle();
                currentAngle += (60.0f * unit.getFlankingDirection());
                if (currentAngle >= 360.0f) currentAngle -= 360.0f;
                if (currentAngle < 0.0f) currentAngle += 360.0f;
                unit.setFlankingAngle(currentAngle);

                cachedFlankPos = null;
                flankCalcCooldown = 0;
                stuckCounter = 0;
                return;
            }
        } else {
            stuckCounter = 0;
        }

        lastFlankTarget = flankPos;

        boolean destinationChanged = cachedFlankPos != null &&
                flankPos.distanceTo(cachedFlankPos) > 2.0;

        if (pathRecalcCooldown <= 0 || destinationChanged) {
            unit.getNavigation().moveTo(flankPos.x, flankPos.y, flankPos.z, FLANK_SPEED);
            pathRecalcCooldown = PATH_RECALC_INTERVAL;
        } else {
            pathRecalcCooldown--;
        }

        debug("[ManeuverGoal] Flanking - Unit: " + unit.getId() +
                " | Dist: " + String.format("%.1f", distToFlankPos) +
                " | Stuck: " + stuckCounter);

        if (distToFlankPos < 5.0 || unit.getNavigation().isDone()) {
            unit.setSoldierState(SoldierState.SEEK_COVER);
            cachedFlankPos = null;
        }
    }

    // CLose Range
    private Vec3 aggressiveCQB(LivingEntity target) {
        Vec3 dir = directionTo(target);
        Vec3 lateral = new Vec3(-dir.z, 0, dir.x);

        if (unit.getRandom().nextBoolean()) lateral = lateral.scale(1.0);
        else lateral = lateral.scale(-1.0);

        Vec3 moveDir = lateral.scale(3.0).add(dir.scale(1.5)).normalize();

        return unit.position().add(moveDir.scale(3.0));
    }

    // Mid Range
    private Vec3 basicStrafe(LivingEntity target) {
        Vec3 dir = directionTo(target);
        Vec3 lateral = new Vec3(-dir.z, 0, dir.x);

        if (unit.getRandom().nextBoolean()) lateral = lateral.scale(1.0);
        else lateral = lateral.scale(-1.0);

        Vec3 moveDir = lateral.add(dir.scale(-0.2)).normalize();

        return unit.position().add(moveDir.scale(5.0));
    }

    // Long Range
    private Vec3 suppressionHold(LivingEntity target) {
        Vec3 dir = directionTo(target);
        Vec3 lateral = new Vec3(-dir.z, 0, dir.x);

        if (unit.getRandom().nextBoolean()) lateral = lateral.scale(1.0);
        else lateral = lateral.scale(-1.0);

        return unit.position().add(lateral.normalize().scale(1.5));
    }

    /** HELPERS */
    private Vec3 applyWallBias(Vec3 originalMovePos) {
        Level level = unit.level();
        BlockPos currentPos = unit.blockPosition();

        Vec3 leftDir = unit.getViewVector(1.0f).yRot((float) Math.toRadians(90));
        Vec3 rightDir = unit.getViewVector(1.0f).yRot((float) Math.toRadians(-90));

        boolean wallOnLeft = !level.getBlockState(currentPos.relative(getDirectionFromVec(leftDir), 2)).isAir();
        boolean wallOnRight = !level.getBlockState(currentPos.relative(getDirectionFromVec(rightDir), 2)).isAir();

        if (wallOnLeft && !wallOnRight) {
            return originalMovePos.add(leftDir.scale(1.5));
        }
        if (wallOnRight && !wallOnLeft) {
            return originalMovePos.add(rightDir.scale(1.5));
        }

        return originalMovePos;
    }

    private net.minecraft.core.Direction getDirectionFromVec(Vec3 vec) {
        return net.minecraft.core.Direction.getNearest(vec.x, vec.y, vec.z);
    }

    private Vec3 directionTo(LivingEntity target) {
        return new Vec3(
                target.getX() - unit.getX(),
                0,
                target.getZ() - unit.getZ()
        ).normalize();
    }


    private Vec3 calculateCircularFlankPosition(LivingEntity target) {
        Vec3 lastKnownPos = unit.getLastKnownTargetPos();
        if (lastKnownPos == null) {
            return basicStrafe(target);
        }

        double currentDistance = unit.position().distanceTo(lastKnownPos);
        double flankRadius;

        if (currentDistance > 55.0) {
            flankRadius = 55.0;
        } else if (currentDistance > 20.0) {
            flankRadius = currentDistance;
        } else {
            flankRadius = 17.0;
        }

        flankRadius = Math.max(flankRadius, 15.0);
        flankRadius = Math.min(flankRadius, 55.0);

        float angleIncrement;
        if (flankRadius > 45.0) {
            angleIncrement = 25.0f;
        } else if (flankRadius > 30.0) {
            angleIncrement = 40.0f;
        } else {
            angleIncrement = 50.0f;
        }

        float currentAngle = unit.getFlankingAngle();
        int direction = unit.getFlankingDirection();

        currentAngle += (angleIncrement * direction);

        if (currentAngle >= 360.0f) currentAngle -= 360.0f;
        if (currentAngle < 0.0f) currentAngle += 360.0f;

        unit.setFlankingAngle(currentAngle);

        double radians = Math.toRadians(currentAngle);
        double offsetX = Math.cos(radians) * flankRadius;
        double offsetZ = Math.sin(radians) * flankRadius;

        Vec3 targetPos = new Vec3(
                lastKnownPos.x + offsetX,
                lastKnownPos.y,
                lastKnownPos.z + offsetZ
        );

        BlockPos targetBlock = BlockPos.containing(targetPos);
        BlockPos safePos = findNearestSafePosition(targetBlock, 5);

        if (safePos != null) {
            unit.setCoverSearchOrigin(safePos);
            return Vec3.atCenterOf(safePos);
        }

        return targetPos;
    }

    // Performance Fix - Cache
    private BlockPos cachedSafePos = null;
    private Vec3 lastCachedOrigin = null;
    private static final double CACHE_INVALIDATION_DIST = 4.0;

    @Nullable
    private BlockPos findNearestSafePosition(BlockPos target, int searchRadius) {

        Vec3 targetVec = Vec3.atCenterOf(target);
        if (cachedSafePos != null && lastCachedOrigin != null &&
                targetVec.distanceTo(lastCachedOrigin) < CACHE_INVALIDATION_DIST) {
            return cachedSafePos;
        }

        Level level = unit.level();
        for (int r = 0; r <= searchRadius; r++) {

            for (int dx = -r; dx <= r; dx++) {

                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r && r > 0) continue;
                    BlockPos candidate = target.offset(dx, 0, dz);

                    if (level.getBlockState(candidate).isAir() &&
                            level.getBlockState(candidate.above()).isAir() &&
                            level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) {
                        cachedSafePos = candidate;
                        lastCachedOrigin = targetVec;

                        return candidate;
                    }
                }

            }

        }

        cachedSafePos = null;
        lastCachedOrigin = targetVec;
        return null;
    }

    @Override
    public void stop() {
        unit.getNavigation().stop();
        cachedFlankPos = null;
        cachedSafePos = null;
        lastCachedOrigin = null;
        pathRecalcCooldown = 0;
        flankCalcCooldown = 0;
    }

    private static void debug(String message, Object... args) {
        if (isDebug && LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }
}
