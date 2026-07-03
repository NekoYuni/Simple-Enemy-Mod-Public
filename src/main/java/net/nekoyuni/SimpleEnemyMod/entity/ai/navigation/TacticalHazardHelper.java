package net.nekoyuni.SimpleEnemyMod.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.tactical.TacticalBlockClassifier;

/**
 * Shared hazard checks for tactical navigation and cover selection.
 */
public final class TacticalHazardHelper {

    private TacticalHazardHelper() {
        // Utility class.
    }

    public static boolean isHazardous(Level level, BlockPos pos) {
        return isHazardousState(level.getBlockState(pos));
    }

    public static boolean isHazardousState(BlockState state) {
        return TacticalAiConfig.enableHazardousCoverRejection()
                && TacticalBlockClassifier.isHazardousState(state);
    }

    public static boolean isSafeStandable(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());

        if (isHazardousState(feet) || isHazardousState(head) || isHazardousState(floor)) {
            return false;
        }

        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
