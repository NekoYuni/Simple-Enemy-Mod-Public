package net.nekoyuni.SimpleEnemyMod.compat.geckolib;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

public class GeckoCompatClient {


    public static <T extends AbstractUnit, M extends net.minecraft.client.model.EntityModel<T>>
    RenderLayer<T, M> createArmorLayer(RenderLayerParent<T, M> parent, HumanoidModel<?> dummyModel) {

        return net.nekoyuni.SimpleEnemyMod.compat.geckolib.internal.GeckoArmorLayerImpl.create(parent, dummyModel);
    }

    public static boolean isGeckoArmor(ItemStack stack) {
        if (!GeckoCompat.LOADED || stack.isEmpty()) {
            return false;
        }

        return net.nekoyuni.SimpleEnemyMod.compat.geckolib.internal.GeckoHooksImpl.isGeckoArmor(stack);
    }
}
