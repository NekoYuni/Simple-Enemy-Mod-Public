package net.nekoyuni.SimpleEnemyMod.procedural.events.type;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobSpawnType;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.DynamicEvent;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import net.nekoyuni.SimpleEnemyMod.spawn.utils.SpawnHelper;

public class PatrolEvent extends DynamicEvent {

    public PatrolEvent() {
        super("military_patrol");
    }

    @Override
    public double getBaseChance() {
        return CommonConfig.PATROL_BASE_CHANCE.get();
    }

    @Override
    public double getFailureMultiplier() {
        return CommonConfig.PATROL_FAILURE_MULTIPLIER.get();
    }

    @Override
    public int getMinDistance() {
        return 60;
    }

    @Override
    public int getMaxDistance() {
        return 110;
    }

    @Override
    public double getBiomeModifier(ServerLevel level, BlockPos pos) {
        var biomeHolder = level.getBiome(pos);

        if (biomeHolder.is(BiomeTags.IS_FOREST) || biomeHolder.is(BiomeTags.IS_MOUNTAIN)) {
            return 2.0;
        }

        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            return 0.0;
        }

        return 1.0;
    }

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, BlockPos pos) {

        if (!SpawnHelper.isValidSpawn(level, pos)) {
            return false;
        }

        boolean isRuFaction = level.random.nextBoolean();
        int squadSize = 3 + level.random.nextInt(2);

        for (int i = 0; i < squadSize; i++) {
            spawnSoldier(level, pos, isRuFaction, false);
        }

        spawnSoldier(level, pos, isRuFaction, true);

        return true;
    }

    private void spawnSoldier(ServerLevel level, BlockPos basePos, boolean isRu, boolean isLeader) {

        double offsetX = (level.random.nextDouble() - 0.5) * 4;
        double offsetZ = (level.random.nextDouble() - 0.5) * 4;

        BlockPos spawnPos = basePos.offset((int)offsetX, 0, (int)offsetZ);
        spawnPos = new BlockPos(spawnPos.getX(), basePos.getY(), spawnPos.getZ());

        AbstractUnit unit;

        if (isRu) {
            unit = new RUunitEntity(ModEntities.RUUNIT.get(), level);
        } else {
            unit = new USunitEntity(ModEntities.USUNIT.get(), level);
        }

        if (isLeader) {
            unit.setRole(UnitRole.SQUAD_LEADER);
        } else {
            unit.setRole(UnitRole.SQUAD_UNIT);
        }

        unit.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        unit.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

        level.addFreshEntity(unit);
    }
}
