package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.TacticalNavigationHelper;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;

import java.util.EnumSet;
import java.util.Locale;

/** Moves idle units toward recently heard hostile gunfire positions. */
public class TacticalInvestigateSoundGoal extends Goal {
    private final AbstractUnit unit;
    private long lastInvestigateSoundLogTick = -40L;

    public TacticalInvestigateSoundGoal(AbstractUnit unit) {
        this.unit = unit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return TacticalAiConfig.enableTacticalHearing()
                && this.unit.hasHeardThreat()
                && this.unit.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        Vec3 heardPosition = this.unit.getLastHeardEnemyPosition();
        if (heardPosition == null) {
            return;
        }

        this.unit.getLookControl().setLookAt(heardPosition.x, heardPosition.y + 1.0D, heardPosition.z);

        double reachedDistance = TacticalAiConfig.soundInvestigateReachedDistance();
        if (this.unit.position().distanceToSqr(heardPosition) <= reachedDistance * reachedDistance) {
            this.unit.clearHeardThreat();
            return;
        }

        if (this.unit.getNavigation().isDone()) {
            Vec3 destination = spreadInvestigationPoint(heardPosition);
            TacticalNavigationHelper.moveTo(
                    this.unit,
                    destination.x,
                    destination.y,
                    destination.z,
                    TacticalAiConfig.soundInvestigateSpeed()
            );
        }

        long now = this.unit.level().getGameTime();
        if (now - this.lastInvestigateSoundLogTick >= 40L) {
            this.lastInvestigateSoundLogTick = now;
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "INVESTIGATE_SOUND",
                    this.unit,
                    "pos=" + String.format(Locale.ROOT, "%.2f,%.2f,%.2f", heardPosition.x, heardPosition.y, heardPosition.z)
                            + " strength=" + String.format(Locale.ROOT, "%.2f", this.unit.getLastHeardEnemyStrength())
            );
        }
    }

    private Vec3 spreadInvestigationPoint(Vec3 heardPosition) {
        int hash = this.unit.getUUID().hashCode();
        double angle = (hash & 0xFFFF) / 65535.0D * Math.PI * 2.0D;
        double radius = 1.5D + Math.abs((hash >> 16) % 4);
        return heardPosition.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }
}
