package net.nekoyuni.SimpleEnemyMod.inventory;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;

public class UnitInventoryHandler extends ItemStackHandler {
    private final PmcUnitEntity unit;

    public UnitInventoryHandler(PmcUnitEntity unit) {
        super(18); // 18
        this.unit = unit;
    }

    @Override
    protected void onContentsChanged(int slot) {
        ItemStack stack = this.getStackInSlot(slot);

        switch (slot) {
            case 0 -> {
                unit.setItemSlot(EquipmentSlot.MAINHAND, stack);

                if (!unit.level().isClientSide) {
                    IGunOperator operator = IGunOperator.fromLivingEntity(unit);

                    if (!stack.isEmpty()) {
                        operator.draw(() -> stack);
                    }
                }
            }
            case 1 -> unit.setItemSlot(EquipmentSlot.OFFHAND, stack);
            case 2 -> unit.setItemSlot(EquipmentSlot.FEET, stack);
            case 3 -> unit.setItemSlot(EquipmentSlot.LEGS, stack);
            case 4 -> unit.setItemSlot(EquipmentSlot.CHEST, stack);
            case 5 -> unit.setItemSlot(EquipmentSlot.HEAD, stack);

        }

    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return super.isItemValid(slot, stack);
    }
}
