package net.nekoyuni.SimpleEnemyMod.event.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.system.SuppressionManager;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientOverlayHandler {

    private static final ResourceLocation SUPPRESSION_TEXTURE = new ResourceLocation("simpleenemymod",
            "textures/gui/suppression_effect.png");

    public static final IGuiOverlay SUPPRESSION_OVERLAY = (gui, guiGraphics, partialTick,
                                                           screenWidth, screenHeight) -> {
        float level = SuppressionManager.suppressionLevel;

        if (level > 0.01f) {

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, level);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, SUPPRESSION_TEXTURE);

            guiGraphics.blit(SUPPRESSION_TEXTURE, 0, 0, screenWidth, screenHeight, 0.0F, 0.0F,
                    screenWidth, screenHeight, screenWidth, screenHeight);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    };

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("suppression_overlay", SUPPRESSION_OVERLAY);
    }

}
