package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.LadderRouteNavigator;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.TacticalNavigationHelper;
import net.nekoyuni.SimpleEnemyMod.entity.ai.tactical.TacticalCoverFinder;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SeekCoverGoal extends Goal {

    private static final int PROGRESS_SAMPLE_INTERVAL_TICKS = 10;
    private static final int STALL_RECOVERY_THRESHOLD_TICKS = 40;
    private static final int MAX_STALL_REPATH_ATTEMPTS = 1;
    private static final double MIN_POSITION_PROGRESS_SQR = 0.04D;
    private static final double MIN_TARGET_PROGRESS = 0.10D;

    private final AbstractUnit unit;
    private final Mob mob;
    private final double speed;
    private final int searchRadius;
    private int cooldownTicks = 0;
    private boolean abortedByManager = false;
    private String abortReason = "none";

    private boolean targetIsCover = false;
    private boolean targetIsEvasion = false;
    private boolean movementAbortRequested = false;
    private int progressSampleTicks = 0;
    private int stalledTicks = 0;
    private int repathAttempts = 0;
    private double lastDistanceToTarget = Double.MAX_VALUE;

    @Nullable
    private Vec3 lastProgressPosition;

    @Nullable
    private BlockPos targetPosition;

    @Nullable
    private TacticalCoverFinder.CoverSearchResult lastSearchResult;

    @Nullable
    private LivingEntity selectedThreat;

    @Nullable
    private Vec3 selectedThreatPosition;

    public SeekCoverGoal(AbstractUnit unit, double speed, int searchRadius) {
        this.unit = unit;
        this.mob = unit;
        this.speed = speed;
        this.searchRadius = Math.max(1, searchRadius);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!unit.isMovementLockedByManager()) {
            return false;
        }

        if (unit.getSoldierState() != SoldierState.SEEK_COVER) {
            return false;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (isVerticalTransitionInProgress()) {
            this.cooldownTicks = Math.max(20, TacticalAiConfig.coverSearchCooldownTicks());
            return false;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (!TacticalCoverFinder.tryAcquireCoverSearchSlot(unit)) {
            int jitter = Math.max(1, TacticalAiConfig.coverSearchBudgetJitterTicks());
            this.cooldownTicks = 1 + Math.floorMod(unit.getId(), jitter);
            return false;
        }

        this.selectedThreat = target;
        this.selectedThreatPosition = target.position();
        this.lastSearchResult = TacticalCoverFinder.findCoverOrEvasion(unit, target, this.searchRadius);
        if (this.lastSearchResult == null || !this.lastSearchResult.hasPosition()) {
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "COVER_SEARCH_FAILED",
                    unit,
                    "radius=" + this.searchRadius + " cooldown=" + TacticalAiConfig.coverSearchCooldownTicks()
            );
            this.selectedThreat = null;
            this.selectedThreatPosition = null;
            this.cooldownTicks = TacticalAiConfig.coverSearchCooldownTicks();
            return false;
        }

        this.targetPosition = this.lastSearchResult.position();
        this.targetIsCover = this.lastSearchResult.isCover();
        this.targetIsEvasion = this.lastSearchResult.isEvasion();
        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                this.targetIsCover ? "COVER_SELECTED" : "EVASION_SELECTED",
                unit,
                "mode=" + this.lastSearchResult.mode()
                        + " score=" + String.format(java.util.Locale.ROOT, "%.2f", this.lastSearchResult.score())
                        + " pos=" + this.targetPosition
        );
        return true;
    }

    private boolean isVerticalTransitionInProgress() {
        if (mob.onClimbable() || !mob.onGround()) {
            return true;
        }
        String ladderState = LadderRouteNavigator.debugState(unit);
        return ladderState != null && !"IDLE".equals(ladderState) && !ladderState.startsWith("FAILED_COOLDOWN");
    }

    @Override
    public void start() {
        if (this.targetPosition == null) {
            this.movementAbortRequested = true;
            this.abortReason = "missing_target_position";
            return;
        }

        // Once a cover move has been accepted, this goal owns movement until it reaches cover,
        // the selected threat changes, or the manager changes the soldier state. Public scenario
        // logs showed that short manager lock TTLs can expire while a valid cover path is still
        // being followed, causing false COVER_ABORTED events. Keep the lock alive locally instead
        // of treating lock expiry as an external manager abort.
        refreshMovementOwnership();
        resetProgressWatchdog(true);

        // Always let PathNavigation own terrain traversal. The former backward-strafe branch
        // stopped navigation and continuously drove MoveControl.strafe(), which cannot negotiate
        // one-block height changes and could walk forever against a block edge during combat.
        boolean pathStarted = startNavigationToTarget();
        if (!pathStarted) {
            this.movementAbortRequested = true;
            this.abortReason = "initial_path_failed";
        }

        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                this.targetIsCover ? "COVER_MOVE_START" : "EVASION_MOVE_START",
                unit,
                "pos=" + this.targetPosition
                        + " speed=" + String.format(java.util.Locale.ROOT, "%.2f", this.speed)
                        + " pathStarted=" + pathStarted
        );
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPosition == null) {
            this.abortedByManager = true;
            this.abortReason = "missing_target_position";
            return false;
        }

        if (this.movementAbortRequested) {
            return false;
        }

        LivingEntity currentThreat = mob.getTarget();

        if (unit.getSoldierState() != SoldierState.SEEK_COVER) {
            // Target acquisition can briefly clear/reassign the mob target while a valid
            // cover path is already in progress. TacticalManagerGoal may therefore see
            // a transient no-target tick and set IDLE even though the original threat is
            // still alive. Treat this as a recoverable manager hiccup, not a cover abort.
            if (unit.getSoldierState() == SoldierState.IDLE && hasRelevantThreat(currentThreat)) {
                unit.setSoldierState(SoldierState.SEEK_COVER);
                refreshMovementOwnership();
            } else {
                this.abortedByManager = true;
                this.abortReason = "state_changed_to_" + unit.getSoldierState();
                return false;
            }
        }

        if (!hasRelevantThreat(currentThreat)) {
            this.abortedByManager = false;
            this.abortReason = "threat_unavailable";
            return false;
        }

        refreshMovementOwnership();

        if (hasReachedTarget()) {
            return false;
        }

        // Ladder fallback can temporarily stop vanilla navigation while the active ladder route
        // manually drives motion, so do not interpret navigation.isDone() as failure during a
        // non-idle ladder route.
        if (isVerticalTransitionInProgress()) {
            return true;
        }

        if (mob.getNavigation().isDone()) {
            if (tryRecoverNavigation("navigation_done_before_reach")) {
                return true;
            }
            this.abortReason = "navigation_done_before_reach";
            return false;
        }

        return true;
    }

    @Override
    public void stop() {
        boolean reachedTarget = !abortedByManager
                && this.targetPosition != null
                && hasReachedTarget();

        if (reachedTarget && this.targetIsCover) {
            unit.setSoldierState(SoldierState.HOLD_COVER);
            unit.setCoverBlock(this.targetPosition);
            SemScenarioRuntimeManager.get().logTrackedEntityEvent("COVER_REACHED", unit, "pos=" + this.targetPosition);
        } else if (reachedTarget && this.targetIsEvasion) {
            unit.setSoldierState(SoldierState.ENGAGE);
            SemScenarioRuntimeManager.get().logTrackedEntityEvent("EVASION_REACHED", unit, "pos=" + this.targetPosition);
        } else if (this.targetPosition != null) {
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    this.targetIsCover ? "COVER_ABORTED" : "EVASION_ABORTED",
                    unit,
                    "pos=" + this.targetPosition
                            + " abortedByManager=" + abortedByManager
                            + " reason=" + abortReason
                            + " distance=" + String.format(java.util.Locale.ROOT, "%.2f", Math.sqrt(distanceToTargetCenterSqr()))
                            + " repathAttempts=" + this.repathAttempts
            );
        }

        mob.getNavigation().stop();
        LadderRouteNavigator.clear(unit);

        this.targetIsCover = false;
        this.targetIsEvasion = false;
        this.lastSearchResult = null;
        this.selectedThreat = null;
        this.selectedThreatPosition = null;
        this.lastProgressPosition = null;
        this.lastDistanceToTarget = Double.MAX_VALUE;
        this.progressSampleTicks = 0;
        this.stalledTicks = 0;
        this.repathAttempts = 0;
        this.movementAbortRequested = false;

        unit.releaseMovementLock();
        abortedByManager = false;
        abortReason = "none";
        this.targetPosition = null;
        unit.setCoverSearchOrigin(null);
        this.cooldownTicks = TacticalAiConfig.coverSearchCooldownTicks();
    }

    @Override
    public void tick() {
        refreshMovementOwnership();

        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        tickProgressWatchdog();

        // Let GoalSelector call stop() after canContinueToUse() becomes false. Calling stop()
        // manually from tick() can produce double-stop edge cases and stale abort flags.
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Cover movement is a short tactical commitment. Vanilla target goals and the SEM
     * scenario harness can legitimately switch a unit from one enemy to another while it
     * is already moving to a valid hard-cover position. Treat that as a threat update, not
     * as a failed cover path, as long as the replacement target is still a valid enemy.
     */
    private boolean hasRelevantThreat(@Nullable LivingEntity currentThreat) {
        if (isLivingThreat(this.selectedThreat)) {
            if (currentThreat != null && currentThreat != this.selectedThreat && isValidReplacementThreat(currentThreat)) {
                this.selectedThreat = currentThreat;
                this.selectedThreatPosition = currentThreat.position();
            }
            return true;
        }

        if (isValidReplacementThreat(currentThreat)) {
            this.selectedThreat = currentThreat;
            this.selectedThreatPosition = currentThreat.position();
            return true;
        }

        return false;
    }

    private boolean isLivingThreat(@Nullable LivingEntity threat) {
        return threat != null
                && threat.isAlive()
                && !threat.isRemoved()
                && threat.level() == mob.level();
    }

    private boolean isValidReplacementThreat(@Nullable LivingEntity threat) {
        if (!isLivingThreat(threat) || threat == mob) {
            return false;
        }

        if (!mob.canAttack(threat)) {
            return false;
        }

        if (UnitFactionTags.isSameTaggedFaction(unit, threat)) {
            return false;
        }

        // If the selected threat disappeared and the unit immediately retargeted another
        // nearby opponent, finishing the current cover path is usually better than
        // stutter-cancelling and starting a new expensive cover scan. For very large target
        // jumps, abort so the next SEEK_COVER pass can score cover against the new axis.
        if (this.selectedThreatPosition != null && threat.position().distanceToSqr(this.selectedThreatPosition) > 24.0D * 24.0D) {
            return false;
        }

        return true;
    }

    private boolean hasReachedTarget() {
        if (this.targetPosition == null) {
            return false;
        }
        return distanceToTargetCenterSqr() <= getReachRadius() * getReachRadius();
    }

    private double distanceToTargetCenterSqr() {
        if (this.targetPosition == null) {
            return Double.MAX_VALUE;
        }
        double dx = mob.getX() - (this.targetPosition.getX() + 0.5D);
        double dy = mob.getY() - this.targetPosition.getY();
        double dz = mob.getZ() - (this.targetPosition.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private double getReachRadius() {
        return this.targetIsCover ? 2.5D : 2.0D;
    }

    private void refreshMovementOwnership() {
        unit.lockMovementForManager(Math.max(20, TacticalAiConfig.coverSearchCooldownTicks()));
    }

    private boolean startNavigationToTarget() {
        if (this.targetPosition == null) {
            return false;
        }

        return TacticalNavigationHelper.moveTo(
                mob,
                this.targetPosition.getX() + 0.5D,
                this.targetPosition.getY(),
                this.targetPosition.getZ() + 0.5D,
                this.speed
        );
    }

    private void tickProgressWatchdog() {
        if (this.targetPosition == null || this.movementAbortRequested || hasReachedTarget()) {
            return;
        }

        // Ladder traversal and ordinary jumps intentionally suspend ground navigation. The
        // ladder helper owns its own timeout, so sampling here would produce false stalls.
        if (isVerticalTransitionInProgress()) {
            resetProgressWatchdog(false);
            return;
        }

        if (++this.progressSampleTicks < PROGRESS_SAMPLE_INTERVAL_TICKS) {
            return;
        }
        this.progressSampleTicks = 0;

        Vec3 currentPosition = mob.position();
        double currentDistance = Math.sqrt(distanceToTargetCenterSqr());
        if (this.lastProgressPosition == null || this.lastDistanceToTarget == Double.MAX_VALUE) {
            this.lastProgressPosition = currentPosition;
            this.lastDistanceToTarget = currentDistance;
            return;
        }

        boolean positionProgress = currentPosition.distanceToSqr(this.lastProgressPosition) >= MIN_POSITION_PROGRESS_SQR;
        boolean targetProgress = currentDistance <= this.lastDistanceToTarget - MIN_TARGET_PROGRESS;

        if (positionProgress || targetProgress) {
            this.stalledTicks = 0;
        } else {
            this.stalledTicks += PROGRESS_SAMPLE_INTERVAL_TICKS;
        }

        this.lastProgressPosition = currentPosition;
        this.lastDistanceToTarget = currentDistance;

        if (this.stalledTicks < STALL_RECOVERY_THRESHOLD_TICKS) {
            return;
        }

        if (tryRecoverNavigation("no_progress")) {
            return;
        }

        this.movementAbortRequested = true;
        this.abortReason = this.repathAttempts >= MAX_STALL_REPATH_ATTEMPTS
                ? "stalled_after_repath"
                : "stalled_repath_failed";
        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                this.targetIsCover ? "COVER_STALL_ABORT" : "EVASION_STALL_ABORT",
                unit,
                "pos=" + this.targetPosition
                        + " distance=" + String.format(java.util.Locale.ROOT, "%.2f", currentDistance)
                        + " reason=" + this.abortReason
        );
    }

    private boolean tryRecoverNavigation(String reason) {
        if (this.targetPosition == null || this.repathAttempts >= MAX_STALL_REPATH_ATTEMPTS) {
            return false;
        }

        this.repathAttempts++;
        mob.getNavigation().stop();
        LadderRouteNavigator.clear(unit);
        boolean pathStarted = startNavigationToTarget();

        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                this.targetIsCover ? "COVER_REPATH" : "EVASION_REPATH",
                unit,
                "pos=" + this.targetPosition
                        + " attempt=" + this.repathAttempts
                        + " reason=" + reason
                        + " pathStarted=" + pathStarted
        );

        if (pathStarted) {
            resetProgressWatchdog(false);
        }
        return pathStarted;
    }

    private void resetProgressWatchdog(boolean resetRepathAttempts) {
        this.progressSampleTicks = 0;
        this.stalledTicks = 0;
        this.lastProgressPosition = mob.position();
        this.lastDistanceToTarget = this.targetPosition == null
                ? Double.MAX_VALUE
                : Math.sqrt(distanceToTargetCenterSqr());
        if (resetRepathAttempts) {
            this.repathAttempts = 0;
            this.movementAbortRequested = false;
        }
    }
}
