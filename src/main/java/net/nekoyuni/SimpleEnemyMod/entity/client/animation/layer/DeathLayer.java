package net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition.IAnimationCondition;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class DeathLayer extends AbstractAnimationLayer{

    private final Function<Entity, AnimationDefinition> variantSelector;
    private boolean hasStarted = false;

    private static final boolean isDebug = false;

    public DeathLayer(
            String name,
            AnimationState animationState,
            AnimationDefinition animationDefinition,
            @Nullable IAnimationCondition additionalCondition
    ) {
        this(name, animationState, entity -> animationDefinition, additionalCondition);
    }

    public DeathLayer(
            String name,
            AnimationState animationState,
            Function<Entity, AnimationDefinition> variantSelector,
            @Nullable IAnimationCondition additionalCondition

    ) {
        super(name, animationState, null, AnimationPriority.CRITICAL, additionalCondition);
        this.variantSelector = variantSelector;
    }

    @Override
    public boolean canPlay(Entity entity, int tickCount) {
        if (hasStarted) {
            return true;
        }

        if (entity instanceof LivingEntity livingEntity) {
            boolean isDying = livingEntity.isDeadOrDying();

            if (isDying && condition != null) {
                return condition.test(entity, tickCount);
            }

            return isDying;

        }

        return false;

    }

    @Override
    protected void onStart(Entity entity, int tickCount) {
        if (hasStarted) {
            return;
        }

        hasStarted = true;

        AnimationDefinition selectedVariant = variantSelector.apply(entity);

        if (isDebug) {
            System.out.println("[DeathLayer:" + getName() + "] DEATH STARTED - Animation: "
                    + (selectedVariant != null ? selectedVariant.toString() : "default"));
        }
    }

    @Override
    public void stop() {

        if (isDebug) {
            System.out.println("[DeathLayer:" + getName() + "] ATTEMPT FAILED TO STOP");
        }
    }

    @Override
    public AnimationPriority getPriority() {
        return AnimationPriority.CRITICAL;
    }

}
