package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;


public class NoPlayerHurtByTargetGoal extends HurtByTargetGoal {

    public NoPlayerHurtByTargetGoal(PathfinderMob pMob, Class<?>... pToIgnoreDamage) {
        super(pMob, pToIgnoreDamage);
    }

    @Override
    public boolean canUse() {

        if (!super.canUse()) {
            return false;
        }

        LivingEntity potentialTarget = this.mob.getLastHurtByMob();

        if (potentialTarget instanceof Player) {
            this.mob.setTarget(null);
            return false;
        }

        return true;
    }
}
