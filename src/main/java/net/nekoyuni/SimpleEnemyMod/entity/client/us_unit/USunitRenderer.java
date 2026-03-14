package net.nekoyuni.SimpleEnemyMod.entity.client.us_unit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.nekoyuni.SimpleEnemyMod.config.ClientConfig;
import net.nekoyuni.SimpleEnemyMod.entity.client.GunLayerRenderer;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class USunitRenderer extends MobRenderer<USunitEntity, USunitModel<USunitEntity>> {


    private static ResourceLocation[] USUNIT_TEXTURES = new ResourceLocation[]{
            new ResourceLocation("simpleenemymod", "textures/entity/us_unit/us_unit_default.png")
    };

    public USunitRenderer(EntityRendererProvider.Context context) {
        super(context, new USunitModel<>(context.bakeLayer(USunitModelLayers.USUNIT_LAYER)), 0.5f);

        this.addLayer(new GunLayerRenderer<>(this, context.getItemInHandRenderer()));
        reloadTextures(context.getResourceManager());
    }

    @Override
    public ResourceLocation getTextureLocation(USunitEntity entity) {
        int variant = entity.getVariant();

        if (variant >= 0 && variant < USUNIT_TEXTURES.length) {
            return USUNIT_TEXTURES[variant];
        }

        return USUNIT_TEXTURES[0];
    }

    @Override
    public boolean shouldRender(USunitEntity entity, Frustum frustum, double camX, double camY, double camZ) {

        int configDist = ClientConfig.RENDER_DISTANCE.get();
        double maxDistance = configDist * configDist;

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;

        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq <= maxDistance)
            return true;

        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    protected void setupRotations(USunitEntity pEntity, PoseStack pPoseStack,
                                  float pAgeInTicks, float pBodyYRot, float pPartialTicks) {

        if (pEntity.deathAnimationState.isStarted()) {

            this.model.setupAnim(pEntity, 0, 0, pEntity.tickCount + pPartialTicks,
                    0, 0);

            float rootMotionX = this.model.root().x / 16.0F;
            float rootMotionY = this.model.root().y / 16.0F;
            float rootMotionZ = this.model.root().z / 16.0F;

            float rootRotX = this.model.root().xRot;
            float rootRotY = this.model.root().yRot;
            float rootRotZ = this.model.root().zRot;

            pPoseStack.translate(rootMotionX, rootMotionY, rootMotionZ);
            pPoseStack.mulPose(new Quaternionf().rotationXYZ(rootRotX, rootRotY, rootRotZ));

            this.model.root().setPos(0, 0, 0);
            this.model.root().setRotation(0, 0, 0);

            float bodyRotation = Mth.lerp(pPartialTicks, pEntity.yBodyRotO, pEntity.yBodyRot);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRotation));

            return;
        }

        super.setupRotations(pEntity, pPoseStack, pAgeInTicks, pBodyYRot, pPartialTicks);
    }

    @Override
    public void render(USunitEntity p_115455_, float p_115456_, float p_115457_, PoseStack p_115458_, MultiBufferSource p_115459_, int p_115460_) {
        super.render(p_115455_, p_115456_, p_115457_, p_115458_, p_115459_, p_115460_);
    }

    private static void reloadTextures(ResourceManager rm) {
        List<ResourceLocation> found = new ArrayList<>();
        rm.listResources("textures/entity/us_unit", path -> path.getPath().endsWith(".png"))
                .keySet().forEach(found::add);
        found.sort(Comparator.comparing(ResourceLocation::getPath));
        if (!found.isEmpty()) USUNIT_TEXTURES = found.toArray(new ResourceLocation[0]);
    }

}