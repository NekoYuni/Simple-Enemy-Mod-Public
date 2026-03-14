package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class PeekFromCoverGoal extends Goal {

    private final AbstractUnit unit;
    private final Mob mob;
    private final double peekSpeed;

    private Vec3 peekPosition;
    private int peekTicks;
    private int cooldownTicks;

    private static final int MAX_PEEK_TICKS = 18;
    private static final int PEEK_COOLDOWN = 25;
    private static final int MAX_SCAN_DISTANCE = 4;
    private static final int LATERAL_RANGE = 4;

    public PeekFromCoverGoal(AbstractUnit unit, double speed) {
        this.unit = unit;
        this.mob = unit;
        this.peekSpeed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (unit.getSoldierState() != SoldierState.HOLD_COVER) return false;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (mob.hasLineOfSight(target)) return false;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        BlockPos cover = unit.getCoverBlock();
        if (cover == null) return false;

        this.peekPosition = findFlexiblePeekSpot(target, cover);
        return this.peekPosition != null;
    }

    @Override
    public void start() {
        if (peekPosition == null) return;
        this.peekTicks = 0;

        mob.getNavigation().moveTo(
                peekPosition.x + 0.5,
                peekPosition.y,
                peekPosition.z + 0.5,
                peekSpeed
        );
    }

    @Override
    public boolean canContinueToUse() {
        if (unit.getSoldierState() != SoldierState.HOLD_COVER) return false;

        return peekTicks < MAX_PEEK_TICKS;
    }

    @Override
    public void tick() {
        peekTicks++;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        mob.getLookControl().setLookAt(target, 30f, 30f);

        if (mob.hasLineOfSight(target)) {
            stop();
            return;
        }

        if (mob.getNavigation().isDone()) {
            if (peekTicks > MAX_PEEK_TICKS / 2) {
                stop();
            }
        }
    }

    @Override
    public void stop() {
        BlockPos cover = unit.getCoverBlock();
        if (cover != null) {
            mob.getNavigation().moveTo(
                    cover.getX() + 0.5,
                    cover.getY(),
                    cover.getZ() + 0.5,
                    peekSpeed
            );
        }

        peekPosition = null;
        peekTicks = 0;
        cooldownTicks = PEEK_COOLDOWN;
    }

    @Nullable
    private Vec3 findFlexiblePeekSpot(LivingEntity target, BlockPos coverBlock) {

        Level level = mob.level();
        Vec3 targetEyes = target.getEyePosition();
        Vec3 coverCenter = Vec3.atCenterOf(coverBlock);

        Vec3 toTarget = target.position().subtract(coverCenter).normalize();
        if (Double.isNaN(toTarget.x) || Double.isNaN(toTarget.z))
            toTarget = new Vec3(0, 0, -1);

        Vec3 lateral = new Vec3(-toTarget.z, 0, toTarget.x).normalize();

        int[] sideOffsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
        for (int side : sideOffsets) {
            if (Math.abs(side) > LATERAL_RANGE) continue;

            Vec3 latOffset = lateral.scale(side);

            for (int dist = 1; dist <= MAX_SCAN_DISTANCE; dist++) {

                Vec3 candidate = coverCenter
                        .add(latOffset)
                        .add(toTarget.scale(dist * 0.5));

                BlockPos foot = BlockPos.containing(candidate);

                if (!isSafe(level, foot)) continue;

                Vec3 eye = Vec3.atCenterOf(foot).add(0, mob.getEyeHeight(), 0);
                HitResult hit = level.clip(new ClipContext(
                        eye, targetEyes,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        mob
                ));

                if (hit.getType() == HitResult.Type.BLOCK) continue;

                if (mob.getNavigation().createPath(foot, 1) != null) {
                    return new Vec3(foot.getX(), foot.getY(), foot.getZ());
                }
            }
        }

        return null;
    }

    private boolean isSafe(Level level, BlockPos pos) {
        BlockPos below = pos.below();

        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(below).isSolidRender(level, below);
    }
}

