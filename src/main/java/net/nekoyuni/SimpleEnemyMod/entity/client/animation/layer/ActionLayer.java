package net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition.IAnimationCondition;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.IAnimatedEntity;

import javax.annotation.Nullable;

public class ActionLayer extends AbstractAnimationLayer {

    private final AnimationDefinition[] animationVariants;
    private int currentVariantIndex = 0;

    private final float configDurationSeconds;
    private int durationTicks;
    private int ticksRemaining = 0;
    private int lastTickSeen = -1;

    private int lastTriggerValue = 0;
    private boolean waitingForReset = false;

    private final TriggerDetector triggerDetector;

    // DEBUG
    private final boolean isDebug = false;

    @FunctionalInterface
    public interface TriggerDetector {
        int getCurrentValue(Entity entity);
    }

    public ActionLayer(
            String name,
            AnimationState animationState,
            AnimationDefinition animationDefinition,
            AnimationPriority priority,
            float durationSeconds,
            TriggerDetector triggerDetector,
            @Nullable IAnimationCondition additionalCondition
    ) {
        this(name, animationState, new AnimationDefinition[]{animationDefinition},
                priority, durationSeconds, triggerDetector, additionalCondition);
    }

    public ActionLayer(
            String name,
            AnimationState animationState,
            AnimationDefinition[] animationVariants,
            AnimationPriority priority,
            float durationSeconds,
            TriggerDetector triggerDetector,
            @Nullable IAnimationCondition additionalCondition
    ) {
        super(name, animationState, animationVariants[0], priority, additionalCondition);
        this.animationVariants = animationVariants;
        this.configDurationSeconds = durationSeconds;
        this.triggerDetector = triggerDetector;
    }

    @Override
    public boolean canPlay(Entity entity, int tickCount) {
        int currentTriggerValue = triggerDetector.getCurrentValue(entity);

        if (currentTriggerValue == 0) {
            waitingForReset = false;
            lastTriggerValue = 0;
            return false;
        }

        if (waitingForReset) {
            if (currentTriggerValue > lastTriggerValue) {
                waitingForReset = false;
            } else {
                lastTriggerValue = currentTriggerValue;
                return false;
            }
        }

        boolean isNewTrigger = currentTriggerValue > lastTriggerValue;
        lastTriggerValue = currentTriggerValue;

        if (isNewTrigger) {

            if (isDebug) {
                System.out.println("[ActionLayer:" + getName() + "] Valid Trigger! Value: " + currentTriggerValue);
            }

            return true;
        }

        if (isPlaying() && ticksRemaining > 0) {
            return true;
        }

        return false;
    }

    @Override
    protected void onStart(Entity entity, int tickCount) {

        if (animationVariants.length > 1) {
            currentVariantIndex = entity.level().getRandom().nextInt(animationVariants.length);

            if (entity instanceof IAnimatedEntity animatedEntity) {
                animatedEntity.onAnimationVariantSelected(getName(), currentVariantIndex);
            }
        } else {
            currentVariantIndex = 0;
        }

        AnimationDefinition selectedVariant = animationVariants[currentVariantIndex];

        int animDurationTicks = (int) (selectedVariant.lengthInSeconds() * 20f);
        int configDurationTicks = (int) (configDurationSeconds * 20f);
        this.durationTicks = Math.max(animDurationTicks, configDurationTicks);

        if (this.durationTicks <= 0) this.durationTicks = 1;

        this.ticksRemaining = this.durationTicks;

        if (isDebug) {
            System.out.println("[ActionLayer:" + getName() + "] START. Time: " + durationTicks + " ticks");
        }
    }

    @Override
    protected void onUpdate(Entity entity, int tickCount) {
        if (tickCount != lastTickSeen) {

            if (ticksRemaining > 0) {
                ticksRemaining--;
            }

            lastTickSeen = tickCount;

            if (isDebug) {
                System.out.println("[ActionLayer] Real Tick: " + tickCount + " | Remaining: " + ticksRemaining);
            }
        }
    }

    @Override
    protected void onStop() {
        ticksRemaining = 0;
        waitingForReset = true;
        lastTickSeen = -1;

        if (isDebug) {
            System.out.println("[ActionLayer:" + getName() +
                    "] STOP. Starting waiting for reset (waitingForReset=true). LastValue=" + lastTriggerValue
            );
        }
    }

    @Override
    public boolean isPlaying() {
        return super.isPlaying() && ticksRemaining > 0;
    }

    public AnimationDefinition getCurrentVariant() {
        return animationVariants[currentVariantIndex];
    }
}