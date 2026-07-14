package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.sensing.TacticalLineOfSightHelper;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Nearest-target goal with tactical line-of-sight and lightweight target-priority rules.
 *
 * <p>Target acquisition remains intentionally conservative: distance is the first priority, then tactical LoS.
 * While already engaged, the goal periodically scans only the close-threat radius and can switch to a much closer
 * visible hostile. This gives CQB threats priority without introducing the full Phase 5 target scoring model yet.</p>
 */
public class VerticalAwareTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private static final int TARGET_SCAN_INTERVAL = 10;
    private static final int MAX_TACTICAL_LOS_CANDIDATES = 32;

    private final Class<T> targetClass;
    private final boolean tacticalMustSee;
    private final double verticalRange;
    private final TargetingConditions tacticalTargetConditions;

    private int tacticalUnseenTicks;
    private int tacticalUnseenMemoryTicks = 60;
    private long nextCloseThreatScanTick;

    public VerticalAwareTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee, double verticalRange) {
        this(mob, targetClass, mustSee, verticalRange, null);
    }

    public VerticalAwareTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee,
                                   double verticalRange, @Nullable Predicate<LivingEntity> predicate) {
        // Disable vanilla visibility only. Eligibility/range checks are rebuilt below and tactical LoS
        // is applied explicitly so glass and panes do not block target acquisition.
        super(mob, targetClass, TARGET_SCAN_INTERVAL, false, false, predicate);
        this.targetClass = targetClass;
        this.tacticalMustSee = mustSee;
        this.verticalRange = verticalRange;
        this.tacticalTargetConditions = TargetingConditions.forCombat()
                .range(this.getFollowDistance())
                .selector(predicate)
                .ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        if (TARGET_SCAN_INTERVAL > 0 && this.mob.getRandom().nextInt(TARGET_SCAN_INTERVAL) != 0) {
            return false;
        }

        this.target = this.findNearestTacticalTarget(this.getFollowDistance(), MAX_TACTICAL_LOS_CANDIDATES);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) {
            return false;
        }

        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget == null || !this.isValidBaseTarget(currentTarget)) {
            return false;
        }

        maybeSwitchToCloseThreat(currentTarget);

        if (this.tacticalMustSee) {
            boolean visible = TacticalLineOfSightHelper.canSeeForTargeting(this.mob, currentTarget, true);
            if (visible) {
                this.tacticalUnseenTicks = 0;
                return true;
            }

            this.tacticalUnseenTicks++;
            return this.tacticalUnseenTicks <= this.tacticalUnseenMemoryTicks;
        }

        return true;
    }

    @Override
    public void start() {
        this.tacticalUnseenTicks = 0;
        this.nextCloseThreatScanTick = 0L;
        super.start();
    }

    @Override
    public void stop() {
        this.tacticalUnseenTicks = 0;
        this.nextCloseThreatScanTick = 0L;
        super.stop();
    }

    @Override
    public VerticalAwareTargetGoal<T> setUnseenMemoryTicks(int unseenMemoryTicks) {
        super.setUnseenMemoryTicks(unseenMemoryTicks);
        this.tacticalUnseenMemoryTicks = unseenMemoryTicks;
        return this;
    }

    private void maybeSwitchToCloseThreat(LivingEntity currentTarget) {
        if (!TacticalAiConfig.enableDistanceTargetPriority()) {
            return;
        }

        long now = this.mob.level().getGameTime();
        if (now < this.nextCloseThreatScanTick) {
            return;
        }
        this.nextCloseThreatScanTick = now + Math.max(1, TacticalAiConfig.closeThreatSwitchScanIntervalTicks());

        double closeRange = Math.max(1.0D, TacticalAiConfig.closeThreatSwitchDistance());
        T closeThreat = this.findNearestTacticalTarget(closeRange, 12);
        if (closeThreat == null || closeThreat == currentTarget) {
            return;
        }

        double currentDistanceSq = this.mob.distanceToSqr(currentTarget);
        double candidateDistanceSq = this.mob.distanceToSqr(closeThreat);
        double closeRangeSq = closeRange * closeRange;
        double ratio = TacticalAiConfig.closeThreatSwitchRatio();

        boolean currentIsFar = currentDistanceSq > closeRangeSq;
        boolean candidateIsMuchCloser = candidateDistanceSq < currentDistanceSq * ratio;
        boolean currentNotVisible = this.tacticalMustSee && !TacticalLineOfSightHelper.canSeeForTargeting(this.mob, currentTarget, true);

        if (currentIsFar || candidateIsMuchCloser || currentNotVisible) {
            this.mob.setTarget(closeThreat);
            this.target = closeThreat;
            this.tacticalUnseenTicks = 0;
        }
    }

    @Nullable
    private T findNearestTacticalTarget(double horizontalRange, int maxLosCandidates) {
        double range = Math.max(1.0D, Math.min(horizontalRange, this.getFollowDistance()));
        AABB searchArea = this.getTargetSearchArea(range);
        List<T> candidates = this.mob.level().getEntitiesOfClass(this.targetClass, searchArea, this::isValidBaseTarget);

        candidates.sort(Comparator.comparingDouble(candidate -> this.mob.distanceToSqr(candidate)));

        int tacticalLosChecks = 0;
        for (T candidate : candidates) {
            if (tacticalLosChecks++ >= Math.max(1, maxLosCandidates)) {
                break;
            }

            if (TacticalLineOfSightHelper.canSeeForTargeting(this.mob, candidate, this.tacticalMustSee)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isValidBaseTarget(LivingEntity target) {
        if (target == null || target == this.mob || !target.isAlive() || target.isSpectator()) {
            return false;
        }

        if (!this.targetClass.isInstance(target)) {
            return false;
        }

        if (!this.mob.canAttack(target)) {
            return false;
        }

        return this.tacticalTargetConditions
                .range(this.getFollowDistance())
                .test(this.mob, target);
    }

    @Override
    protected AABB getTargetSearchArea(double followRange) {
        return this.mob.getBoundingBox().inflate(followRange, verticalRange, followRange);
    }
}
