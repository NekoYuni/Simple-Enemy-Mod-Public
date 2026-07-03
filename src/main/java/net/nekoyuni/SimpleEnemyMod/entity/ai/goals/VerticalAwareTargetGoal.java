package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.nekoyuni.SimpleEnemyMod.entity.ai.sensing.TacticalLineOfSightHelper;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Nearest-target goal with the same vertical search behavior as the current repository, but with
 * target acquisition decoupled from vanilla LoS when tactical LoS is enabled.
 *
 * This matters for glass: vanilla Minecraft sensing treats glass/panes as LoS blockers, while the
 * public AI branch treats them as transparent/breakable tactical clutter. The goal therefore keeps
 * vanilla-like target eligibility checks, disables only vanilla LoS, and then applies
 * TacticalLineOfSightHelper explicitly.
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

        this.target = this.findNearestTacticalTarget();
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
        super.start();
    }

    @Override
    public void stop() {
        this.tacticalUnseenTicks = 0;
        super.stop();
    }

    @Override
    public VerticalAwareTargetGoal<T> setUnseenMemoryTicks(int unseenMemoryTicks) {
        super.setUnseenMemoryTicks(unseenMemoryTicks);
        this.tacticalUnseenMemoryTicks = unseenMemoryTicks;
        return this;
    }

    @Nullable
    private T findNearestTacticalTarget() {
        double followRange = this.getFollowDistance();
        AABB searchArea = this.getTargetSearchArea(followRange);
        List<T> candidates = this.mob.level().getEntitiesOfClass(this.targetClass, searchArea, this::isValidBaseTarget);

        candidates.sort(Comparator.comparingDouble(candidate -> this.mob.distanceToSqr(candidate)));

        int tacticalLosChecks = 0;
        for (T candidate : candidates) {
            if (tacticalLosChecks++ >= MAX_TACTICAL_LOS_CANDIDATES) {
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
