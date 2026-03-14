package net.nekoyuni.SimpleEnemyMod.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.compat.curios.CuriosCompat;
import net.nekoyuni.SimpleEnemyMod.compat.curios.CuriosHelper;
import net.nekoyuni.SimpleEnemyMod.inventory.PmcUnitMenu;


public class PmcUnitScreen extends AbstractContainerScreen<PmcUnitMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SimpleEnemyMod.MODID, "textures/gui/pmc_unit_gui.png");

    public PmcUnitScreen(PmcUnitMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 93;

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (CuriosCompat.LOADED) {
            CuriosHelper.renderCuriosOverlay(guiGraphics, x, y);
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                x + 53, y + 85, 30,
                (float)(x + 62) - mouseX,
                (float)(y + 35) - mouseY,
                this.menu.unit
        );

    }
}
