package net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Common conditions for animations.
 * Use them directly or combine them with .and() / .or()
 */
public class AnimationConditions {

    public static final IAnimationCondition ALWAYS = (entity, tick) -> true;

    public static final IAnimationCondition NEVER = (entity, tick) -> false;

    public static final IAnimationCondition IS_MOVING = (entity, tick) ->
            entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;

    public static final IAnimationCondition IS_STATIONARY = IS_MOVING.negate();

    public static final IAnimationCondition IS_DEAD_OR_DYING = (entity, tick) -> {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.isDeadOrDying();
        }
        return !entity.isAlive();
    };

    public static final IAnimationCondition IS_ALIVE = IS_DEAD_OR_DYING.negate();

    public static final IAnimationCondition IS_ON_GROUND = (entity, tick) ->
            entity.onGround();

    public static final IAnimationCondition IS_IN_AIR = IS_ON_GROUND.negate();

    private AnimationConditions() {}


    /**
     * Condition that checks if a minimum amount of time has passed since the last event.
     * Useful for cooldowns.
     */
    public static IAnimationCondition ticksSince(int minTicks, TickProvider tickProvider) {
        return (entity, currentTick) -> {
            int lastTick = tickProvider.getLastTick(entity);
            return (currentTick - lastTick) >= minTicks;
        };
    }

    @FunctionalInterface
    public interface TickProvider {
        int getLastTick(Entity entity);
    }

    /**
     * Condition that checks if a value has changed since the last check.
     * Useful for detecting triggers like hurtTime.
     */
    public static IAnimationCondition valueChanged(ValueProvider provider) {
        return new IAnimationCondition() {
            private int lastValue = -1;

            @Override
            public boolean test(Entity entity, int tickCount) {
                int currentValue = provider.getValue(entity);
                boolean changed = currentValue != lastValue;
                lastValue = currentValue;
                return changed;
            }
        };
    }

    @FunctionalInterface
    public interface ValueProvider {
        int getValue(Entity entity);
    }
}
