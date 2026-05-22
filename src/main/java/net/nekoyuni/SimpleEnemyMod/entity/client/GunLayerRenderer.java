package net.nekoyuni.SimpleEnemyMod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

public class GunLayerRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private final ItemInHandRenderer itemRenderer;


    public GunLayerRenderer(RenderLayerParent<T, M> parent,
                            ItemInHandRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {

        if (entity.isDeadOrDying()) {
            return;
        }

        ItemStack stack = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }

        ResourceLocation gunId = gunItem.getGunId(stack);
        boolean isMinigun = gunId != null && gunId.getPath().contains("minigun");

        M model = this.getParentModel();

        if (!(model instanceof HierarchicalModel<?> hierarchicalModel)) {
            return;
        }

        @SuppressWarnings("unchecked")
        HierarchicalModel<T> hierarchicalModelTyped =
                (HierarchicalModel<T>) hierarchicalModel;

        ModelPart unit = hierarchicalModelTyped.root().getChild("unit");
        ModelPart rightArm = unit.getChild("rightArm");
        ModelPart rightHandAnchor = rightArm.getChild("rightHandAnchor");

        poseStack.pushPose();

        unit.translateAndRotate(poseStack);
        rightArm.translateAndRotate(poseStack);
        rightHandAnchor.translateAndRotate(poseStack);

        if (isMinigun) {
            renderMinigun(entity, stack, poseStack, buffer, packedLight);
            poseStack.popPose(); // test
            return;
        }

        // revisar bien, AbstractUnit
        boolean inCombat = !(entity instanceof AbstractUnit unit2) || unit2.isInCombat();

        if (inCombat) {
            renderStandardGun(entity, stack, poseStack, buffer, packedLight);
        } else {
            renderStandardGunLowStance(entity, stack, poseStack, buffer, packedLight);
        }

        poseStack.popPose();

    }


    private void renderStandardGun(T entity, ItemStack stack, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight) {

        poseStack.translate(0.0D, 0.10D, 0.28D); // -0.06, 0.73D, 0.3
        poseStack.mulPose(Axis.YP.rotationDegrees(-180)); // 180
        poseStack.mulPose(Axis.XP.rotationDegrees(-95)); // 90
        poseStack.scale(1.0F, -1.0F, -1.0F);

        itemRenderer.renderItem(
                entity,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                buffer,
                packedLight
        );
    }


    private void renderStandardGunLowStance(T entity, ItemStack stack, PoseStack poseStack,
                                            MultiBufferSource buffer, int packedLight) {

        poseStack.translate(0.0D, 0.35D, 0.13D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.scale(1.0F, -1.0F, -1.0F);


        itemRenderer.renderItem(
                entity,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                buffer,
                packedLight);
    }


    private void renderMinigun(T entity, ItemStack stack, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight) {

        poseStack.translate(0.01D, 0.30D, -0.25D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-70)); // -85
        poseStack.mulPose(Axis.ZP.rotationDegrees(6));
        poseStack.scale(1.0F, -1.0F, -1.0F);

        itemRenderer.renderItem(
                entity,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                buffer,
                packedLight
        );
    }

}