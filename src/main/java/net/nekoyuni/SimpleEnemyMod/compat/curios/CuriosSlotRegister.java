package net.nekoyuni.SimpleEnemyMod.compat.curios;

import net.minecraft.world.inventory.Slot;

@FunctionalInterface
public interface CuriosSlotRegister {
    void register(Slot slot);
}
