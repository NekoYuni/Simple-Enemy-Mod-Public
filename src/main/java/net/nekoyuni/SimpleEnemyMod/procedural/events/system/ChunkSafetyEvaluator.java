package net.nekoyuni.SimpleEnemyMod.procedural.events.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkSafetyEvaluator {

    private static final int SAFETY_THRESHOLD = 6;
    private static final int SCAN_RADIUS = 24;
    private static final int SCAN_STEP_XZ = 3;
    private static final int SCAN_STEP_Y = 2;
    private static final int SCAN_HALF_Y = 8;
    private static final int MIN_DIST_FROM_ANY_PLAYER = 32;


    public static boolean isPlayerBase(ServerLevel level, BlockPos targetPos) {

        for (ServerPlayer player : level.players()) {
            double distSq = player.blockPosition().distSqr(targetPos);

            if (distSq < MIN_DIST_FROM_ANY_PLAYER * MIN_DIST_FROM_ANY_PLAYER) {
                return true;
            }
        }

        return hasBaseBlocks(level, targetPos);
    }

    private static boolean hasBaseBlocks(ServerLevel level, BlockPos targetPos) {
        int score = 0;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x += SCAN_STEP_XZ) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z += SCAN_STEP_XZ) {
                for (int y = -SCAN_HALF_Y; y <= SCAN_HALF_Y; y += SCAN_STEP_Y) {
                    BlockState state = level.getBlockState(targetPos.offset(x, y, z));
                    score += scoreBlock(state);
                    if (score >= SAFETY_THRESHOLD) return true;
                }
            }
        }

        return false;
    }

    private static int scoreBlock(BlockState state) {

        if (state.is(Blocks.RED_BED) ||
                state.is(Blocks.WHITE_BED) ||
                state.is(Blocks.BLUE_BED) ||
                state.is(Blocks.GREEN_BED) ||
                state.is(Blocks.YELLOW_BED) ||
                state.is(Blocks.BLACK_BED) ||
                state.is(Blocks.CYAN_BED) ||
                state.is(Blocks.GRAY_BED) ||
                state.is(Blocks.LIGHT_BLUE_BED) ||
                state.is(Blocks.LIGHT_GRAY_BED) ||
                state.is(Blocks.LIME_BED) ||
                state.is(Blocks.MAGENTA_BED) ||
                state.is(Blocks.ORANGE_BED) ||
                state.is(Blocks.PINK_BED) ||
                state.is(Blocks.PURPLE_BED) ||
                state.is(Blocks.BROWN_BED)) {
            return 5;
        }

        if (state.is(Blocks.CRAFTING_TABLE))   return 3;

        if (state.is(Blocks.ENCHANTING_TABLE)) return 3;

        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)) return 2;

        if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) return 2;

        if (state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)) return 2;

        if (state.is(Blocks.OAK_DOOR) || state.is(Blocks.SPRUCE_DOOR) || state.is(Blocks.BIRCH_DOOR) ||
                state.is(Blocks.JUNGLE_DOOR) || state.is(Blocks.ACACIA_DOOR) || state.is(Blocks.DARK_OAK_DOOR) ||
                state.is(Blocks.MANGROVE_DOOR) || state.is(Blocks.CHERRY_DOOR) || state.is(Blocks.IRON_DOOR)) return 2;

        if (state.is(Blocks.BOOKSHELF)) return 1;

        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) return 1;

        return 0;
    }
}
