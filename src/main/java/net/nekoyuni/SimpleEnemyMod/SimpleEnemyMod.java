package net.nekoyuni.SimpleEnemyMod;


import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.nekoyuni.SimpleEnemyMod.client.gui.screens.PmcUnitScreen;
import net.nekoyuni.SimpleEnemyMod.compat.cloth.ClothConfigCompat;
import net.nekoyuni.SimpleEnemyMod.compat.cloth.ClothConfigScreenHelper;
import net.nekoyuni.SimpleEnemyMod.config.ModConfigs;
import net.nekoyuni.SimpleEnemyMod.entity.client.pmc_unit.PmcUnitRenderer;
import net.nekoyuni.SimpleEnemyMod.procedural.events.DynamicEventManager;
import net.nekoyuni.SimpleEnemyMod.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.nekoyuni.SimpleEnemyMod.entity.client.us_unit.USunitRenderer;
import net.nekoyuni.SimpleEnemyMod.entity.client.ru_unit.RUunitRenderer;
import net.nekoyuni.SimpleEnemyMod.network.ModNetworking;
import org.slf4j.Logger;


@Mod(SimpleEnemyMod.MODID)
public class SimpleEnemyMod {

    public static final String MODID = "simpleenemymod";
    private static final Logger LOGGER = LogUtils.getLogger();


    public SimpleEnemyMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        ModMenuTypes.register(modEventBus);
        ModConfigs.register(ModLoadingContext.get());

        DynamicEventManager.register();


        if (ClothConfigCompat.LOADED) {

            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) ->
                            ClothConfigScreenHelper.createConfigScreen(parentScreen)
                    )
            );
        }

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetworking.register();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {

            event.accept(ModItems.US_UNIT_SPAWN_EGG);
            event.accept(ModItems.US_SQUAD_LEADER_SPAWN_EGG);
            event.accept(ModItems.US_SQUAD_UNIT_SPAWN_EGG);

            event.accept(ModItems.RU_UNIT_SPAWN_EGG);
            event.accept(ModItems.RU_SQUAD_LEADER_SPAWN_EGG);
            event.accept(ModItems.RU_SQUAD_UNIT_SPAWN_EGG);

            event.accept(ModItems.PMC_UNIT_SPAWN_EGG);
            event.accept(ModItems.PMC_SQUAD_LEADER_SPAWN_EGG);
            event.accept(ModItems.PMC_SQUAD_UNIT_SPAWN_EGG);
        }


    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NYAHELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            EntityRenderers.register(ModEntities.USUNIT.get(), USunitRenderer::new);
            EntityRenderers.register(ModEntities.RUUNIT.get(), RUunitRenderer::new);
            EntityRenderers.register(ModEntities.PMCUNIT.get(), PmcUnitRenderer::new);
            MenuScreens.register(ModMenuTypes.PMC_UNIT_MENU.get(), PmcUnitScreen::new);
        }
    }
}
