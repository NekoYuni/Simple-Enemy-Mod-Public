package net.nekoyuni.SimpleEnemyMod.entity.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.nekoyuni.SimpleEnemyMod.compat.geckolib.GeckoCompat;
import net.nekoyuni.SimpleEnemyMod.compat.geckolib.GeckoCompatClient;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;


public class UniversalArmorLayer<T extends AbstractUnit, M extends EntityModel<T> & IArmorBoneProvider>
        extends RenderLayer<T, M> {

    private final HumanoidModel<LivingEntity> innerModel;
    private final HumanoidModel<LivingEntity> outerModel;

    // Global settings
    private static final float GLOBAL_Y_OFFSET = -0.85F;

    // Scales
    private static final float SCALE_HEAD = 1.25F;
    private static final float SCALE_BODY = 1.98F;
    private static final float SCALE_ARMS = 1.90F;
    private static final float SCALE_LEGS = 2.10F;
    private static final float SCALE_BOOTS = 2.15F;

    // OFFSET X, Y, Z
    // Arms and Chest
    private static final float OFF_ARM_RIGHT_X = -3.95F;
    private static final float OFF_ARM_RIGHT_Y = 0.0F;
    private static final float OFF_ARM_RIGHT_Z = 0.0F;

    private static final float OFF_ARM_LEFT_X = 3.95F;
    private static final float OFF_ARM_LEFT_Y = 0.0F;
    private static final float OFF_ARM_LEFT_Z = 0.0F;

    // Legs
    private static final float OFF_LEG_RIGHT_X = -2.0F;
    private static final float OFF_LEG_RIGHT_Y = 8.0F;
    private static final float OFF_LEG_RIGHT_Z = -0.10F;

    private static final float OFF_LEG_LEFT_X = 2.0F;
    private static final float OFF_LEG_LEFT_Y = 8.0F;
    private static final float OFF_LEG_LEFT_Z = 0.10F;

    // Foot
    private static final float OFF_BOOT_RIGHT_X = -2.0F;
    private static final float OFF_BOOT_RIGHT_Y = 11.0F;
    private static final float OFF_BOOT_RIGHT_Z = 0.8F;

    private static final float OFF_BOOT_LEFT_X = 2.0F;
    private static final float OFF_BOOT_LEFT_Y = 11.0F;
    private static final float OFF_BOOT_LEFT_Z = -1.2F;


    @SuppressWarnings("unchecked")
    public UniversalArmorLayer(RenderLayerParent<T, M> parent, HumanoidModel<?> inner, HumanoidModel<?> outer) {
        super(parent);
        this.innerModel = (HumanoidModel<LivingEntity>) inner;
        this.outerModel = (HumanoidModel<LivingEntity>) outer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {

        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, packedLight, outerModel,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, packedLight, innerModel,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, packedLight, outerModel,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, packedLight, outerModel,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @SuppressWarnings("unchecked")
    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity,
                                  EquipmentSlot slot, int packedLight, HumanoidModel<LivingEntity> defaultModel,
                                  float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch) {

        ItemStack itemStack = entity.getItemBySlot(slot);
        if (itemStack.isEmpty()) return;

        if (GeckoCompat.LOADED && GeckoCompatClient.isGeckoArmor(itemStack)) {
            return;
        }

        if (!(itemStack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        HumanoidModel<LivingEntity> armorModel = getArmorModel(entity, itemStack, slot, defaultModel);
        if (armorModel == null) {
            return;
        }

        armorModel.setupAnim((LivingEntity) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        syncArmorToEntity(this.getParentModel(), armorModel, slot);
        applyManualScale(armorModel, slot);
        applyManualPosition(armorModel, slot);

        boolean hasGlint = itemStack.hasFoil();
        boolean isDyeable = itemStack.getItem() instanceof DyeableArmorItem;
        ResourceLocation texture = getArmorTexture(entity, itemStack, slot, armorItem);

        if (texture == null) return;

        poseStack.pushPose();
        this.getParentModel().translateToBody(poseStack);
        poseStack.translate(0.0F, GLOBAL_Y_OFFSET, 0.0F);

        renderArmorModel(poseStack, buffer, packedLight, hasGlint, armorModel, texture, isDyeable, itemStack);

        if (isDyeable) {
            ResourceLocation overlayTexture = getArmorOverlayTexture(entity, itemStack, slot, armorItem);

            if (overlayTexture != null) {
                renderArmorModel(poseStack, buffer, packedLight, hasGlint, armorModel, overlayTexture, false, itemStack);
            }
        }

        poseStack.popPose();
    }

    private void applyManualScale(HumanoidModel<LivingEntity> armorModel, EquipmentSlot slot) {
        switch (slot) {
            case HEAD -> {
                setPartScale(armorModel.head, SCALE_HEAD);
                setPartScale(armorModel.hat, SCALE_HEAD);
            }
            case CHEST -> {
                setPartScale(armorModel.body, SCALE_BODY);
                setPartScale(armorModel.rightArm, SCALE_ARMS);
                setPartScale(armorModel.leftArm, SCALE_ARMS);
            }
            case LEGS -> {
                setPartScale(armorModel.body, SCALE_BODY);
                setPartScale(armorModel.rightLeg, SCALE_LEGS);
                setPartScale(armorModel.leftLeg, SCALE_LEGS);
            }
            case FEET -> {
                setPartScale(armorModel.rightLeg, SCALE_BOOTS);
                setPartScale(armorModel.leftLeg, SCALE_BOOTS);
            }
        }
    }

    /**
     * New method for adjusting positions individually
     */
    private void applyManualPosition(HumanoidModel<LivingEntity> armorModel, EquipmentSlot slot) {
        switch (slot) {
            case CHEST -> {
                addPartOffset(armorModel.rightArm, OFF_ARM_RIGHT_X, OFF_ARM_RIGHT_Y, OFF_ARM_RIGHT_Z);
                addPartOffset(armorModel.leftArm, OFF_ARM_LEFT_X, OFF_ARM_LEFT_Y, OFF_ARM_LEFT_Z);
            }
            case LEGS -> {
                addPartOffset(armorModel.rightLeg, OFF_LEG_RIGHT_X, OFF_LEG_RIGHT_Y, OFF_LEG_RIGHT_Z);
                addPartOffset(armorModel.leftLeg, OFF_LEG_LEFT_X, OFF_LEG_LEFT_Y, OFF_LEG_LEFT_Z);
            }
            case FEET -> {
                addPartOffset(armorModel.rightLeg, OFF_BOOT_RIGHT_X, OFF_BOOT_RIGHT_Y, OFF_BOOT_RIGHT_Z);
                addPartOffset(armorModel.leftLeg, OFF_BOOT_LEFT_X, OFF_BOOT_LEFT_Y, OFF_BOOT_LEFT_Z);
            }
        }
    }

    private void setPartScale(ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }

    private void addPartOffset(ModelPart part, float x, float y, float z) {
        part.x += x;
        part.y += y;
        part.z += z;
    }

    private void syncArmorToEntity(M entityModel, HumanoidModel<LivingEntity> armorModel, EquipmentSlot slot) {
        armorModel.setAllVisible(false);

        switch (slot) {
            case HEAD -> {
                armorModel.head.visible = true;
                armorModel.hat.visible = true;
                copyModelPart(entityModel.getHead(), armorModel.head);
                copyModelPart(entityModel.getHead(), armorModel.hat);
            }
            case CHEST -> {
                armorModel.body.visible = true;
                armorModel.rightArm.visible = true;
                armorModel.leftArm.visible = true;
                copyModelPart(entityModel.getBody(), armorModel.body);
                copyModelPart(entityModel.getRightArm(), armorModel.rightArm);
                copyModelPart(entityModel.getLeftArm(), armorModel.leftArm);
            }
            case LEGS -> {
                armorModel.body.visible = true;
                armorModel.rightLeg.visible = true;
                armorModel.leftLeg.visible = true;
                copyModelPart(entityModel.getBody(), armorModel.body);
                copyModelPart(entityModel.getRightLeg(), armorModel.rightLeg);
                copyModelPart(entityModel.getLeftLeg(), armorModel.leftLeg);
            }
            case FEET -> {
                armorModel.rightLeg.visible = true;
                armorModel.leftLeg.visible = true;
                copyModelPart(entityModel.getRightLeg(), armorModel.rightLeg);
                copyModelPart(entityModel.getLeftLeg(), armorModel.leftLeg);
            }
        }
    }

    private void copyModelPart(ModelPart source, ModelPart target) {
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
    }

    @SuppressWarnings("unchecked")
    private HumanoidModel<LivingEntity> getArmorModel(T entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<LivingEntity> defaultModel) {

        var model = ForgeHooksClient.getArmorModel(entity, stack, slot, defaultModel);

        return model instanceof HumanoidModel ? (HumanoidModel<LivingEntity>) model : null;
    }

    private ResourceLocation getArmorTexture(T entity, ItemStack stack, EquipmentSlot slot, ArmorItem armorItem) {

        String baseTexture = armorItem.getArmorTexture(stack, entity, slot, null);
        String texture = ForgeHooksClient.getArmorTexture(entity, stack, baseTexture, slot, null);

        if (texture != null) {
            return new ResourceLocation(texture);
        }

        String material = armorItem.getMaterial().getName();
        String domain = "minecraft";
        String path = material;

        if (material.contains(":")) {
            String[] split = material.split(":");
            domain = split[0];
            path = split[1];
        }

        String layer = (slot == EquipmentSlot.LEGS) ? "layer_2" : "layer_1";

        return new ResourceLocation(domain, "textures/models/armor/" + path + "_" + layer + ".png");
    }

    private ResourceLocation getArmorOverlayTexture(T entity, ItemStack stack, EquipmentSlot slot, ArmorItem armorItem) {

        String texture = ForgeHooksClient.getArmorTexture(
                entity,
                stack,
                armorItem.getArmorTexture(stack, entity, slot, "overlay"), slot, "overlay"
        );

        return texture != null ? new ResourceLocation(texture) : null;
    }

    private void renderArmorModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean hasGlint,
                                  HumanoidModel<LivingEntity> model, ResourceLocation texture, boolean isDyeable, ItemStack stack) {

        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                buffer,
                RenderType.armorCutoutNoCull(texture),
                false, hasGlint
        );

        float r = 1.0F, g = 1.0F, b = 1.0F;

        if (isDyeable && stack.getItem() instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(stack);
            r = ((color >> 16) & 255) / 255.0F;
            g = ((color >> 8) & 255) / 255.0F;
            b = (color & 255) / 255.0F;
        }
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
    }
}

