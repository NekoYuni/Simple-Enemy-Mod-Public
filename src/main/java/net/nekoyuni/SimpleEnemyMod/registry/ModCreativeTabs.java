package net.nekoyuni.SimpleEnemyMod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleEnemyMod.MODID);

    public static final RegistryObject<CreativeModeTab> SIMPLE_ENEMY_MOD_TAB = CREATIVE_MODE_TABS.register(
            "simple_enemy_mod_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativeTab.simple_enemy_mod_tab"))
                    .icon(() -> new ItemStack(ModItems.RECRUIT_TABLE_ITEM.get()))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.RECRUIT_TABLE_ITEM.get());

                        output.accept(ModItems.PMC_UNIT_SPAWN_EGG.get());
                        output.accept(ModItems.PMC_SQUAD_LEADER_SPAWN_EGG.get());
                        output.accept(ModItems.PMC_SQUAD_UNIT_SPAWN_EGG.get());

                        output.accept(ModItems.US_UNIT_SPAWN_EGG.get());
                        output.accept(ModItems.US_SQUAD_LEADER_SPAWN_EGG.get());
                        output.accept(ModItems.US_SQUAD_UNIT_SPAWN_EGG.get());

                        output.accept(ModItems.RU_UNIT_SPAWN_EGG.get());
                        output.accept(ModItems.RU_SQUAD_LEADER_SPAWN_EGG.get());
                        output.accept(ModItems.RU_SQUAD_UNIT_SPAWN_EGG.get());

                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
