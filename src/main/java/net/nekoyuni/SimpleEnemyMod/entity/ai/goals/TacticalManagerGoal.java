package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.AIDifficultySettings;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.OrderType;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class TacticalManagerGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger(TacticalManagerGoal.class);
    private static final boolean isDebug = false;

    private final AbstractUnit unit;

    private final float RETREAT_HEALTH_PCT = 0.35f;
    private final double ENGAGE_DIST_FAR = 64.0;
    private final double ENGAGE_DIST_MID = 15.0;

    private LivingEntity lastTarget = null;
    private long lastLogTime = 0;

    private boolean isForcedRush = false;
    private long forcedRushStartTick = 0;
    private static final long MAX_RUSH_TIME = 200;

    private final long PATIENCE_TIMEOUT;
    private final long FLANKING_DURATION;
    private final long COVER_WAIT_TIME;
    private final double RUSH_SPEED;


    public TacticalManagerGoal(AbstractUnit unit) {
        this.unit = unit;
        this.setFlags(EnumSet.noneOf(Flag.class));

        AIDifficultySettings settings = AIDifficultySettings.fromConfig();
        this.PATIENCE_TIMEOUT = settings.patienceTimeout;
        this.FLANKING_DURATION = settings.flankingDuration;
        this.COVER_WAIT_TIME = settings.coverWaitTime;
        this.RUSH_SPEED = settings.rushSpeed;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        long time = unit.level().getGameTime();

        if (unit instanceof PmcUnitEntity pmc) {
            OrderType order = pmc.getOrder();

            if (order == OrderType.HOLD_POSITION) {

                if (unit.getSoldierState() != SoldierState.ENGAGE && unit.getSoldierState() != SoldierState.IDLE) {
                    unit.setSoldierState(SoldierState.ENGAGE);
                }
                return;
            }
        }

        if (time - lastLogTime > 40) {
            lastLogTime = time;
        }

        LivingEntity target = unit.getTarget();

        if (target != lastTarget) {
            isForcedRush = false;
            unit.releaseMovementLock();
        }

        if (isForcedRush && (time - forcedRushStartTick > MAX_RUSH_TIME)) {
            isForcedRush = false;
            logDecision("RUSH FAILED (Timeout) - Returning to normal logic");
        }

        if (target == null || !target.isAlive() || target.level() != unit.level()) {
            isForcedRush = false;

            if (unit.getSoldierState() != SoldierState.IDLE) {
                unit.setSoldierState(SoldierState.IDLE);
                logDecision("NO TARGET - Setting IDLE");
            }
            lastTarget = null;
            unit.setFlankingActive(false);
            return;
        }

        if (lastTarget == null) {
            unit.setSoldierState(SoldierState.SEEK_COVER);
            lastTarget = target;
            logDecision("NEW TARGET DETECTED");
            return;
        }
        lastTarget = target;

        double distance = unit.distanceTo(target);
        boolean hasLOS = unit.hasLineOfSight(target);
        boolean isLowHealth = (unit.getHealth() / unit.getMaxHealth()) < RETREAT_HEALTH_PCT;
        SoldierState currentState = unit.getSoldierState();

        if (hasLOS) {
            unit.setLastKnownTargetPos(target.position());
            unit.setLastSeenTargetTick(time);
        }

        // LowHealth TODO
        if (isLowHealth) {
            if (unit.isFlankingActive()) {
                unit.setFlankingActive(false);
                logDecision("FLANK CANCELLED (Low Health)");
            }

            if (currentState == SoldierState.HOLD_COVER) {
                return;
            }
            if (currentState != SoldierState.SEEK_COVER) {
                unit.setSoldierState(SoldierState.SEEK_COVER);
            }
            return;
        }

        // Strategy
        StrategyType idealStrategy = calculateIdealStrategy(distance, hasLOS);

        if (idealStrategy != unit.getStrategy()) {
            unit.setStrategy(idealStrategy);
            logDecision("STRATEGY → " + idealStrategy + " (D:" + String.format("%.1f", distance) + ", LoS:" + hasLOS + ")");
        }

        switch (unit.getStrategy()) {
            case MID_RANGE, LONG_RANGE -> {
                handleMidRangeFlanking(target, time, hasLOS, distance, currentState);
            }
            case CLOSE_RANGE -> {
                handleCloseRange(target, time, hasLOS, distance, currentState);
            }
        }
    }


    private StrategyType calculateIdealStrategy(double distance, boolean hasLOS) {

        if (distance <= ENGAGE_DIST_MID) {

            if (isForcedRush) {
                isForcedRush = false;
                logDecision("RUSH SUCCESSFUL - Entering natural CLOSE_RANGE");
            }
            if (unit.isFlankingActive()) {
                unit.setFlankingActive(false);
                logDecision("FLANK AUTO-CANCELLED (Entered Close Range)");
            }

            return StrategyType.CLOSE_RANGE;
        }

        if (isForcedRush) {
            return StrategyType.CLOSE_RANGE;
        }

        if (distance > ENGAGE_DIST_FAR) {
            return StrategyType.LONG_RANGE;
        } else {
            return StrategyType.MID_RANGE;
        }
    }


    private void handleMidRangeFlanking(LivingEntity target, long time, boolean hasLOS,
                                        double distance, SoldierState currentState) {

        long timeSinceLastSeen = time - unit.getLastSeenTargetTick();

        // With LoS
        if (hasLOS) {
            if (unit.isFlankingActive()) {
                unit.setFlankingActive(false);
                unit.releaseMovementLock(); // LOCK TEST

                logDecision("FLANK CANCELLED (LoS Recovered)");
            }

            if (currentState == SoldierState.TACTICAL_MOV || currentState == SoldierState.ENGAGE) {
                unit.setSoldierState(SoldierState.SEEK_COVER);
                unit.lockMovementForStrategy(unit.getStrategy()); // LOCK TEST

                logDecision("SEEKING COVER (Strategy: " + unit.getStrategy() + ")");
            }
            return;
        }

        // Without LoS - Waiting
        if (timeSinceLastSeen < PATIENCE_TIMEOUT) {

            if (!unit.isFlankingActive()) {

                if (currentState != SoldierState.HOLD_COVER && currentState != SoldierState.SEEK_COVER) {
                    unit.setSoldierState(SoldierState.SEEK_COVER);
                    unit.lockMovementForStrategy(unit.getStrategy()); // LOCK TEST
                }

                if (time % 40 == 0) {
                    logDecision("WAITING (Patience: " + timeSinceLastSeen + "/" + PATIENCE_TIMEOUT + ")");
                }
            }
            return;
        }

        // Start Flanking
        if (!unit.isFlankingActive()) {
            unit.setFlankingActive(true);
            unit.setFlankingStartTick(time);
            unit.releaseMovementLock(); // LOCK TEST

            Vec3 pivotPos = (target != null) ? target.position() : unit.getLastKnownTargetPos();

            if (pivotPos == null) {
                unit.setFlankingActive(false);
                return;
            }

            double dx = unit.getX() - pivotPos.x;
            double dz = unit.getZ() - pivotPos.z;

            float currentAngle = (float) Math.toDegrees(Math.atan2(dz, dx));

            if (currentAngle < 0) {
                currentAngle += 360f;
            }

            unit.setFlankingAngle(currentAngle);
            unit.setFlankingDirection(unit.getRandom().nextBoolean() ? 1 : -1);
            unit.setSoldierState(SoldierState.TACTICAL_MOV);

            logDecision("STARTING FLANK (Angle: " + String.format("%.1f", currentAngle) + "°)");
            return;
        }

        // Flanking Timeout
        long flankingElapsed = time - unit.getFlankingStartTick();

        if (flankingElapsed > FLANKING_DURATION) {
            unit.setFlankingActive(false);
            unit.releaseMovementLock(); // LOCK TEST

            this.isForcedRush = true;
            this.forcedRushStartTick = time;

            Vec3 rushTarget = unit.getLastKnownTargetPos();
            if (rushTarget == null && target != null) {
                rushTarget = target.position();
            }

            if (rushTarget != null) {
                unit.getNavigation().moveTo(rushTarget.x, rushTarget.y, rushTarget.z, RUSH_SPEED);
                unit.setSoldierState(SoldierState.TACTICAL_MOV);

                logDecision("FLANK TIMEOUT → ACTIVATING FORCED RUSH to " +
                        String.format("(%.1f, %.1f, %.1f)", rushTarget.x, rushTarget.y, rushTarget.z));
            }
            return;
        }

        // Flanking Active
        if (time % 40 == 0) {
            logDecision("FLANKING [" + currentState + "] Progress: " + flankingElapsed + "/" + FLANKING_DURATION);
        }

        switch (currentState) {
            case HOLD_COVER -> {

                if (time % COVER_WAIT_TIME == 0) {
                    unit.releaseMovementLock(); // LOCK TEST
                    unit.setSoldierState(SoldierState.TACTICAL_MOV);

                    logDecision("FLANK: Continuing to next arc position");
                }
            }

            case SEEK_COVER -> {
                // SeekCoverGoal
            }

            case TACTICAL_MOV -> {
                // TacticalManeuverGoal
            }

            case ENGAGE -> {
                unit.setSoldierState(SoldierState.TACTICAL_MOV);
                logDecision("FLANK: Forcing back to TACTICAL_MOV from ENGAGE");
            }

            case IDLE -> {
                unit.setSoldierState(SoldierState.TACTICAL_MOV);
                logDecision("FLANK: Waking up from IDLE");
            }

            default -> {
                unit.setSoldierState(SoldierState.TACTICAL_MOV);
                logDecision("FLANK: Unexpected state " + currentState + ", resetting");
            }
        }
    }


    private void handleCloseRange(LivingEntity target, long time, boolean hasLOS,
                                  double distance, SoldierState currentState) {


        if (isForcedRush && distance <= ENGAGE_DIST_MID) {

            isForcedRush = false;
            unit.releaseMovementLock(); // LOCK TEST

            logDecision("RUSH COMPLETED - Now in Close Range combat");
        }

        if (!hasLOS && currentState != SoldierState.SEEK_COVER) {

            unit.setSoldierState(SoldierState.SEEK_COVER);
            unit.lockMovementForStrategy(StrategyType.CLOSE_RANGE); // 2s lock
            logDecision("🏃 CQB: Lost LoS, quick cover seek");

            return;
        }

        if (hasLOS && currentState == SoldierState.HOLD_COVER) {

            if (unit.getRandom().nextFloat() < 0.3f) {
                unit.setSoldierState(SoldierState.ENGAGE);
                unit.releaseMovementLock();
                logDecision("CQB: Peeking from cover");

            }
            return;
        }

        if (hasLOS) {
            if (currentState == SoldierState.ENGAGE && unit.getRandom().nextFloat() < 0.05f) {

                unit.setSoldierState(SoldierState.TACTICAL_MOV);
                unit.releaseMovementLock();

                logDecision("CQB: Repositioning while engaging");

            } else if (currentState != SoldierState.ENGAGE) {
                unit.setSoldierState(SoldierState.ENGAGE);

            }
        } else {
            unit.setSoldierState(SoldierState.TACTICAL_MOV);
            unit.releaseMovementLock();
        }

        if (time % 40 == 0) {
            logDecision("CLOSE_RANGE: " + currentState + " | D:" + String.format("%.1f", distance));
        }
    }


    private void logDecision(String reason) {

        if (!isDebug) return;

        long time = unit.level().getGameTime();
        long timeSinceSeen = time - unit.getLastSeenTargetTick();
        long flankDuration = unit.isFlankingActive() ? (time - unit.getFlankingStartTick()) : 0;

        LivingEntity target = unit.getTarget();
        double dist = (target != null) ? unit.distanceTo(target) : -1;
        boolean hasLos = (target != null) && unit.hasLineOfSight(target);

        LOGGER.debug(
                "[SEM-AI] T:{} | U:{} | St:{} | Str:{} | Flk:{}(T:{}) | Rush:{} | D:{} | LoS:{} | Ago:{} | {}",
                time,
                unit.getId(),
                unit.getSoldierState(),
                unit.getStrategy(),
                unit.isFlankingActive(),
                flankDuration,
                isForcedRush,
                dist,
                hasLos,
                timeSinceSeen,
                reason
        );
    }
}

