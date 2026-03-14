package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;

import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.AiGunSpreadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.EnumSet;

public class RangedGunAttackGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangedGunAttackGoal.class);

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    private static final boolean isDebug = false;

    private final Mob mob;
    private LivingEntity target;

    private final float MAX_SHOOT_DISTANCE_SQR;
    private final float BASE_SPREAD_DEGREES;
    private final float SPREAD_INCREASE_PER_BLOCK;
    private final int MIN_BURST_SHOTS;
    private final int MAX_BURST_SHOTS;
    private final int MIN_BURST_COOLDOWN_TICKS;
    private final int MAX_BURST_COOLDOWN_TICKS;

    private static final int MAX_TICKS_STUCK_ACTION = 100;
    private int ticksWaitingForBusyAction = 0;

    private enum GoalState {
        IDLE,
        BURST_FIRING,
        BURST_COOLDOWN
    }

    private GoalState currentGoalState = GoalState.IDLE;
    private int burstShotsFired = 0;
    private int currentBurstTarget = 0;
    private int burstCooldownTicks = 0;

    private int attackDelay = 0;
    private boolean cachedHasLoS = false;


    public RangedGunAttackGoal(Mob mob,
                               float maxShootDistance,
                               float baseSpread, float spreadIncrease,
                               int minBurst, int maxBurst,
                               int minBurstCooldown, int maxBurstCooldown) {
        this.mob = mob;
        this.MAX_SHOOT_DISTANCE_SQR = maxShootDistance * maxShootDistance;
        this.BASE_SPREAD_DEGREES = baseSpread;
        this.SPREAD_INCREASE_PER_BLOCK = spreadIncrease;
        this.MIN_BURST_SHOTS = minBurst;
        this.MAX_BURST_SHOTS = maxBurst;
        this.MIN_BURST_COOLDOWN_TICKS = minBurstCooldown;
        this.MAX_BURST_COOLDOWN_TICKS = maxBurstCooldown;
        this.setFlags(EnumSet.of(Flag.LOOK));

        debug(ANSI_BLUE + "[AI AttackGoal] Goal initialized for {}."
                + ANSI_RESET, this.mob.getName().getString());
    }

    @Override
    public boolean canUse() {
        LivingEntity currentTarget = this.mob.getTarget();

        if (currentTarget == null || !currentTarget.isAlive()) {
            return false;
        }

        if (!(this.mob.getMainHandItem().getItem() instanceof AbstractGunItem)) {
            return false;
        }

        if (this.mob.distanceToSqr(currentTarget) > this.MAX_SHOOT_DISTANCE_SQR) {
            return false;
        }

        return this.mob.getSensing().hasLineOfSight(currentTarget);
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive() &&
                this.mob.getMainHandItem().getItem() instanceof AbstractGunItem;
    }

    @Override
    public void start() {
        this.attackDelay = 3 + this.mob.getRandom().nextInt(5);

        this.target = this.mob.getTarget();

        debug("[AI AttackGoal] {} started. Target: {}",
                this.mob.getName().getString(), this.target != null ? this.target.getName().getString() : "null");

        this.burstShotsFired = 0;
        this.currentBurstTarget = 0;
        this.burstCooldownTicks = 0;
        this.currentGoalState = GoalState.IDLE;
        this.ticksWaitingForBusyAction = 0;

        IGunOperator operator = IGunOperator.fromLivingEntity(this.mob);
        operator.aim(true);

        if (this.target != null) {
            this.mob.getLookControl().setLookAt(this.target, 30F, 30F);
        }
    }

    @Override
    public void stop() {

        debug("[AI AttackGoal] {} stopped. Resetting states related to gun.", this.mob.getName().getString());

        this.target = null;
        this.burstShotsFired = 0;
        this.currentBurstTarget = 0;
        this.burstCooldownTicks = 0;
        this.currentGoalState = GoalState.IDLE;
        this.ticksWaitingForBusyAction = 0;

        IGunOperator operator = IGunOperator.fromLivingEntity(this.mob);
        operator.aim(false);
    }


    @Override
    public void tick() {

        if (this.attackDelay > 0) {
            this.attackDelay--;

            if (this.target != null) this.mob.getLookControl().setLookAt(this.target, 30F, 30F);
            return;
        }

        this.target = this.mob.getTarget();


        if (this.target == null || !this.target.isAlive() || this.mob.level().isClientSide) {

            if (currentGoalState != GoalState.IDLE) {
                resetGoalStates();
            }
            return;
        }

        this.mob.getLookControl().setLookAt(this.target, 30F, 30F);

        ItemStack gunStack = this.mob.getMainHandItem();

        if (!(gunStack.getItem() instanceof AbstractGunItem)) {
            if (currentGoalState != GoalState.IDLE) {
                resetGoalStates();
            }
            return;
        }

        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            if (currentGoalState != GoalState.IDLE) {
                resetGoalStates();
            }
            return;
        }

        IGunOperator operator = IGunOperator.fromLivingEntity(this.mob);

        boolean canSeeTargetRoughly = this.mob.getSensing().hasLineOfSight(this.target);
        if (canSeeTargetRoughly) {
            this.mob.getLookControl().setLookAt(this.target, 30F, 30F);
            this.mob.yBodyRot = this.mob.yHeadRot; // TEST
        }

        boolean isDrawingTACZ = operator.getSynDrawCoolDown() > 0;
        boolean isReloadingTACZ = operator.getSynReloadState().getStateType().isReloading();
        boolean isBoltingTACZ = operator.getSynIsBolting();
        boolean isBusyTACZ = isDrawingTACZ || isReloadingTACZ || isBoltingTACZ;

        if (isBusyTACZ) {
            this.ticksWaitingForBusyAction++;
            if (this.ticksWaitingForBusyAction > MAX_TICKS_STUCK_ACTION) {

                error(ANSI_RED + "[AI AttackGoal] Timeout! TACZ is stuck on an action. Forcing reset" + ANSI_RESET);

                resetGoalStates();
                return;
            }
        } else {
            this.ticksWaitingForBusyAction = 0;
        }

        double distanceSq = this.mob.distanceToSqr(this.target);
        boolean inShootRange = distanceSq <= this.MAX_SHOOT_DISTANCE_SQR;

        if ((this.mob.tickCount + this.mob.getId()) % 5 == 0) {
            this.cachedHasLoS = hasLineOfSightToTarget(this.target);
        }

        boolean hasClearLoSNow = this.cachedHasLoS;

        switch (this.currentGoalState) {
            case IDLE:
                handleIdleState(operator, gunStack, distanceSq, canSeeTargetRoughly, isBusyTACZ);
                break;
            case BURST_COOLDOWN:
                handleBurstCooldownState(inShootRange, canSeeTargetRoughly, hasClearLoSNow);
                break;
            case BURST_FIRING:
                handleBurstFiringState(operator, gunStack, distanceSq, canSeeTargetRoughly, hasClearLoSNow, iGun, isBusyTACZ);
                break;
        }
    }


    private void resetGoalStates() {
        this.burstShotsFired = 0;
        this.currentBurstTarget = 0;
        this.burstCooldownTicks = 0;
        this.currentGoalState = GoalState.IDLE;
        this.ticksWaitingForBusyAction = 0;
    }

    private void handleIdleState(
            IGunOperator operator,
            ItemStack gunStack,
            double distanceSq,
            boolean hasClearLoSNow,
            boolean isBusyTACZ) {

        boolean inShootRange = distanceSq <= MAX_SHOOT_DISTANCE_SQR;

        if (!inShootRange || !hasClearLoSNow) {
            return;
        }

        if (isBusyTACZ) {
            return;
        }

        if (operator.getSynDrawCoolDown() > 0) {
            info(ANSI_YELLOW + "[AI AttackGoal] IDLE: Weapon not drawn. Starting draw." + ANSI_RESET);

            operator.draw(() -> gunStack);
            return;
        }

        if (operator.getSynReloadState().getStateType().isReloading()) {
            info(ANSI_YELLOW + "[AI AttackGoal] IDLE: Out of ammo. Starting reload." + ANSI_RESET);

            return;
        }

        if (operator.getSynIsBolting()) {
            info(ANSI_YELLOW + "[AI AttackGoal] IDLE: Weapon needs bolt. Starting bolt." + ANSI_RESET);

            operator.bolt();
            return;
        }

        if (operator.getSynShootCoolDown() <= 0) {
            debug(ANSI_GREEN + "[AI AttackGoal] IDLE: Weapon ready to fire. Transitioning to BURST_FIRING." + ANSI_RESET);

            this.currentGoalState = GoalState.BURST_FIRING;
        }
    }


    private void handleBurstCooldownState(boolean inShootRange, boolean canSeeTargetRoughly, boolean hasClearLoSNow) {

        if (this.burstCooldownTicks > 0) {
            this.burstCooldownTicks--;
            trace("[AI AttackGoal] BURST_COOLDOWN: {} ticks remaining.", this.burstCooldownTicks);

        } else {
            debug(ANSI_BLUE + "[AI AttackGoal] BURST_COOLDOWN: Cooldown complete. Transitioning to IDLE" + ANSI_RESET);

            this.currentGoalState = GoalState.IDLE;
            this.burstShotsFired = 0;
            this.currentBurstTarget = 0;
        }

        if (!inShootRange || !canSeeTargetRoughly || !hasClearLoSNow) {
            debug(ANSI_YELLOW +
                    "[AI AttackGoal] BURST_COOLDOWN: Holding fire; LoS/range locked or out of range. Returning to IDLE." + ANSI_RESET);

            resetGoalStates();
        }
    }

    private void handleBurstFiringState(
            IGunOperator operator,
            ItemStack gunStack,
            double distanceSq,
            boolean canSeeTargetRoughly,
            boolean hasClearLoSNow,
            IGun iGun,
            boolean isBusyTACZ) {

        boolean inShootRange = distanceSq <= MAX_SHOOT_DISTANCE_SQR;

        if (!inShootRange || !canSeeTargetRoughly || !hasClearLoSNow) {
            debug(ANSI_YELLOW +
                    "[AI AttackGoal] BURST_FIRING: Holding fire; LoS/range locked or out of range. Returning to IDLE." + ANSI_RESET);

            resetGoalStates();
            this.burstCooldownTicks = this.MIN_BURST_COOLDOWN_TICKS;
            return;
        }

        if (isBusyTACZ) {
            info(ANSI_YELLOW +
                    "[AI AttackGoal] BURST_FIRING: TACZ is busy (draw/reload/bolt). Returning to IDLE to wait." + ANSI_RESET);

            resetGoalStates();
            return;
        }


        if (this.currentBurstTarget == 0) {
            RandomSource rand = this.mob.getRandom();
            this.currentBurstTarget = this.MIN_BURST_SHOTS + rand.nextInt(this.MAX_BURST_SHOTS - this.MIN_BURST_SHOTS + 1);
            this.burstShotsFired = 0;

            debug(ANSI_BLUE +
                    "[AI AttackGoal] BURST_FIRING: Starting a new burst of {} shots",
                    this.currentBurstTarget + ANSI_RESET
            );
        }

        if (this.burstShotsFired >= this.currentBurstTarget) {

            this.burstCooldownTicks = this.MIN_BURST_COOLDOWN_TICKS + this.mob.getRandom().nextInt(
                    this.MAX_BURST_COOLDOWN_TICKS - this.MIN_BURST_COOLDOWN_TICKS + 1
            );

            debug(ANSI_BLUE +
                    "[AI AttackGoal] Burst completed. Cooldown: {} ticks. Transitioning to BURST_COOLDOWN.",
                    this.burstCooldownTicks + ANSI_RESET
            );

            this.currentGoalState = GoalState.BURST_COOLDOWN;
            this.currentBurstTarget = 0;
            this.burstShotsFired = 0;

            return;
        }

        Vec3 eyePos = this.mob.getEyePosition();
        Vec3 targetCenter = this.target.position().add(0, this.target.getBbHeight() / 2.0, 0);
        Vec3 lookVec = targetCenter.subtract(eyePos).normalize();

        this.mob.getLookControl().setLookAt(targetCenter.x, targetCenter.y, targetCenter.z);
        this.mob.yBodyRot = this.mob.yHeadRot; // TEST

        double dx = lookVec.x;
        double dy = lookVec.y;
        double dz = lookVec.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float basePitch = Mth.wrapDegrees((float) (-(Math.atan2(dy, horizontal) * (180.0 / Math.PI))));
        float baseYaw = Mth.wrapDegrees((float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F);

        float distance = (float) eyePos.distanceTo(targetCenter);
        RandomSource random = this.mob.getRandom();

        float finalPitch = AiGunSpreadHelper.CalculateSpread(basePitch, distance, this.BASE_SPREAD_DEGREES,
                this.SPREAD_INCREASE_PER_BLOCK, random);

        float finalYaw = AiGunSpreadHelper.CalculateSpread(baseYaw, distance, this.BASE_SPREAD_DEGREES,
                this.SPREAD_INCREASE_PER_BLOCK, random);


        operator.aim(true);
        ShootResult shootResult = operator.shoot(() -> finalPitch, () -> finalYaw);


        switch (shootResult) {
            case SUCCESS:
                this.burstShotsFired++;
                trace(ANSI_GREEN +
                        "[AI AttackGoal] Successful shot! Burst {}/{}",
                        this.burstShotsFired, this.currentBurstTarget + ANSI_RESET
                );
                break;

            case NO_AMMO:
                info(ANSI_YELLOW + "[AI AttackGoal] BURST_FIRING: Out of burst ammo. Starting reload." + ANSI_RESET);
                operator.reload();
                resetGoalStates();
                break;

            case NOT_DRAW:
                info(ANSI_YELLOW + "[AI AttackGoal] BURST_FIRING: Weapon not drawn. Starting draw" + ANSI_RESET);
                operator.draw(() -> gunStack);
                resetGoalStates();
                break;

            case NEED_BOLT:
                info(ANSI_YELLOW + "[AI AttackGoal] BURST_FIRING: The weapon needs to bolt. Starting bolt." + ANSI_RESET);
                operator.bolt();
                resetGoalStates();
                break;

            case COOL_DOWN:
                trace("[AI AttackGoal] BURST_FIRING: Weapon on internal TACZ cooldown (ShootResult.COOL_DOWN). Waiting.");
                break;

            default:
                warn(ANSI_RED +
                        "[AI AttackGoal] BURST_FIRING: Uncontrolled shot result: " +
                        shootResult + ". Back to IDLE." + ANSI_RESET
                );
                resetGoalStates();
                break;
        }
    }


    private boolean hasLineOfSightToTarget(LivingEntity pTarget) {
        if (pTarget == null) return false;

        Level level = this.mob.level();

        Vec3 mobEyePos = this.mob.getEyePosition();
        Vec3 targetCenterPos = pTarget.getEyePosition();

        ClipContext context = new ClipContext(mobEyePos, targetCenterPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.mob);

        HitResult hitResult = level.clip(context);

        return hitResult.getType() == HitResult.Type.MISS;
    }


    private static void debug(String message, Object... args) {
        if (isDebug && LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    private static void info(String message, Object... args) {
        if (isDebug && LOGGER.isInfoEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    private static void warn(String message, Object... args) {
        if (isDebug && LOGGER.isWarnEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    private static void trace(String message, Object... args) {
        if (isDebug && LOGGER.isTraceEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    private static void error(String message, Object... args) {
        if (isDebug && LOGGER.isErrorEnabled()) {
            LOGGER.debug(message, args);
        }
    }
}
