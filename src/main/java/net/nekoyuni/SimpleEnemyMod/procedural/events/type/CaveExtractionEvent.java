package net.nekoyuni.SimpleEnemyMod.procedural.events.type;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
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
        return CommonConfig.CAVE_BASE_CHANCE.get();
    }

    @Override
    public double getFailureMultiplier() {
        return CommonConfig.CAVE_FAILURE_MULTIPLIER.get();
    }

    @Override
    public int getMinDistance() {
        return 15;
    }

    @Override
    public int getMaxDistance() {
        return 35;
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

        boolean isRu = level.random.nextBoolean();
        int squadSize = 2 + level.random.nextInt(3);

        for (int i = 0; i < squadSize; i++) {
            spawnCaveUnit(level, cavePos, isRu);
        }

        return true;
    }

    private BlockPos findCaveFloorNearPlayer(ServerLevel level, ServerPlayer player) {
        int radius = getMaxDistance();

        for (int i = 0; i < 15; i++) {

            int x = player.getBlockX() + level.random.nextInt(radius * 2) - radius;
            int z = player.getBlockZ() + level.random.nextInt(radius * 2) - radius;

            int y = player.getBlockY() + level.random.nextInt(20) - 10;

            BlockPos testPos = new BlockPos(x, y, z);

            if (level.isEmptyBlock(testPos) &&
                    level.isEmptyBlock(testPos.above()) &&
                    level.getBlockState(testPos.below()).isSolidRender(level, testPos.below()) &&
                    !level.canSeeSky(testPos)) {

                return testPos;
            }
        }
        return null;
    }

    private void spawnCaveUnit(ServerLevel level, BlockPos basePos, boolean isRu) {
        double dx = (level.random.nextDouble() - 0.5) * 2;
        double dz = (level.random.nextDouble() - 0.5) * 2;

        BlockPos spawnPos = basePos.offset((int)dx, 0, (int)dz);
        spawnPos = new BlockPos(spawnPos.getX(), basePos.getY(), spawnPos.getZ());

        AbstractUnit unit = isRu ? new RUunitEntity(ModEntities.RUUNIT.get(), level) : new USunitEntity(ModEntities.USUNIT.get(), level);

        unit.setRole(UnitRole.DEFAULT);
        unit.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        unit.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

        level.addFreshEntity(unit);
    }
}