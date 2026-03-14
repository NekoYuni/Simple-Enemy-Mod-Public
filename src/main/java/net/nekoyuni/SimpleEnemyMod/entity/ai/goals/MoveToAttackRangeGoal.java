package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;

import java.util.EnumSet;

public class MoveToAttackRangeGoal extends Goal {

    private final Mob mob;
    private final double detectionRange;
    private final double attackRange;
    private final double speed;

    private LivingEntity target;
    private int cooldownTicks = 0;
    private static final int COOLDOWN_DURATION = 20;
    private static final double RANGE_BUFFER = 2.0;


    public MoveToAttackRangeGoal(Mob mob, double detectionRange, double attackRange, double speed) {
        this.mob = mob;
        this.detectionRange = detectionRange;
        this.attackRange = attackRange;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        this.target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (mob instanceof AbstractUnit unit) {
            SoldierState state = unit.getSoldierState();
            if (state == SoldierState.SEEK_COVER ||
                    state == SoldierState.HOLD_COVER ||
                    state == SoldierState.TACTICAL_MOV) {
                return false;
            }
        }

        double distanceToTarget = mob.distanceTo(target);

        return distanceToTarget > (attackRange + RANGE_BUFFER);
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (mob.getTarget() != target) {
            return false;
        }

        if (mob instanceof AbstractUnit unit) {
            SoldierState state = unit.getSoldierState();
            if (state == SoldierState.SEEK_COVER ||
                    state == SoldierState.HOLD_COVER ||
                    state == SoldierState.TACTICAL_MOV) {
                return false;
            }
        }

        double distanceToTarget = mob.distanceTo(target);

        return distanceToTarget > attackRange;
    }

    @Override
    public void start() {
        PathNavigation navigation = mob.getNavigation();
        navigation.moveTo(target, speed);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        this.target = null;
        this.cooldownTicks = COOLDOWN_DURATION;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            return;
        }

        double distanceToTarget = mob.distanceTo(target);

        if (distanceToTarget <= attackRange) {
            PathNavigation navigation = mob.getNavigation();
            navigation.stop();
            this.stop();
            return;
        }

        PathNavigation navigation = mob.getNavigation();
        if (!navigation.isInProgress() || navigation.getTargetPos() == null) {
            navigation.moveTo(target, speed);
        }

        if (mob.tickCount % 10 == 0) {

            if (navigation.getTargetPos() != null) {

                double distanceToNavTarget = Math.sqrt(navigation.getTargetPos().distSqr(target.blockPosition()));
                if (distanceToNavTarget > 5.0) {
                    navigation.moveTo(target, speed);
                }

            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

