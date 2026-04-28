package net.nekoyuni.SimpleEnemyMod.entity.equipment;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.nekoyuni.SimpleEnemyMod.data.UnitLoadout;
import net.nekoyuni.SimpleEnemyMod.event.common.UnitLoadoutManager;

public class BanditWeaponEquipper {

    private static final String FACTION_ID = "bandits"; // Matches data/simpleenemymod/unit_loadouts/bandits/loadouts.json.

    public static void equipRandomGun(LivingEntity entity, RandomSource random) {
        UnitLoadout selectedLoadout;

        try {
            selectedLoadout = UnitLoadoutManager.getRandomLoadout(FACTION_ID, random); // Pick one bandit pistol loadout.
        } catch (IllegalStateException e) {
            System.err.println("ERROR: The bandit could not be equipped. " + e.getMessage());
            return;
        }

        ItemStack gunStack = GunItemBuilder.create()
                .setId(selectedLoadout.gunId)
                .setAmmoCount(selectedLoadout.ammoCount)
                .setFireMode(PmcUnitWeaponEquipper.getJsonFireMode(selectedLoadout.fireMode)) // Reuse existing JSON fire-mode parsing.
                .setCount(1)
                .build();

        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            System.err.println("ERROR: The created itemstack is not an IGun weapon. Check Loadout: " + selectedLoadout.gunId);
            return;
        }

        selectedLoadout.scopeId.ifPresent(scopeId -> {
            ItemStack scopeStack = AttachmentItemBuilder.create().setId(scopeId).build();
            iGun.installAttachment(gunStack, scopeStack); // Optional scope attachment from JSON.
        });

        selectedLoadout.muzzleId.ifPresent(muzzleId -> {
            ItemStack muzzleStack = AttachmentItemBuilder.create().setId(muzzleId).build();
            iGun.installAttachment(gunStack, muzzleStack); // Optional muzzle attachment from JSON.
        });

        selectedLoadout.gripId.ifPresent(gripId -> {
            ItemStack gripStack = AttachmentItemBuilder.create().setId(gripId).build();
            iGun.installAttachment(gunStack, gripStack); // Optional grip attachment from JSON.
        });

        iGun.setMaxDummyAmmoAmount(gunStack, Integer.MAX_VALUE); // Let NPC shooting logic use virtual ammo.
        iGun.setDummyAmmoAmount(gunStack, 9999);

        entity.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
    }
}
