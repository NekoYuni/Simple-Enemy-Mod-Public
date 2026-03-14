package net.nekoyuni.SimpleEnemyMod.entity.client.ru_unit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.ModAnimationsDefinitions;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.config.UnitAnimationConfig;
import net.nekoyuni.SimpleEnemyMod.entity.client.util.UnitModelDefinitions;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;


public class RUunitModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart fakeRoot;
    private final ModelPart unit;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;


    public RUunitModel(ModelPart root) {
        this.fakeRoot = root.getChild("fakeRoot");
        this.unit = this.fakeRoot.getChild("unit");
        this.head = this.unit.getChild("head");
        this.body = this.unit.getChild("body");
        this.rightArm = this.unit.getChild("rightArm");
        this.leftArm = this.unit.getChild("leftArm");
        this.rightLeg = this.unit.getChild("rightLeg");
        this.leftLeg = this.unit.getChild("leftLeg");
    }


    public static LayerDefinition createBodyLayer() {
        return UnitModelDefinitions.createBaseUnitBodyLayer();
    }


    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        if (!(entity instanceof AbstractUnit unitEntity)) {
            return;
        }

        if (unitEntity.getAnimationManager() == null) {
            unitEntity.setAnimationManager(
                    UnitAnimationConfig.create(
                            unitEntity,
                            this.head,
                            this.rightArm,
                            this.leftArm
                    )
            );

        }

        unitEntity.getAnimationManager().update(unitEntity, unitEntity.tickCount);

        // Death
        if (unitEntity.deathAnimationState.isStarted()) {
            boolean playBack = unitEntity.getEntityData().get(AbstractUnit.BACK_DEATH);

            if (playBack) {
                this.animate(unitEntity.deathAnimationState, ModAnimationsDefinitions.UNIT_DEATH_BACK, ageInTicks, 1.0f);
            } else {
                this.animate(unitEntity.deathAnimationState, ModAnimationsDefinitions.UNIT_DEATH, ageInTicks, 1.0f);
            }

            return;
        }

        // Hurt
        if (unitEntity.hurtAnimationState.isStarted()) {
            int variantIndex = unitEntity.currentHurtVariant;
            int safeVariantIndex = (variantIndex >= 0 && variantIndex < ModAnimationsDefinitions.UNIT_HURT_VARIANTS.length)
                    ? variantIndex
                    : 0;

            this.animate(unitEntity.hurtAnimationState,
                    ModAnimationsDefinitions.UNIT_HURT_VARIANTS[safeVariantIndex],
                    ageInTicks, 2.0f);

            return;
        }

        // Walk
        if (unitEntity.walkAnimationState.isStarted()) {
            this.animate(unitEntity.walkAnimationState, ModAnimationsDefinitions.UNIT_WALK, ageInTicks, 1.0f);
        }
        // Idle
        else if (unitEntity.idleAnimationState.isStarted()) {
            this.animate(unitEntity.idleAnimationState, ModAnimationsDefinitions.UNIT_IDLE, ageInTicks, 1.0f);
        }

        unitEntity.getAnimationManager().applyProceduralLayers(this.root(), unitEntity, ageInTicks);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        fakeRoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ModelPart root() {
        return this.fakeRoot;
    }


}

