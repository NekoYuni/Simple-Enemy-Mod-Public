package net.nekoyuni.SimpleEnemyMod.event.common;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.DropsConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;

@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UnitDropsHandler {

    @SubscribeEvent
    public static void onUnitDrops(LivingDropsEvent event) {

        if (!(event.getEntity() instanceof AbstractUnit unit)) return;

        event.getDrops().removeIf(itemEntity ->
                IGun.getIGunOrNull(itemEntity.getItem()) != null
        );

        if (unit instanceof PmcUnitEntity pmcUnit && pmcUnit.getOwnerUUID() != null) {
            dropPmcWeapon(pmcUnit, event);
            return;
        }

        if (!DropsConfig.ENABLE_CUSTOM_DROPS.get()) return;

        ItemStack sourceGun = findGunStack(unit);
        if (sourceGun.isEmpty()) return;

        IGun iGun = IGun.getIGunOrNull(sourceGun);
        if (iGun == null) return;

        ResourceLocation gunId = iGun.getGunId(sourceGun);
        FireMode fireMode = iGun.getFireMode(sourceGun);

        ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> index.getGunData().getAmmoId())
                .orElse(null);

        if (ammoId == null) return;

        if (unit.getRandom().nextFloat() <= DropsConfig.GUN_DROP_CHANCE.get()) {

            int maxAmmo = TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> index.getGunData().getAmmoAmount())
                    .orElse(30);

            int partialAmmo = unit.getRandom().nextInt(Math.max(1, maxAmmo / 4));

            ItemStack cleanGun = GunItemBuilder.create()
                    .setId(gunId)
                    .setAmmoCount(partialAmmo)
                    .setFireMode(fireMode)
                    .setCount(1)
                    .build();

            IGun iCleanGun = IGun.getIGunOrNull(cleanGun);
            if (iCleanGun != null) {
                for (AttachmentType type : AttachmentType.values()) {
                    ItemStack attachment = iGun.getAttachment(sourceGun, type);
                    if (!attachment.isEmpty()) {
                        iCleanGun.installAttachment(cleanGun, attachment.copy());
                    }
                }
            }

            ItemEntity cleanGunEntity = new ItemEntity(
                    unit.level(), unit.getX(), unit.getY(), unit.getZ(), cleanGun
            );
            event.getDrops().add(cleanGunEntity);

        }

        if (unit.getRandom().nextFloat() <= DropsConfig.AMMO_DROP_CHANCE.get()) {

            int ammoCount = 10 + unit.getRandom().nextInt(21);
            ItemStack ammoDrop = AmmoItemBuilder.create()
                    .setId(ammoId)
                    .setCount(ammoCount)
                    .build();

            ItemEntity ammoEntity = new ItemEntity(
                    unit.level(), unit.getX(), unit.getY(), unit.getZ(), ammoDrop
            );
            event.getDrops().add(ammoEntity);
        }
    }


    private static ItemStack findGunStack(AbstractUnit unit) {

        ItemStack[] result = { ItemStack.EMPTY };

        unit.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (IGun.getIGunOrNull(stack) != null) {
                    result[0] = stack;
                    return;
                }
            }
        });

        if (!result[0].isEmpty()) return result[0];

        ItemStack mainHand = unit.getMainHandItem();
        if (IGun.getIGunOrNull(mainHand) != null) {
            return mainHand;
        }

        ItemStack offHand = unit.getOffhandItem();
        if (IGun.getIGunOrNull(offHand) != null) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static void dropPmcWeapon(PmcUnitEntity pmcUnit, LivingDropsEvent event) {

        ItemStack sourceGun = findGunStack(pmcUnit);
        if (sourceGun.isEmpty()) return;

        ItemEntity gunEntity = new ItemEntity(
                pmcUnit.level(),
                pmcUnit.getX(), pmcUnit.getY(), pmcUnit.getZ(),
                sourceGun.copy()
        );
        event.getDrops().add(gunEntity);
    }
}
