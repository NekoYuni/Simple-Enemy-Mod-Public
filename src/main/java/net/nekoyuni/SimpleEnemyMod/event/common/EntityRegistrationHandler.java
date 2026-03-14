package net.nekoyuni.SimpleEnemyMod.event.common;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;



@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistrationHandler {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {

        event.put(ModEntities.USUNIT.get(), USunitEntity.createAttributes().build());
        event.put(ModEntities.RUUNIT.get(), RUunitEntity.createAttributes().build());
        event.put(ModEntities.PMCUNIT.get(), PmcUnitEntity.createAttributes().build());

    }

}
