package net.nekoyuni.SimpleEnemyMod.entity.ai.tactical;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.Locale;

/**
 * Central block classification for neutral tactical AI helpers.
 *
 * Visibility, traversal and cover logic must agree on what is glass, foliage,
 * fragile navigation clutter, real cover and environmental hazard.
 */
public final class TacticalBlockClassifier {

    private TacticalBlockClassifier() {
        // Utility class.
    }

    public static boolean isVisionTransparentClutter(BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (isGlassLike(state)) {
            return true;
        }

        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)) {
            return true;
        }

        return isFragilePlantLike(state);
    }

    public static boolean isBreakableNavigationSoftCover(BlockState state) {
        return isGlassLike(state)
                || state.is(BlockTags.LEAVES)
                || isFragilePlantLike(state);
    }

    /**
     * Soft cover can conceal or obstruct movement, but it must not be scored as real ballistic cover.
     */
    public static boolean isSoftCover(BlockState state) {
        return isVisionTransparentClutter(state) || isBreakableNavigationSoftCover(state);
    }

    public static boolean isHardCover(BlockState state, Level level, BlockPos pos) {
        if (state.isAir() || isSoftCover(state) || isHazardousState(state)) {
            return false;
        }

        return state.isSolidRender(level, pos) || !state.getCollisionShape(level, pos).isEmpty();
    }

    public static double hardCoverStrength(BlockState state, Level level, BlockPos pos) {
        if (!isHardCover(state, level, pos)) {
            return 0.0D;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.ANCIENT_DEBRIS)) {
            return 1.0D;
        }
        if (state.isSolidRender(level, pos)) {
            return 0.82D;
        }
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return 0.58D;
        }
        return 0.20D;
    }

    public static boolean isHazardousState(BlockState state) {
        return state.is(Blocks.LAVA)
                || state.getFluidState().is(FluidTags.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean isGlassLike(BlockState state) {
        String descriptionId = descriptionId(state);
        return descriptionId.contains("glass") || descriptionId.contains("pane");
    }

    public static boolean isFragilePlantLike(BlockState state) {
        Block block = state.getBlock();
        String descriptionId = descriptionId(state);
        return block == Blocks.GRASS
                || block == Blocks.TALL_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.DEAD_BUSH
                || block == Blocks.VINE
                || block == Blocks.CAVE_VINES
                || block == Blocks.CAVE_VINES_PLANT
                || block == Blocks.GLOW_LICHEN
                || block == Blocks.HANGING_ROOTS
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.SEAGRASS
                || block == Blocks.TALL_SEAGRASS
                || block == Blocks.KELP
                || block == Blocks.KELP_PLANT
                || descriptionId.endsWith(".grass")
                || descriptionId.contains("tall_grass")
                || descriptionId.contains("fern")
                || descriptionId.contains("dead_bush")
                || descriptionId.contains("vine")
                || descriptionId.contains("lichen");
    }

    private static String descriptionId(BlockState state) {
        return state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
    }
}
