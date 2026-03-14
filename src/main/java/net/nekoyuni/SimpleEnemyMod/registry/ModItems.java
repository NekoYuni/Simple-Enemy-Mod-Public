package net.nekoyuni.SimpleEnemyMod.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.item.RoleSpawnEggItem;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;

public class ModItems {


    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SimpleEnemyMod.MODID);


    private static final int COLOR_US_UNIT = 0x776340;
    private static final int COLOR_US_SQUAD_LEADER = 0x605532;
    private static final int COLOR_US_SQUAD_UNIT = 0x6b5a3a;

    private static final int COLOR_RU_UNIT = 0x585645;
    private static final int COLOR_RU_SQUAD_LEADER = 0x403D2A;
    private static final int COLOR_RU_SQUAD_UNIT = 0x21201D;

    private static final int COLOR_PMC_UNIT = 0x5B533F;


    // US UNIT SPAWN EGGS
    public static final RegistryObject<Item> US_UNIT_SPAWN_EGG = ITEMS.register("us_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.USUNIT.get(),
                    COLOR_US_UNIT,
                    0x5c4e36,
                    new Item.Properties().stacksTo(64),
                    UnitRole.DEFAULT
            )
    );

    public static final RegistryObject<Item> US_SQUAD_LEADER_SPAWN_EGG = ITEMS.register("us_squad_leader_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.USUNIT.get(),
                    COLOR_US_SQUAD_LEADER,
                    0x8a7a4f,
                    new Item.Properties().stacksTo(64),
                    UnitRole.SQUAD_LEADER
            )
    );

    public static final RegistryObject<Item> US_SQUAD_UNIT_SPAWN_EGG = ITEMS.register("us_squad_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.USUNIT.get(),
                    COLOR_US_SQUAD_UNIT,
                    0x4a3f29,
                    new Item.Properties().stacksTo(64),
                    UnitRole.SQUAD_UNIT
            )
    );


    // RU UNIT SPAWN EGGS
    public static final RegistryObject<Item> RU_UNIT_SPAWN_EGG = ITEMS.register("ru_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.RUUNIT.get(),
                    COLOR_RU_UNIT,
                    0x302F24,
                    new Item.Properties().stacksTo(64),
                    UnitRole.DEFAULT
            )
    );

    public static final RegistryObject<Item> RU_SQUAD_LEADER_SPAWN_EGG = ITEMS.register("ru_squad_leader_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.RUUNIT.get(),
                    COLOR_RU_SQUAD_LEADER,
                    0x8B886A,
                    new Item.Properties().stacksTo(64),
                    UnitRole.SQUAD_LEADER
            )
    );

    public static final RegistryObject<Item> RU_SQUAD_UNIT_SPAWN_EGG = ITEMS.register("ru_squad_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.RUUNIT.get(),
                    COLOR_RU_SQUAD_UNIT,
                    0x78755A,
                    new Item.Properties().stacksTo(64),
                    UnitRole.SQUAD_UNIT
            )
    );


    // PMC UNIT EGGS
    public static final RegistryObject<Item> PMC_UNIT_SPAWN_EGG = ITEMS.register("pmc_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.PMCUNIT.get(),
                    COLOR_PMC_UNIT,
                    0x252B1F,
                    new Item.Properties().stacksTo(64),
                    UnitRole.FRIENDLY_DEFAULT
            )
    );

    public static final RegistryObject<Item> PMC_SQUAD_LEADER_SPAWN_EGG = ITEMS.register("pmc_squad_leader_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.PMCUNIT.get(),
                    COLOR_PMC_UNIT,
                    0x767860,
                    new Item.Properties().stacksTo(64),
                    UnitRole.FRIENDLY_SQUAD_LEADER
            )
    );

    public static final RegistryObject<Item> PMC_SQUAD_UNIT_SPAWN_EGG = ITEMS.register("pmc_squad_unit_spawn_egg",
            () -> new RoleSpawnEggItem(
                    () -> ModEntities.PMCUNIT.get(),
                    COLOR_PMC_UNIT,
                    0x767860,
                    new Item.Properties().stacksTo(64),
                    UnitRole.FRIENDLY_SQUAD_UNIT
            )
    );

    // ITEM DISPLAY
    public static final RegistryObject<Item> RECRUIT_TABLE_ITEM = ITEMS.register("recruit_table",
            () -> new BlockItem(
                    ModBlocks.RECRUIT_TABLE.get(),
                    new Item.Properties()
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
