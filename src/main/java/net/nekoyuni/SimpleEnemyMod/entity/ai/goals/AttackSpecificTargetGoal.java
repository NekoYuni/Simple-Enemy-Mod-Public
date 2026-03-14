package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.OrderType;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;

import java.util.EnumSet;

public class AttackSpecificTargetGoal extends Goal {
    private final PmcUnitEntity mob;

    public AttackSpecificTargetGoal(PmcUnitEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return mob.getOrder() == OrderType.ATTACK_THAT_TARGET && mob.getAttackTargetId() != -1;
    }

    @Override
    public void start() {
        forceUpdateTarget();
    }

    @Override
    public void tick() {
        if (mob.tickCount % 5 == 0) {
            forceUpdateTarget();
        }
    }

    private void forceUpdateTarget() {
        Entity target = mob.level().getEntity(mob.getAttackTargetId());

        if (target instanceof LivingEntity living && target.isAlive()) {
            mob.setTarget(living);
        } else {
            this.stop();
        }
    }

    @Override
    public void stop() {
        mob.setOrder(OrderType.FREE_FIRE);
        mob.setAttackTargetId(-1);
        mob.setTarget(null);
    }
}
