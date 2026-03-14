package net.nekoyuni.SimpleEnemyMod.spawn.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class SpawnHelper {

    public static BlockPos getRandomPositionNearPlayer(ServerPlayer player, int minDistance, int maxDistance, ServerLevel world) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = minDistance + Math.random() * (maxDistance - minDistance);

        int spawnX = player.getBlockX() + (int)(Math.cos(angle) * distance);
        int spawnZ = player.getBlockZ() + (int)(Math.sin(angle) * distance);
        int spawnY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);

        return new BlockPos(spawnX, spawnY, spawnZ);
    }

    public static boolean isValidSpawn(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos.below());

        return state.isSolid() && world.getBrightness(LightLayer.BLOCK, pos) < 10;
    }
}
