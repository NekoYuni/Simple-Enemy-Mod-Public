package net.nekoyuni.SimpleEnemyMod.procedural.events.type;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.EventSpawnConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.DynamicEvent;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;

public class CaveExtractionEvent extends DynamicEvent {

    public CaveExtractionEvent() {
        super("cave_extraction");
    }

    @Override
    public double getBaseChance() {
        return EventSpawnConfig.CAVE_BASE_CHANCE.get();
    }

    @Override
    public double getFailureMultiplier() {
        return EventSpawnConfig.CAVE_FAILURE_MULTIPLIER.get();
    }

    @Override
    public int getMinDistance() {
        return 30;
    }

    @Override
    public int getMaxDistance() {
        return 60;
    }

    @Override
    public boolean canExecute(ServerLevel level, ServerPlayer player) {
        return player.getY() < 60;
    }

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, BlockPos posIgnorada) {

        BlockPos cavePos = findCaveFloorNearPlayer(level, player);
        if (cavePos == null) {
            return false;
        }

        int min = EventSpawnConfig.CAVE_MIN_SIZE.get();
        int max = EventSpawnConfig.CAVE_MAX_SIZE.get();

        boolean isRu = level.random.nextBoolean();
        int squadSize = min + level.random.nextInt(Math.max(1, max - min + 1));

        for (int i = 0; i < squadSize; i++) {
            spawnCaveUnit(level, cavePos, isRu);
        }

        return true;
    }

    private BlockPos findCaveFloorNearPlayer(ServerLevel level, ServerPlayer player) {
        int minDist = getMinDistance();
        int maxDist = getMaxDistance();

        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int dist = minDist + level.random.nextInt(maxDist - minDist);

            int x = player.getBlockX() + (int)(Math.cos(angle) * dist);
            int z = player.getBlockZ() + (int)(Math.sin(angle) * dist);

            int playerY = player.getBlockY();
            for (int dy = 0; dy >= -15; dy--) {
                BlockPos testPos = new BlockPos(x, playerY + dy, z);

                if (level.isEmptyBlock(testPos) &&
                        level.isEmptyBlock(testPos.above()) &&
                        level.getBlockState(testPos.below()).isSolidRender(level, testPos.below()) &&
                        !level.canSeeSky(testPos)) {
                    return testPos;
                }
            }
        }
        return null;
    }

    private void spawnCaveUnit(ServerLevel level, BlockPos basePos, boolean isRu) {
        double dx = (level.random.nextDouble() - 0.5) * 8;
        double dz = (level.random.nextDouble() - 0.5) * 8;

        BlockPos spawnPos = basePos.offset((int)dx, 0, (int)dz);
        spawnPos = new BlockPos(spawnPos.getX(), basePos.getY(), spawnPos.getZ());

        AbstractUnit unit = isRu ? new RUunitEntity(ModEntities.RUUNIT.get(), level) : new USunitEntity(ModEntities.USUNIT.get(), level);

        unit.setRole(UnitRole.DEFAULT);
        unit.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        unit.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

        level.addFreshEntity(unit);
    }
}