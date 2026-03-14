package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;

import javax.annotation.Nullable;
import java.util.EnumSet;


public class SeekCoverGoal extends Goal {

    private final AbstractUnit unit;
    private final Mob mob;
    private final double speed;
    private final int searchRadius;
    private int cooldownTicks = 0;
    private boolean abortedByManager = false;

    @Nullable
    private BlockPos targetCover;

    public SeekCoverGoal(AbstractUnit unit, double speed, int searchRadius) {
        this.unit = unit;
        this.mob = unit;
        this.speed = speed;
        this.searchRadius = Math.max(1, searchRadius);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        // LOCK TEST
        if (!unit.isMovementLockedByManager()) {
            return false;
        }

        if (unit.getSoldierState() != SoldierState.SEEK_COVER) {
            return false;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        LivingEntity target = mob.getTarget();
        Vec3 dangerPos = null;

        if (target != null) {
            dangerPos = target.getEyePosition();

        } else if (unit.getLastKnownTargetPos() != null) {
            dangerPos = unit.getLastKnownTargetPos();
        }

        if (dangerPos == null) return false;
        this.targetCover = findCoverPosition(dangerPos);

        return this.targetCover != null;
    }

    @Override
    public void start() {
        if (this.targetCover == null) return;

        mob.getNavigation().moveTo(
                targetCover.getX() + 0.5,
                targetCover.getY(),
                targetCover.getZ() + 0.5,
                this.speed
        );
    }

    @Override
    public boolean canContinueToUse() {

        if (!unit.isMovementLockedByManager()) {
            abortedByManager = true;
            return false;
        }

        return unit.getSoldierState() == SoldierState.SEEK_COVER
                && this.targetCover != null
                && !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {

        boolean reachedCover = !abortedByManager &&
                this.targetCover != null &&
                mob.blockPosition().closerThan(this.targetCover, 2.5);

        if (reachedCover) {
            unit.setSoldierState(SoldierState.HOLD_COVER);
            unit.setCoverBlock(this.targetCover);
        }

        unit.releaseMovementLock();
        abortedByManager = false;
        this.targetCover = null;
        unit.setCoverSearchOrigin(null);
        this.cooldownTicks = 20;
    }

    @Override
    public void tick() {
        if (this.targetCover != null && mob.getNavigation().isDone()) {
            this.stop();
        }
    }


    @Nullable
    private BlockPos findCoverPosition(Vec3 targetEyes) {
        Level level = mob.level();
        BlockPos overridePos = unit.getCoverSearchOrigin();
        BlockPos origin = (overridePos != null) ? overridePos : mob.blockPosition();


        for (int r = 1; r <= this.searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    BlockPos candidate = origin.offset(dx, 0, dz);
                    BlockState state = level.getBlockState(candidate);

                    if (!state.isSolidRender(level, candidate)) continue;

                    Vec3 blockCenter = Vec3.atCenterOf(candidate);

                    HitResult hit = level.clip(new ClipContext(
                            targetEyes,
                            blockCenter,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            mob
                    ));

                    if (hit.getType() != HitResult.Type.BLOCK) continue;

                    BlockPos standPos = findStandableAdjacent(level, candidate, origin);
                    if (standPos == null) continue;

                    if (mob.getNavigation().createPath(standPos, 1) != null) {
                        return standPos;
                    }
                }
            }
        }
        return null;
    }


    @Nullable
    private BlockPos findStandableAdjacent(Level level, BlockPos coverBlock, BlockPos origin) {

        Vec3 dirToOrigin = Vec3.atCenterOf(origin).subtract(Vec3.atCenterOf(coverBlock));
        Direction preferred = Direction.getNearest(dirToOrigin.x, dirToOrigin.y, dirToOrigin.z);

        if (preferred.getAxis().isHorizontal()) {
            BlockPos p = coverBlock.relative(preferred);
            if (isStandable(level, p)) return p;
        }

        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos p = coverBlock.relative(d);
            if (isStandable(level, p)) return p;
        }
        return null;
    }

    private boolean isStandable(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());

        return feet.isAir() && head.isAir() && floor.isSolidRender(level, pos.below());
    }
}

