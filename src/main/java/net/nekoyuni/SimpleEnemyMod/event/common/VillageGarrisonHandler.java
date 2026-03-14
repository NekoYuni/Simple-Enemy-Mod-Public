package net.nekoyuni.SimpleEnemyMod.event.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillageGarrisonHandler {

    private static final List<PendingGarrison> PENDING_GARRISONS = new ArrayList<>();


    private static class PendingGarrison {
        final ServerLevel level;
        final BlockPos pos;
        int delayTicks;

        PendingGarrison(ServerLevel level, BlockPos pos, int delayTicks) {
            this.level = level;
            this.pos = pos;
            this.delayTicks = delayTicks;
        }
    }

    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {

        if (!CommonConfig.enableVillageGarrison) {
            return;
        }

        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Villager villager)) {
            return;
        }

        CompoundTag persistentData = villager.getPersistentData();
        if (persistentData.getBoolean("sem_garrison_checked")) {
            return;
        }
        persistentData.putBoolean("sem_garrison_checked", true);

        PENDING_GARRISONS.add(new PendingGarrison((ServerLevel) event.getLevel(), villager.blockPosition(), 100));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;

        ServerLevel currentLevel = (ServerLevel) event.level;

        Iterator<PendingGarrison> iterator = PENDING_GARRISONS.iterator();
        while (iterator.hasNext()) {
            PendingGarrison task = iterator.next();

            if (task.level != currentLevel) continue;

            task.delayTicks--;

            if (task.delayTicks <= 0) {
                iterator.remove();

                BlockPos farCorner = task.pos.offset(40, 0, 40);
                if (!currentLevel.isLoaded(farCorner) || !currentLevel.isLoaded(task.pos)) {
                    continue;
                }

                List<AbstractUnit> guardsNearby = currentLevel.getEntitiesOfClass(
                        AbstractUnit.class,
                        new AABB(task.pos).inflate(40.0)
                );

                if (!guardsNearby.isEmpty()) {
                    continue;
                }

                boolean isRu = currentLevel.random.nextBoolean();
                int squadSize = 2 + currentLevel.random.nextInt(3);

                for (int i = 0; i < squadSize; i++) {
                    spawnGuard(currentLevel, task.pos, isRu);
                }
            }
        }
    }

    private static void spawnGuard(ServerLevel level, BlockPos basePos, boolean isRu) {
        double dx = (level.random.nextDouble() - 0.5) * 8;
        double dz = (level.random.nextDouble() - 0.5) * 8;
        BlockPos spawnPos = basePos.offset((int)dx, 0, (int)dz);

        if (!level.isLoaded(spawnPos)) return;

        int maxIntentos = 20;
        while (level.isEmptyBlock(spawnPos.below()) && spawnPos.getY() > level.getMinBuildHeight() && maxIntentos > 0) {
            spawnPos = spawnPos.below();
            maxIntentos--;
        }

        AbstractUnit unit = isRu ? new RUunitEntity(ModEntities.RUUNIT.get(), level) : new USunitEntity(ModEntities.USUNIT.get(), level);
        unit.setRole(UnitRole.DEFAULT);
        unit.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        unit.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(unit);
    }
}