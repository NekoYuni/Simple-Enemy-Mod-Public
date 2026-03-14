package net.nekoyuni.SimpleEnemyMod.compat.curios;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosHelper {

    // TODO Move to ClientHelper
    private static final ResourceLocation CURIOS_OVERLAY =
            new ResourceLocation(SimpleEnemyMod.MODID, "textures/gui/pmc_unit_gui_curios.png");


    public static void addCuriosSlots(CuriosSlotRegister registerSlot, LivingEntity entity) {
        CuriosApi.getCuriosInventory(entity).ifPresent(curiosInventory -> {

            curiosInventory.getStacksHandler("head").ifPresent(headHandler -> {

                SlotItemHandler slot = new SlotItemHandler(headHandler.getStacks(), 0, -24, 19);
                slot.setBackground(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("curios", "slot/empty_head_slot"));
                registerSlot.register(slot);
            });

            curiosInventory.getStacksHandler("back").ifPresent(backHandler -> {

                SlotItemHandler slot = new SlotItemHandler(backHandler.getStacks(), 0, -24, 37);
                slot.setBackground(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("curios", "slot/empty_back_slot"));
                registerSlot.register(slot);
            });

            curiosInventory.getStacksHandler("body").ifPresent(bodyHandler -> {

                SlotItemHandler slot = new SlotItemHandler(bodyHandler.getStacks(), 0, -24, 55);
                slot.setBackground(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("curios", "slot/empty_body_slot"));
                registerSlot.register(slot);
            });

            curiosInventory.getStacksHandler("belt").ifPresent(beltHandler -> {

                SlotItemHandler slot = new SlotItemHandler(beltHandler.getStacks(), 0, -24, 73);
                slot.setBackground(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("curios", "slot/empty_belt_slot"));
                registerSlot.register(slot);
            });

        });
    }

    @FunctionalInterface
    public interface CuriosPieceRenderer<T extends LivingEntity> {
        void render(ItemStack stack, EquipmentSlot mappedSlot);
    }

    public static <T extends LivingEntity> void renderCuriosSlots(
            T entity,
            CuriosPieceRenderer<T> renderer
    ) {
        CuriosApi.getCuriosInventory(entity).ifPresent(curiosInventory -> {

            // back = CHEST
            renderSlot(curiosInventory, "back",  EquipmentSlot.CHEST, renderer);

            // head = HEAD   (sombreros, cascos extra)
            renderSlot(curiosInventory, "head",  EquipmentSlot.HEAD,  renderer);

            // body = CHEST
            renderSlot(curiosInventory, "body",  EquipmentSlot.CHEST, renderer);

            // belt = LEGS
            renderSlot(curiosInventory, "belt",  EquipmentSlot.LEGS,  renderer);
        });
    }

    private static <T extends LivingEntity> void renderSlot(
            top.theillusivec4.curios.api.type.capability.ICuriosItemHandler inventory,
            String slotId,
            EquipmentSlot mappedSlot,
            CuriosPieceRenderer<T> renderer
    ) {
        inventory.getStacksHandler(slotId).ifPresent(handler -> {
            ItemStack stack = handler.getStacks().getStackInSlot(0);
            if (!stack.isEmpty()) {
                renderer.render(stack, mappedSlot);
            }
        });
    }

    // TODO Move to ClientHelper
    public static void renderCuriosOverlay(GuiGraphics guiGraphics, int x, int y) {
        int overlayWidth  = 32;
        int overlayHeight = 85;

        guiGraphics.blit(
                CURIOS_OVERLAY,
                x + overlayWidth - 64,
                y + 12,
                0, 0,
                overlayWidth, overlayHeight
        );
    }

}
