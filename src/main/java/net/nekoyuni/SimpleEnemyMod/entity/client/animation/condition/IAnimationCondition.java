package net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface IAnimationCondition {

    /**
     * Determines if the animation can be played in the current context.
     *
     @param entity The evaluated entity
     @param tickCount The current tick count of the game
     @return true if the animation can be played
     */

    boolean test(Entity entity, int tickCount);

    default IAnimationCondition and(IAnimationCondition other) {
        return (entity, tick) -> this.test(entity, tick) && other.test(entity, tick);
    }

    default IAnimationCondition or(IAnimationCondition other) {
        return (entity, tick) -> this.test(entity, tick) || other.test(entity, tick);
    }

    default IAnimationCondition negate() {
        return (entity, tick) -> !this.test(entity, tick);
    }

}
