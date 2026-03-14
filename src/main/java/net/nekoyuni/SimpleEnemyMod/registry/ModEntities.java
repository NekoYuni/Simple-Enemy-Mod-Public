package net.nekoyuni.SimpleEnemyMod.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SimpleEnemyMod.MODID);


    public static final RegistryObject<EntityType<USunitEntity>> USUNIT =
            ENTITY_TYPES.register("usunit",
                    () -> EntityType.Builder.of(USunitEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(128)
                            .build("usunit"));

    public static final RegistryObject<EntityType<RUunitEntity>> RUUNIT =
            ENTITY_TYPES.register("ruunit",
                    () -> EntityType.Builder.of(RUunitEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(128)
                            .build("ruunit"));

    public static final RegistryObject<EntityType<PmcUnitEntity>> PMCUNIT =
            ENTITY_TYPES.register("pmcunit",
                    () -> EntityType.Builder.of(PmcUnitEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(128)
                            .build("pmcunit"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
