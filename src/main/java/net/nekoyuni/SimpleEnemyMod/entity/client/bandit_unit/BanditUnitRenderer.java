package net.nekoyuni.SimpleEnemyMod.entity.client.bandit_unit;

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
import net.nekoyuni.SimpleEnemyMod.entity.client.us_unit.USunitModel;
import net.nekoyuni.SimpleEnemyMod.entity.client.us_unit.USunitModelLayers;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.BanditUnitEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BanditUnitRenderer extends MobRenderer<BanditUnitEntity, USunitModel<BanditUnitEntity>> {

    private static ResourceLocation[] BANDIT_TEXTURES = new ResourceLocation[]{
            new ResourceLocation("simpleenemymod", "textures/entity/us_unit/us_unit_default.png")
    }; // Bandits intentionally reuse US skins.

    public BanditUnitRenderer(EntityRendererProvider.Context context) {
        super(context, new USunitModel<>(context.bakeLayer(USunitModelLayers.USUNIT_LAYER)), 0.5f);
        this.addLayer(new GunLayerRenderer<>(this, context.getItemInHandRenderer())); // Keep weapon rendering identical to other units.
        reloadTextures(context.getResourceManager());
    }

    @Override
    public ResourceLocation getTextureLocation(BanditUnitEntity entity) {
        int variant = entity.getVariant();

        if (variant >= 0 && variant < BANDIT_TEXTURES.length) {
            return BANDIT_TEXTURES[variant];
        }

        return BANDIT_TEXTURES[0];
    }

    @Override
    public boolean shouldRender(BanditUnitEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        int configDist = ClientConfig.RENDER_DISTANCE.get();
        double maxDistance = configDist * configDist;

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq <= maxDistance) {
            return true; // Preserve the custom unit render distance behavior.
        }

        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    protected void setupRotations(BanditUnitEntity entity, PoseStack poseStack, float ageInTicks, float bodyYRot, float partialTicks) {
        if (entity.deathAnimationState.isStarted()) {
            this.model.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);

            float rootMotionX = this.model.root().x / 16.0F;
            float rootMotionY = this.model.root().y / 16.0F;
            float rootMotionZ = this.model.root().z / 16.0F;

            float rootRotX = this.model.root().xRot;
            float rootRotY = this.model.root().yRot;
            float rootRotZ = this.model.root().zRot;

            poseStack.translate(rootMotionX, rootMotionY, rootMotionZ);
            poseStack.mulPose(new Quaternionf().rotationXYZ(rootRotX, rootRotY, rootRotZ));

            this.model.root().setPos(0, 0, 0);
            this.model.root().setRotation(0, 0, 0);

            float bodyRotation = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRotation));
            return;
        }

        super.setupRotations(entity, poseStack, ageInTicks, bodyYRot, partialTicks);
    }

    @Override
    public void render(BanditUnitEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void reloadTextures(ResourceManager rm) {
        List<ResourceLocation> found = new ArrayList<>();
        rm.listResources("textures/entity/us_unit", path -> path.getPath().endsWith(".png"))
                .keySet().forEach(found::add);
        found.sort(Comparator.comparing(ResourceLocation::getPath));
        if (!found.isEmpty()) {
            BANDIT_TEXTURES = found.toArray(new ResourceLocation[0]); // Load all available US variants for bandits.
        }
    }
}
