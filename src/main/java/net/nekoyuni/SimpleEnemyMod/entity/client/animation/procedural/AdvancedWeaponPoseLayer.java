package net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.util.WeaponPose;


import java.util.Optional;

public class AdvancedWeaponPoseLayer extends AbstractProceduralLayer {

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public AdvancedWeaponPoseLayer(ModelPart head, ModelPart rightArm, ModelPart leftArm) {
        super("advanced_weapon_pose");
        this.head = head;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    @Override
    protected void applyTransformations(ModelPart root, Entity entity, float partialTick) {

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        ItemStack stack = living.getMainHandItem();
        WeaponPose pose = determinePose(stack);

        switch (pose) {
            case HEAVY_HIP_FIRE -> applyHipFirePose(root, living);
            case PISTOL_AIM -> applyPistolPose(root, living);
            case RIFLE_AIM -> applyRiflePose(root, living);
            case NONE -> { }
        }
    }

    private WeaponPose determinePose(ItemStack stack) {

        if (!(stack.getItem() instanceof GunItemDataAccessor gunItem)) {
            return WeaponPose.NONE;
        }

        ResourceLocation gunId = gunItem.getGunId(stack);

        if (gunId.getPath().contains("minigun")) {
            return WeaponPose.HEAVY_HIP_FIRE;
        }

        Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);

        if (gunIndexOpt.isPresent()) {
            String type = gunIndexOpt.get().getType();

            if ("pistol".equals(type)) {
                return WeaponPose.PISTOL_AIM;
            }
        }

        return WeaponPose.RIFLE_AIM;
    }


    private void applyRiflePose(ModelPart root, Entity entity) {
        float yawFollow = 0.65f;
        float pitchFollow = 0.85f;

        rightArm.yRot += head.yRot * yawFollow;
        rightArm.xRot += head.xRot * pitchFollow;

        leftArm.yRot += head.yRot * yawFollow;
        leftArm.xRot += head.xRot * pitchFollow;
    }


    private void applyHipFirePose(ModelPart root, Entity entity) {

        float heavyPitchFactor = 0.15f;
        float yawFollow = 0.6f;
        float targetPitch = head.xRot * heavyPitchFactor;

        // Head follow
        rightArm.yRot += head.yRot * yawFollow;
        leftArm.yRot  += head.yRot * yawFollow;

        rightArm.xRot += targetPitch;
        leftArm.xRot  += targetPitch;

        rightArm.xRot += 1.48f;
        leftArm.xRot  += 0.18f;

        rightArm.zRot += 0.12f;
        rightArm.yRot -= 0.05f;

        leftArm.zRot  -= 0.25f;
        leftArm.yRot  += 0.12f;

        leftArm.xRot  += 0.08f;
    }

    private void applyPistolPose(ModelPart root, Entity entity) {

        float yawFollow = 0.7f;
        float pitchFollow = 0.9f;

        // Head follow
        rightArm.yRot += head.yRot * yawFollow;
        rightArm.xRot += head.xRot * pitchFollow;

        leftArm.yRot += head.yRot * yawFollow;
        leftArm.xRot += head.xRot * pitchFollow;

        // Hold Gun
        rightArm.yRot -= 0.12f;
        leftArm.yRot += 0.12f;

        // Roll for arms
        rightArm.zRot += 0.04f;
        leftArm.zRot -= 0.04f;
    }
}
