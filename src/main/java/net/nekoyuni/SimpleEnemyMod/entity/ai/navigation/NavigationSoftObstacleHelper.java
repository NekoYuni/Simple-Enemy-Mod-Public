package net.nekoyuni.SimpleEnemyMod.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.tactical.TacticalBlockClassifier;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

/**
 * Server-side traversal support for harmless navigation clutter.
 *
 * This is the generic version of the public-branch bandit helper: it is intentionally
 * bound to AbstractUnit, not to bandit/scenario/loot classes.
 */
public final class NavigationSoftObstacleHelper {

    private static final double SIMPLE_LADDER_CLIMB_SPEED = 0.22D;

    private NavigationSoftObstacleHelper() {
        // Utility class.
    }

    public static void tickTraversalSupport(AbstractUnit unit) {
        if (unit.level().isClientSide || !unit.isAlive()) {
            return;
        }

        tickSimpleLadderClimb(unit);

        if (TacticalAiConfig.enableSoftObstacleBreaking()) {
            breakBlockingSoftCover(unit);
        }
    }

    private static void tickSimpleLadderClimb(AbstractUnit unit) {
        if (!TacticalAiConfig.enableLadderFallback()) {
            return;
        }
        if (!isTouchingOrFacingLadder(unit)) {
            return;
        }

        boolean wantsToMoveUp = unit.horizontalCollision || hasHigherNavigationTarget(unit);
        if (!wantsToMoveUp) {
            return;
        }

        Vec3 motion = unit.getDeltaMovement();
        unit.setDeltaMovement(motion.x, Math.max(motion.y, SIMPLE_LADDER_CLIMB_SPEED), motion.z);
        unit.fallDistance = 0.0F;
    }

    private static boolean isTouchingOrFacingLadder(AbstractUnit unit) {
        BlockPos feet = unit.blockPosition();
        if (isLadder(unit.level().getBlockState(feet)) || isLadder(unit.level().getBlockState(feet.above()))) {
            return true;
        }

        Direction facing = unit.getDirection();
        BlockPos front = feet.relative(facing);
        return isLadder(unit.level().getBlockState(front)) || isLadder(unit.level().getBlockState(front.above()));
    }

    private static boolean hasHigherNavigationTarget(AbstractUnit unit) {
        if (unit.getNavigation().isDone() || unit.getNavigation().getTargetPos() == null) {
            return false;
        }

        return unit.getNavigation().getTargetPos().getY() > unit.blockPosition().getY();
    }

    private static boolean isLadder(BlockState state) {
        return state.is(Blocks.LADDER);
    }

    private static void breakBlockingSoftCover(AbstractUnit unit) {
        Level level = unit.level();
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        Path path = unit.getNavigation().getPath();
        boolean hasActivePath = path != null && !path.isDone();
        boolean isActivelyBlocked = unit.horizontalCollision;

        // Do not break nearby soft blocks just because the unit is idle and looking at them.
        if (!hasActivePath && !isActivelyBlocked) {
            return;
        }

        BlockPos feet = unit.blockPosition();
        Direction direction = getMovementDirection(unit);

        if (hasActivePath) {
            BlockPos nextNode = path.getNextNodePos();
            tryBreak(level, nextNode, unit);
            tryBreak(level, nextNode.above(), unit);
        }

        tryBreak(level, feet, unit);
        tryBreak(level, feet.above(), unit);

        BlockPos front = feet.relative(direction);
        tryBreak(level, front, unit);
        tryBreak(level, front.above(), unit);

        if (isActivelyBlocked) {
            BlockPos secondFront = front.relative(direction);
            tryBreak(level, secondFront, unit);
            tryBreak(level, secondFront.above(), unit);
        }
    }

    private static Direction getMovementDirection(AbstractUnit unit) {
        Vec3 motion = unit.getDeltaMovement();
        if (Math.abs(motion.x) > 0.01D || Math.abs(motion.z) > 0.01D) {
            return Direction.getNearest(motion.x, 0.0D, motion.z);
        }
        return unit.getDirection();
    }

    private static void tryBreak(Level level, BlockPos pos, AbstractUnit unit) {
        BlockState state = level.getBlockState(pos);
        if (!isBreakableNavigationSoftCover(state)) {
            return;
        }

        level.destroyBlock(pos, false, unit);
        unit.getNavigation().recomputePath();
    }

    public static boolean isBreakableNavigationSoftCover(BlockState state) {
        return TacticalBlockClassifier.isBreakableNavigationSoftCover(state);
    }

    public static boolean isGlassLike(BlockState state) {
        return TacticalBlockClassifier.isGlassLike(state);
    }
}
