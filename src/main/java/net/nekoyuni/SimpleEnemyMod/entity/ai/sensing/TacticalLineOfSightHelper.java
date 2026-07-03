package net.nekoyuni.SimpleEnemyMod.entity.ai.sensing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.tactical.TacticalBlockClassifier;

/**
 * Shared tactical visibility checks for firearm AI.
 *
 * Vanilla sensing treats glass, panes, foliage and many visually thin blocks as full LoS blockers.
 * For gun-armed NPCs this helper keeps hard cover meaningful while allowing target acquisition and
 * firing through transparent/fragile visual clutter, matching the public AI branch's glass handling.
 */
public final class TacticalLineOfSightHelper {

    private static final double SAMPLE_STEP_BLOCKS = 0.20D;
    private static final int MAX_SAMPLES = 768;

    private TacticalLineOfSightHelper() {
    }

    public static boolean isPreciseFoliageLosEnabled() {
        return TacticalAiConfig.enablePreciseFoliageLos();
    }

    public static boolean canSeeForTargeting(Mob observer, LivingEntity target, boolean mustSee) {
        if (!mustSee) {
            return true;
        }

        if (!isPreciseFoliageLosEnabled()) {
            return observer.getSensing().hasLineOfSight(target);
        }

        return hasFoliageTransparentLineOfSight(observer, target);
    }

    /**
     * Tactical LoS used by both target acquisition and gun firing.
     *
     * The first check keeps the cheap vanilla/COLLIDER success path. If collider LoS is blocked, the
     * fallback performs its own sampled block trace and ignores only blocks that the public branch
     * already treats as breakable/soft visual cover, especially glass and panes.
     */
    public static boolean hasFoliageTransparentLineOfSight(LivingEntity observer, LivingEntity target) {
        if (observer == null || target == null || !target.isAlive()) {
            return false;
        }

        Level level = observer.level();
        Vec3 from = observer.getEyePosition();

        for (Vec3 to : getTargetSamplePoints(target)) {
            if (hasColliderLineOfSight(level, observer, from, to)) {
                return true;
            }

            if (hasSoftObstacleTransparentLineOfSight(level, from, to)) {
                return true;
            }
        }

        return false;
    }

    private static Vec3[] getTargetSamplePoints(LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        double height = target.getBbHeight();

        return new Vec3[]{
                target.getEyePosition(),
                new Vec3(x, y + height * 0.72D, z),
                new Vec3(x, y + height * 0.55D, z),
                new Vec3(x, y + Math.max(0.35D, height * 0.35D), z)
        };
    }

    private static boolean hasColliderLineOfSight(Level level, LivingEntity observer, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, observer);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }

    private static boolean hasSoftObstacleTransparentLineOfSight(Level level, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        if (distance <= 0.0001D) {
            return true;
        }

        int samples = Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP_BLOCKS)));
        Vec3 delta = to.subtract(from);
        long lastCheckedPos = Long.MIN_VALUE;

        for (int i = 1; i < samples; i++) {
            double t = (double) i / (double) samples;
            Vec3 sample = from.add(delta.scale(t));
            BlockPos pos = BlockPos.containing(sample);
            long packedPos = pos.asLong();

            if (packedPos == lastCheckedPos) {
                continue;
            }
            lastCheckedPos = packedPos;

            BlockState state = level.getBlockState(pos);
            if (TacticalBlockClassifier.isVisionTransparentClutter(state)) {
                continue;
            }

            VoxelShape collisionShape = state.getCollisionShape(level, pos);
            if (collisionShape.isEmpty()) {
                continue;
            }

            if (collisionShape.clip(from, to, pos) != null) {
                return false;
            }
        }

        return true;
    }

    public static boolean isVisionTransparentClutter(BlockState state) {
        return TacticalBlockClassifier.isVisionTransparentClutter(state);
    }

    public static boolean isGlassLike(BlockState state) {
        return TacticalBlockClassifier.isGlassLike(state);
    }
}
