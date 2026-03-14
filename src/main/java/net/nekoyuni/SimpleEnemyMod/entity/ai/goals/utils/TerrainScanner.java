package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TerrainScanner {

    public enum FormationType {
        COLUMN,
        WEDGE
    }

    /**
     * Checks if there's a wall structure within the specified range
     * A wall is defined as a solid structure at least 3 blocks wide and 2 blocks high
     *
     * @param world The world to scan
     * @param center The center position to scan from
     * @param range The scanning range (recommended: 5 blocks)
     * @return true if a wall structure is found, false otherwise
     */
    public static boolean isWall(Level world, BlockPos center, int range) {

        for (int x = -range; x <= range; x += 2) {

            for (int z = -range; z <= range; z += 2) {
                BlockPos scanPos = center.offset(x, 0, z);

                if (hasWallStructure(world, scanPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines the appropriate formation type based on terrain
     *
     * @param world The world to scan
     * @param leaderPos The squad leader's position
     * @param scanRange The range to scan for terrain features
     * @return The recommended formation type
     */
    public static FormationType getFormationType(Level world, BlockPos leaderPos, int scanRange) {
        if (isWall(world, leaderPos, scanRange)) {
            return FormationType.COLUMN;
        } else {
            return FormationType.WEDGE;
        }
    }

    /**
     * Checks if a specific position has a wall structure (3 wide x 2 high)
     *
     * @param world The world to check
     * @param pos The position to check
     * @return true if wall structure exists
     */
    private static boolean hasWallStructure(Level world, BlockPos pos) {

        int solidBlocks = 0;
        int requiredBlocks = 6;


        for (int width = 0; width < 3; width++) {

            for (int height = 0; height < 2; height++) {
                BlockPos checkPos = pos.offset(width, height, 0);

                if (isSolidBlock(world, checkPos)) {
                    solidBlocks++;
                }
            }
        }


        if (solidBlocks < requiredBlocks) {
            solidBlocks = 0;

            for (int depth = 0; depth < 3; depth++) {

                for (int height = 0; height < 2; height++) {
                    BlockPos checkPos = pos.offset(0, height, depth);

                    if (isSolidBlock(world, checkPos)) {
                        solidBlocks++;
                    }

                }
            }
        }

        return solidBlocks >= requiredBlocks;
    }

    /**
     * Checks if a block is considered solid for formation purposes
     *
     * @param world The world
     * @param pos The position to check
     * @return true if the block is solid
     */
    private static boolean isSolidBlock(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);


        return !state.isAir() &&
                state.isSolidRender(world, pos) &&
                state.getFluidState().isEmpty();
    }

    /**
     * Gets the scan interval in ticks (6 seconds = 120 ticks)
     *
     * @return scan interval in game ticks
     */
    public static int getScanInterval() {
        return 100;
    }
}
