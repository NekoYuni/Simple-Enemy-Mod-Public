package net.nekoyuni.SimpleEnemyMod.entity.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class VerticalAwareTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private final double verticalRange;

    public VerticalAwareTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee, double verticalRange) {
        super(mob, targetClass, mustSee);
        this.verticalRange = verticalRange;
    }

    public VerticalAwareTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee,
                                   double verticalRange, @Nullable Predicate<LivingEntity> predicate) {
        super(mob, targetClass, 10, mustSee, false, predicate);
        this.verticalRange = verticalRange;
    }

    @Override
    protected AABB getTargetSearchArea(double followRange) {
        return this.mob.getBoundingBox().inflate(followRange, verticalRange, followRange);
    }
}
