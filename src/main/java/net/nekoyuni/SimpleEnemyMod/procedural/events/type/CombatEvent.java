package net.nekoyuni.SimpleEnemyMod.procedural.events.type;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.procedural.events.system.DynamicEvent;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import net.nekoyuni.SimpleEnemyMod.spawn.utils.SpawnHelper;

public class CombatEvent extends DynamicEvent {

    public CombatEvent() {
        super("far_combat");
    }

    @Override
    public double getBaseChance() {
        return CommonConfig.COMBAT_BASE_CHANCE.get();
    }

    @Override
    public double getFailureMultiplier() {
        return CommonConfig.COMBAT_FAILURE_MULTIPLIER.get();
    }

    @Override
    public int getMinDistance() {
        return 70;
    }

    @Override
    public int getMaxDistance() {
        return 120;
    }

    @Override
    public double getBiomeModifier(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);

        if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.HAS_DESERT_PYRAMID)) {
            return 1.5;
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return 0.8;
        }
        return 1.0;
    }

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, BlockPos centerPos) {
        if (!SpawnHelper.isValidSpawn(level, centerPos)) return false;

        int squadSize = 3 + level.random.nextInt(3);
        int separation = 24;

        BlockPos posRu = centerPos.offset(separation, 0, 0);
        BlockPos posUs = centerPos.offset(-separation, 0, 0);

        posRu = adjustHeight(level, posRu);
        posUs = adjustHeight(level, posUs);

        for (int i = 0; i < squadSize; i++) {
            spawnUnit(level, posRu, true);
        }

        for (int i = 0; i < squadSize; i++) {
            spawnUnit(level, posUs, false);
        }

        level.playSound(null, centerPos, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 2.0F, 0.8F);

        return true;
    }

    private void spawnUnit(ServerLevel level, BlockPos basePos, boolean isRu) {
        double dx = (level.random.nextDouble() - 0.5) * 6;
        double dz = (level.random.nextDouble() - 0.5) * 6;

        BlockPos spawnPos = basePos.offset((int)dx, 0, (int)dz);
        spawnPos = new BlockPos(spawnPos.getX(), basePos.getY(), spawnPos.getZ());

        AbstractUnit unit;
        if (isRu) {
            unit = new RUunitEntity(ModEntities.RUUNIT.get(), level);
        } else {
            unit = new USunitEntity(ModEntities.USUNIT.get(), level);
        }

        unit.setRole(UnitRole.DEFAULT);
        unit.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

        unit.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);

        level.addFreshEntity(unit);
    }

    private BlockPos adjustHeight(ServerLevel level, BlockPos pos) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());

        return new BlockPos(pos.getX(), y, pos.getZ());
    }
}
