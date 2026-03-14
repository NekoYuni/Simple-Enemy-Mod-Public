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

public class GunLayerRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private final ItemInHandRenderer itemRenderer;
    private static final String RIGHT_ARM_PART_NAME = "rightArm";


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

        ModelPart rightArm = hierarchicalModelTyped.root()
                .getChild("unit")
                .getChild(RIGHT_ARM_PART_NAME);

        poseStack.pushPose();
        rightArm.translateAndRotate(poseStack);

        if (isMinigun) {
            renderMinigun(entity, stack, poseStack, buffer, packedLight);
        } else {
            renderStandardGun(entity, stack, poseStack, buffer, packedLight);
        }

        poseStack.popPose();


    }


    private void renderStandardGun(
            T entity,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight

    ) {

        poseStack.translate(-0.06D, 0.73D, 0.3D);
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
                packedLight
        );
    }

    private void renderMinigun(T entity, ItemStack stack,
                               PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight) {

        poseStack.translate(-0.03D, 0.85D, 0.1D);

        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-85));

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