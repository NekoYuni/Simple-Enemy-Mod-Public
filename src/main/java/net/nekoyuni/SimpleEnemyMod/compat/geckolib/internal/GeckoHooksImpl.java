package net.nekoyuni.SimpleEnemyMod.compat.geckolib.internal;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;

public class GeckoHooksImpl {

    public static boolean isGeckoArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        return (item instanceof ArmorItem && item instanceof GeoItem);
    }
}
