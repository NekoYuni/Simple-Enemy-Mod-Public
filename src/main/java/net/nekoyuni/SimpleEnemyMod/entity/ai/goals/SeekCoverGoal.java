package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.LadderRouteNavigator;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.NavigationSoftObstacleHelper;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.TacticalHazardHelper;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.TacticalNavigationHelper;

import javax.annotation.Nullable;
import java.util.EnumSet;


public class SeekCoverGoal extends Goal {

    private final AbstractUnit unit;
    private final Mob mob;
    private final double speed;
    private final int searchRadius;
    private int cooldownTicks = 0;
    private boolean abortedByManager = false;

    // Backward strafe system
    private boolean isBackwardCover = false;

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

        if (isVerticalTransitionInProgress()) {
            this.cooldownTicks = 30;
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

    private boolean isVerticalTransitionInProgress() {
        if (mob.onClimbable() || !mob.onGround()) {
            return true;
        }
        String ladderState = LadderRouteNavigator.debugState(unit);
        return ladderState != null && !"IDLE".equals(ladderState) && !ladderState.startsWith("FAILED_COOLDOWN");
    }

    @Override
    public void start() {
        if (this.targetCover == null) return;

        // Calculamos si la cobertura está a la espalda del NPC
        this.isBackwardCover = isCoverBehindUnit();

        if (this.isBackwardCover) {
            // Cobertura a la espalda: detenemos pathfinding, el tick() maneja el strafe
            mob.getNavigation().stop();
        } else {
            // Cobertura al frente o lado: pathfinding normal
            TacticalNavigationHelper.moveTo(mob,
                    targetCover.getX() + 0.5,
                    targetCover.getY(),
                    targetCover.getZ() + 0.5,
                    this.speed
            );
        }
    }

    @Override
    public boolean canContinueToUse() {

        if (!unit.isMovementLockedByManager()) {
            abortedByManager = true;
            return false;
        }

        if (unit.getSoldierState() != SoldierState.SEEK_COVER || this.targetCover == null) {
            return false;
        }

        // Si es backward strafe, chequeamos distancia manualmente
        if (this.isBackwardCover) {
            return !mob.blockPosition().closerThan(this.targetCover, 1.5);
        }

        return !mob.getNavigation().isDone();
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

        // Limpiamos estado del strafe
        this.isBackwardCover = false;

        unit.releaseMovementLock();
        abortedByManager = false;
        this.targetCover = null;
        unit.setCoverSearchOrigin(null);
        this.cooldownTicks = 20;
    }

    @Override
    public void tick() {

        if (this.isBackwardCover) {
            tickBackwardStrafe();
            return;
        }

        // Comportamiento original para cobertura al frente/lado
        if (this.targetCover != null && mob.getNavigation().isDone()) {
            this.stop();
        }
    }

    /**
     * Maneja el retroceso táctico cuando la cobertura está a la espalda.
     * El NPC mantiene la mirada al target y retrocede usando strafe.
     */
    private void tickBackwardStrafe() {
        if (this.targetCover == null) return;

        LivingEntity target = mob.getTarget();

        // Mantener mirada al target mientras retrocedemos
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // Aplicar movimiento de retroceso (forward = -1 = hacia atrás relativo a donde mira)
        mob.getMoveControl().strafe((float) -this.speed, 0.0f);

        // Verificar llegada por distancia
        if (mob.blockPosition().closerThan(this.targetCover, 1.5)) {
            this.stop();
        }
    }

    /**
     * Determina si la cobertura calculada está a la espalda del NPC.
     * Compara la dirección que mira el NPC (hacia el target) con la dirección hacia la cobertura.
     * Si el ángulo entre ambas supera 120°, la cobertura está a sus espaldas.
     */
    private boolean isCoverBehindUnit() {
        if (this.targetCover == null) return false;

        // Dirección hacia donde mira el NPC (hacia el target o su yRot actual)
        LivingEntity target = mob.getTarget();
        Vec3 lookDir;

        if (target != null) {
            lookDir = new Vec3(
                    target.getX() - mob.getX(),
                    0,
                    target.getZ() - mob.getZ()
            ).normalize();
        } else {
            // Sin target, usamos la rotación actual del NPC
            float yaw = mob.getYRot() * (float) (Math.PI / 180.0);
            lookDir = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        }

        // Dirección hacia la cobertura
        Vec3 coverDir = new Vec3(
                targetCover.getX() + 0.5 - mob.getX(),
                0,
                targetCover.getZ() + 0.5 - mob.getZ()
        ).normalize();

        // Producto punto: 1 = misma dirección, -1 = opuesta, 0 = perpendicular
        double dot = lookDir.dot(coverDir);

        // dot < -0.5 equivale a un ángulo > 120° → cobertura a la espalda
        return dot < -0.5;
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

                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(candidate);

                        if (state.getCollisionShape(level, candidate).isEmpty()) continue;
                        if (TacticalHazardHelper.isHazardousState(state)) continue;
                        if (NavigationSoftObstacleHelper.isBreakableNavigationSoftCover(state)) continue;

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

        return TacticalHazardHelper.isSafeStandable(level, pos);
    }
}

