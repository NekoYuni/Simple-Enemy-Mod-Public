package net.nekoyuni.SimpleEnemyMod.entity.client.animation.core;

import net.minecraft.world.entity.Entity;


/**
 * Data context passed during animation updates.
 * Useful for avoiding recalculating values ​​multiple times.
 */
public class AnimationUpdateContext {

    private final Entity entity;
    private final int tickCount;

    private Boolean isMoving;
    private Boolean isDead;

    public AnimationUpdateContext(Entity entity, int tickCount) {
        this.entity = entity;
        this.tickCount = tickCount;
    }

    public Entity getEntity() {
        return entity;
    }

    public int getTickCount() {
        return tickCount;
    }

    public boolean isMoving() {
        if (isMoving == null) {
            isMoving = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        }
        return isMoving;
    }

    public boolean isDead() {
        if (isDead == null) {
            isDead = !entity.isAlive();
        }
        return isDead;
    }
}
