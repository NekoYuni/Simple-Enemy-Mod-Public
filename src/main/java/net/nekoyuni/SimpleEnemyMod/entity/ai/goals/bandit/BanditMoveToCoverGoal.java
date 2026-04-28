package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.bandit;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.BanditUnitEntity;

import java.util.EnumSet;

public class BanditMoveToCoverGoal extends Goal {

    private final BanditUnitEntity bandit;

    public BanditMoveToCoverGoal(BanditUnitEntity bandit) {
        this.bandit = bandit;
        this.setFlags(EnumSet.of(Flag.MOVE)); // Cover movement must override normal combat movement.
    }

    @Override
    public boolean canUse() {
        return CommonConfig.BANDIT_RETREAT_ENABLED.get() && this.bandit.isMovingToCover();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.moveToCover(); // Start pathing immediately when the cover phase begins.
    }

    @Override
    public void tick() {
        Vec3 coverPos = this.bandit.getRetreatCoverPos();
        if (coverPos == null) {
            this.bandit.onCoverPathFailed();
            return;
        }

        if (!this.bandit.getNavigation().isInProgress()) {
            this.moveToCover(); // Retry movement if navigation gives up before the entity state times out.
        }
    }

    @Override
    public void stop() {
        this.bandit.getNavigation().stop(); // Clear leftover cover path when switching to full retreat.
    }

    private void moveToCover() {
        Vec3 coverPos = this.bandit.getRetreatCoverPos();
        if (coverPos == null) {
            this.bandit.onCoverPathFailed();
            return;
        }

        boolean moving = this.bandit.getNavigation().moveTo(
                coverPos.x,
                coverPos.y,
                coverPos.z,
                CommonConfig.BANDIT_RETREAT_SPEED.get()
        );

        if (!moving) {
            this.bandit.onCoverPathFailed(); // Bad path: fall back to the existing full-retreat behavior.
        }
    }
}
