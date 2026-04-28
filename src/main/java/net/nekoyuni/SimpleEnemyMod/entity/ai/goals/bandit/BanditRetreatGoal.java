package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.bandit;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.BanditUnitEntity;

import java.util.EnumSet;

public class BanditRetreatGoal extends Goal {

    private final BanditUnitEntity bandit;
    private int repathCooldown;

    public BanditRetreatGoal(BanditUnitEntity bandit) {
        this.bandit = bandit;
        this.setFlags(EnumSet.of(Flag.MOVE)); // Full retreat must override patrol, combat, and tactical movement.
    }

    @Override
    public boolean canUse() {
        return CommonConfig.BANDIT_RETREAT_ENABLED.get() && this.bandit.isFullRetreating();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
        this.moveAwayFromThreat(); // Pick a retreat destination as soon as the goal starts.
    }

    @Override
    public void tick() {
        if (this.repathCooldown > 0) {
            this.repathCooldown--;
        }

        if (!this.bandit.getNavigation().isInProgress() || this.repathCooldown <= 0) {
            this.moveAwayFromThreat(); // Keep updating retreat direction so the bandit does not stall.
        }
    }

    @Override
    public void stop() {
        this.bandit.getNavigation().stop(); // Avoid carrying retreat navigation into another AI state.
    }

    private void moveAwayFromThreat() {
        LivingEntity threat = this.bandit.getRetreatThreat();
        if (threat == null || !threat.isAlive()) {
            this.bandit.stopRetreating(); // Current design: no valid threat means no reason to keep retreating.
            return;
        }

        Vec3 retreatTarget = DefaultRandomPos.getPosAway(
                this.bandit,
                CommonConfig.BANDIT_RETREAT_SEARCH_HORIZONTAL_RANGE.get(),
                CommonConfig.BANDIT_RETREAT_SEARCH_VERTICAL_RANGE.get(),
                threat.position()
        );

        if (retreatTarget == null) {
            Vec3 away = this.bandit.position().subtract(threat.position());
            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(this.bandit.getRandom().nextDouble() - 0.5D, 0.0D, this.bandit.getRandom().nextDouble() - 0.5D);
            }

            retreatTarget = this.bandit.position().add(
                    away.normalize().scale(CommonConfig.BANDIT_RETREAT_FALLBACK_DISTANCE.get())
            ); // Fallback keeps movement deterministic even when random path search fails.
        }

        this.bandit.getNavigation().moveTo(
                retreatTarget.x,
                retreatTarget.y,
                retreatTarget.z,
                CommonConfig.BANDIT_RETREAT_SPEED.get()
        );
        this.repathCooldown = 20; // Recalculate roughly once per second.
    }
}
