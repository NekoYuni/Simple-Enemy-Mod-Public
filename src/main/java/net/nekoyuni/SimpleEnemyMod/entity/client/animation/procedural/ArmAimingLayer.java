package net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural;

import com.ibm.icu.text.Normalizer2;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public class ArmAimingLayer extends AbstractProceduralLayer {

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    private float yawFollowFactor = 0.65f;
    private float pitchFollowFactor = 0.85f;


    public ArmAimingLayer(ModelPart head, ModelPart rightArm, ModelPart leftArm) {
        super("arm_aiming");
        this.head = head;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    public ArmAimingLayer(ModelPart head, ModelPart rightArm, ModelPart leftArm,
                          float yawFactor, float pitchFactor) {
        this(head, rightArm, leftArm);
        this.yawFollowFactor = yawFactor;
        this.pitchFollowFactor = pitchFactor;
    }

    @Override
    protected void applyTransformations(ModelPart root, Entity entity, float partialTick) {

        rightArm.yRot += head.yRot * yawFollowFactor;
        rightArm.xRot += head.xRot * pitchFollowFactor;

        leftArm.yRot += head.yRot * yawFollowFactor;
        leftArm.xRot += head.xRot * pitchFollowFactor;
    }

    public void setYawFollowFactor(float factor) {
        this.yawFollowFactor = factor;
    }

    public void setHeadPitchFactor(float factor) {
        this.pitchFollowFactor = factor;
    }
}
