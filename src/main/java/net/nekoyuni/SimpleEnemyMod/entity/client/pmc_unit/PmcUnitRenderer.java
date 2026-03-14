package net.nekoyuni.SimpleEnemyMod.entity.client.pmc_unit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.nekoyuni.SimpleEnemyMod.compat.geckolib.GeckoCompatClient;
import net.nekoyuni.SimpleEnemyMod.compat.geckolib.internal.GeckoArmorLayerImpl;
import net.nekoyuni.SimpleEnemyMod.compat.geckolib.GeckoCompat;
import net.nekoyuni.SimpleEnemyMod.config.ClientConfig;
import net.nekoyuni.SimpleEnemyMod.entity.client.GunLayerRenderer;
import net.nekoyuni.SimpleEnemyMod.entity.client.util.UniversalArmorLayer;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class PmcUnitRenderer extends MobRenderer<PmcUnitEntity, PmcUnitModel<PmcUnitEntity>> {

    private static ResourceLocation[] PMCUNIT_TEXTURES = new ResourceLocation[]{
            new ResourceLocation("simpleenemymod", "textures/entity/pmc_unit/pmc_unit_default.png")
    };

    public PmcUnitRenderer(EntityRendererProvider.Context context) {
        super(context, new PmcUnitModel(context.bakeLayer(PmcUnitModelLayers.PMCUNIT_LAYER)), 0.5f);

        HumanoidModel innerArmor = new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        HumanoidModel outerArmor = new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));

        this.addLayer(new UniversalArmorLayer<>(this, innerArmor, outerArmor));

        if (GeckoCompat.LOADED) {
            HumanoidModel geckoDummy = new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
            this.addLayer(GeckoCompatClient.createArmorLayer(this, geckoDummy));
        }

        this.addLayer(new GunLayerRenderer<>(this, context.getItemInHandRenderer()));
        reloadTextures(context.getResourceManager());

    }

    @Override
    public ResourceLocation getTextureLocation(PmcUnitEntity entity) {
        int variant = entity.getVariant();

        if (variant >= 0 && variant < PMCUNIT_TEXTURES.length) {
            return PMCUNIT_TEXTURES[variant];
        }

        return PMCUNIT_TEXTURES[0];
    }

    @Override
    public boolean shouldRender(PmcUnitEntity entity, Frustum frustum, double camX, double camY, double camZ) {

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
    protected void setupRotations(PmcUnitEntity pEntity, PoseStack pPoseStack,
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
    public void render(PmcUnitEntity p_115455_, float p_115456_, float p_115457_, PoseStack p_115458_, MultiBufferSource p_115459_, int p_115460_) {
        super.render(p_115455_, p_115456_, p_115457_, p_115458_, p_115459_, p_115460_);
    }

    private static void reloadTextures(ResourceManager rm) {
        List<ResourceLocation> found = new ArrayList<>();
        rm.listResources("textures/entity/pmc_unit", path -> path.getPath().endsWith(".png"))
                .keySet().forEach(found::add);
        found.sort(Comparator.comparing(ResourceLocation::getPath));
        if (!found.isEmpty()) PMCUNIT_TEXTURES = found.toArray(new ResourceLocation[0]);
    }

}
