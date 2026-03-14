package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class LongPatrolGoal extends WaterAvoidingRandomStrollGoal {

    private final int patrolRadius;
    private final int verticalSearchRange;
    private final float minDistanceSqr;


    public LongPatrolGoal(PathfinderMob mob, double speedModifier, int patrolRadius) {
        super(mob, speedModifier);
        this.patrolRadius = patrolRadius;
        this.verticalSearchRange = 7;
        float minDist = patrolRadius * 0.4F;
        this.minDistanceSqr = minDist * minDist;
        this.setFlags(EnumSet.of(Flag.MOVE)); // test
    }

    @Override
    @Nullable
    protected Vec3 getPosition() {
        if (this.mob.isInWaterOrBubble()) {
            return super.getPosition();
        }

        Vec3 target = LandRandomPos.getPos(this.mob, this.patrolRadius, this.verticalSearchRange);

        if (target != null && target.distanceToSqr(this.mob.position()) >= this.minDistanceSqr) {
            return target;
        }

        Vec3 forwardTarget = LandRandomPos.getPosTowards(
                this.mob,
                this.patrolRadius,
                this.verticalSearchRange,
                this.mob.getLookAngle()
        );

        if (forwardTarget != null && forwardTarget.distanceToSqr(this.mob.position()) >= this.minDistanceSqr) {
            return forwardTarget;
        }

        return null;
    }
}
