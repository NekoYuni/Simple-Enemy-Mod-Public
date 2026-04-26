package net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural;

import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.util.WeaponPose;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

public class CombatStateLayer extends AbstractProceduralLayer {

    private final ModelPart rightArm;
    private final ModelPart leftArm;


    public CombatStateLayer(ModelPart rightArm, ModelPart leftArm) {
        super("combat_state");
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    @Override
    protected void applyTransformations(ModelPart root, Entity entity, float partialTick) {

        if (!(entity instanceof AbstractUnit unit)) return;

        ItemStack stack = unit.getMainHandItem();
        if (!(stack.getItem() instanceof GunItemDataAccessor gunItem)) {
            return;
        }

        ResourceLocation gunId = gunItem.getGunId(stack);
        if (gunId.getPath().contains("minigun")) {
            return;
        }

        if (unit.isActiveCombat()) {
            return;
        }

        // low stance
        rightArm.xRot = -0.95f;
        rightArm.zRot = 0.00f;
        rightArm.yRot = 0.00f;

        leftArm.xRot = -0.25f;
        leftArm.zRot = 0.50f;
        leftArm.yRot = 0.50f;
    }
}
