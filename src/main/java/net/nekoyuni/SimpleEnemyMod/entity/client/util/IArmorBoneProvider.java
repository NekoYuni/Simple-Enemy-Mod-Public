package net.nekoyuni.SimpleEnemyMod.entity.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;

public interface IArmorBoneProvider {

    ModelPart getUnit();
    ModelPart getHead();
    ModelPart getBody();
    ModelPart getRightArm();
    ModelPart getLeftArm();
    ModelPart getRightLeg();
    ModelPart getLeftLeg();

    void translateToBody(PoseStack poseStack);
}
