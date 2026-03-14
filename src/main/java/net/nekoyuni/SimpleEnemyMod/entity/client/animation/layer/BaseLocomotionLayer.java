package net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition.IAnimationCondition;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;

import javax.annotation.Nullable;

public class BaseLocomotionLayer extends AbstractAnimationLayer {

    private final AnimationState idleState;
    private final AnimationState walkState;
    private final AnimationDefinition idleDefinition;
    private final AnimationDefinition walkDefinition;

    private int idleAnimationTimeout = 0;

    private final IAnimationCondition walkCondition;

    private LocomotionState currentState = LocomotionState.IDLE;

    private enum LocomotionState {
        IDLE, WALKING
    }

    // DEBUG
    private final boolean isDebug = false;


    public BaseLocomotionLayer(
            String name,
            AnimationState idleState,
            AnimationDefinition idleDefinition,
            AnimationState walkState,
            AnimationDefinition walkDefinition,
            AnimationPriority priority,
            @Nullable IAnimationCondition additionalCondition

    ) {
        super(name, idleState, idleDefinition, priority, additionalCondition);

        this.idleState = idleState;
        this.idleDefinition = idleDefinition;
        this.walkState = walkState;
        this.walkDefinition = walkDefinition;

        this.walkCondition = (entity, tick) ->
                entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    @Override
    public void play(Entity entity, int tickCount) {
        boolean shouldWalk = walkCondition.test(entity, tickCount);

        if (shouldWalk){
            transitionToWalk(tickCount);
        } else {
            transitionToIdle(entity, tickCount);
        }
    }

    private void transitionToWalk(int tickCount) {
        if (currentState != LocomotionState.WALKING) {

            if (idleState.isStarted()) {
                idleState.stop();
            }

            walkState.stop();
            walkState.start(tickCount);
            currentState = LocomotionState.WALKING;

            if (isDebug) {
                System.out.println("[BaseLocomotionLayer] Transition: IDLE → WALK");
            }
        }
    }

    private void transitionToIdle(Entity entity, int tickCount) {
        if (currentState != LocomotionState.IDLE) {

            if (walkState.isStarted()) {
                walkState.stop();
            }

            this.idleAnimationTimeout = 0;
            currentState = LocomotionState.IDLE;

            if (isDebug) {
                System.out.println("[BaseLocomotionLayer] Transition: WALK → IDLE");
            }
        }

        if (this.idleAnimationTimeout <= 0) {

            if (!idleState.isStarted()) {
                idleState.stop();
                idleState.start(tickCount);

                if (isDebug) {
                    System.out.println("[BaseLocomotionLayer] Idle AnimationState started");
                }
            }

            this.idleAnimationTimeout = entity.level().getRandom().nextInt(40) + 80;
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void stop() {
        if (idleState.isStarted()) {
            idleState.stop();
        }
        if (walkState.isStarted()) {
            walkState.stop();
        }
        currentState = LocomotionState.IDLE;
        idleAnimationTimeout = 0;

        if (isDebug) {
            System.out.println("[BaseLocomotionLayer] Stopped");
        }
    }

    @Override
    protected void onStart(Entity entity, int tickCount) {
        if (isDebug) {
            System.out.println("[BaseLocomotionLayer] Re-Activated - Resetting States");
        }

        boolean shouldWalk = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;

        if (shouldWalk) {
            currentState = LocomotionState.WALKING;
            walkState.stop();
            walkState.start(tickCount);

            if (isDebug) {
                System.out.println("[BaseLocomotionLayer] Started in WALK");
            }

        } else {
            currentState = LocomotionState.IDLE;
            idleAnimationTimeout = 0;
            idleState.stop();
            idleState.start(tickCount);

            if (isDebug) {
                System.out.println("[BaseLocomotionLayer] Started in IDLE");
            }
        }
    }

    @Override
    protected void onStop() {
        if (idleState.isStarted()) {
            idleState.stop();
        }
        if (walkState.isStarted()) {
            walkState.stop();
        }

        if (isDebug) {
            System.out.println("[BaseLocomotionLayer] STOPPED - Clean States");
        }
    }

    @Override
    public boolean isPlaying() {
        return idleState.isStarted() || walkState.isStarted();
    }


    public boolean isWalking() {
        return currentState == LocomotionState.WALKING;
    }

    public boolean isIdle() {
        return currentState == LocomotionState.IDLE;
    }

}
