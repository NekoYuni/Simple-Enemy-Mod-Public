package net.nekoyuni.SimpleEnemyMod.entity.equipment;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.nekoyuni.SimpleEnemyMod.data.UnitLoadout;
import net.nekoyuni.SimpleEnemyMod.event.common.UnitLoadoutManager;

public class PmcUnitWeaponEquipper {

    public static void equipRandomGun(LivingEntity entity, RandomSource random) {

        final String FACTION_ID = "pmc_units";

        UnitLoadout selectedLoadout;
        try {
            selectedLoadout = UnitLoadoutManager.getRandomLoadout(FACTION_ID, random);
        } catch (IllegalStateException e) {
            System.err.println("ERROR: The unit could not be equipped. " + e.getMessage());
            return;
        }

        ItemStack gunStack = GunItemBuilder.create()
                .setId(selectedLoadout.gunId)
                .setAmmoCount(selectedLoadout.ammoCount)
                .setFireMode(getJsonFireMode(selectedLoadout.fireMode))
                .setCount(1)
                .build();

        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            System.err.println("ERROR: The created itemstack is not an IGun weapon. Check Loadout: " + selectedLoadout.gunId);
            return;
        }

        // Scope
        selectedLoadout.scopeId.ifPresent(scopeId -> {
            ItemStack scopeStack = AttachmentItemBuilder.create().setId(scopeId).build();
            iGun.installAttachment(gunStack, scopeStack);
        });

        // Muzzle
        selectedLoadout.muzzleId.ifPresent(muzzleId -> {
            ItemStack muzzleStack = AttachmentItemBuilder.create().setId(muzzleId).build();
            iGun.installAttachment(gunStack, muzzleStack);
        });

        // Grip
        selectedLoadout.gripId.ifPresent(gripId -> {
            ItemStack gripStack = AttachmentItemBuilder.create().setId(gripId).build();
            iGun.installAttachment(gunStack, gripStack);
        });

        // iGun.setMaxDummyAmmoAmount(gunStack, Integer.MAX_VALUE);
        // iGun.setDummyAmmoAmount(gunStack, 9999);


        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            if (!(handler instanceof IItemHandlerModifiable modifiable)) return;

            modifiable.setStackInSlot(0, gunStack);

            ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(selectedLoadout.gunId)
                    .map(index -> index.getGunData().getAmmoId())
                    .orElse(null);

            if (ammoId != null) {
                int reserveAmmo = selectedLoadout.ammoCount * 6;
                ItemStack ammoStack = AmmoItemBuilder.create()
                        .setId(ammoId)
                        .setCount(reserveAmmo)
                        .build();

                for (int slot = 1; slot < modifiable.getSlots(); slot++) {
                    if (modifiable.getStackInSlot(slot).isEmpty()) {
                        modifiable.setStackInSlot(slot, ammoStack);
                        break;
                    }
                }
            }
        });
    }

    protected static FireMode getJsonFireMode(String fireModeStr) {

        if ("AUTO".equalsIgnoreCase(fireModeStr)) {
            return FireMode.AUTO;
        }
        return FireMode.SEMI;
    }

}
