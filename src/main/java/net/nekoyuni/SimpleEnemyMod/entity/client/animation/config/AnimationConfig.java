package net.nekoyuni.SimpleEnemyMod.entity.client.animation.config;


import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition.IAnimationCondition;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.LayeredAnimationManager;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer.ActionLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer.BaseLocomotionLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer.DeathLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural.IProceduralLayer;

import javax.annotation.Nullable;
import java.util.function.Function;

public class AnimationConfig {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final LayeredAnimationManager.Builder managerBuilder = LayeredAnimationManager.builder();

        // LOCOMOTION LAYER

        /**
         * Add locomotion layer (idle + walk).
         */
        public Builder addLocomotionLayer(
                String name,
                AnimationState idleState,
                AnimationDefinition idleDefinition,
                AnimationState walkState,
                AnimationDefinition walkDefinition,
                AnimationPriority priority
        ) {
            return addLocomotionLayer(name, idleState, idleDefinition,
                    walkState, walkDefinition, priority, null);
        }

        public Builder addLocomotionLayer(
                String name,
                AnimationState idleState,
                AnimationDefinition idleDefinition,
                AnimationState walkState,
                AnimationDefinition walkDefinition,
                AnimationPriority priority,
                @Nullable IAnimationCondition condition
        ) {
            BaseLocomotionLayer layer = new BaseLocomotionLayer(
                    name, idleState, idleDefinition, walkState, walkDefinition, priority, condition
            );
            managerBuilder.addAnimationLayer(layer);
            return this;
        }

        // ACTION LAYER

        /**
         * Adds a single action layer (one animation).
         * Useful for: hurt, attack, reload, etc.
         */
        public ActionLayerBuilder addActionLayer(
                String name,
                AnimationState animationState,
                AnimationDefinition animationDefinition
        ) {
            return new ActionLayerBuilder(this, name, animationState,
                    new AnimationDefinition[]{animationDefinition});
        }

        /**
         * Adds an action layer with multiple variations (e.g., hurt with 2 animations).
         */
        public ActionLayerBuilder addActionLayer(
                String name,
                AnimationState animationState,
                AnimationDefinition[] animationVariants
        ) {
            return new ActionLayerBuilder(this, name, animationState, animationVariants);
        }

        // DEATH LAYER

        /**
         * Adds a single death layer (one animation).
         */
        public Builder addDeathLayer(
                String name,
                AnimationState animationState,
                AnimationDefinition animationDefinition
        ) {
            return addDeathLayer(name, animationState, animationDefinition, null);
        }

        public Builder addDeathLayer(
                String name,
                AnimationState animationState,
                AnimationDefinition animationDefinition,
                @Nullable IAnimationCondition condition
        ) {
            DeathLayer layer = new DeathLayer(name, animationState, animationDefinition, condition);
            managerBuilder.addAnimationLayer(layer);
            return this;
        }

        /**
         * Adds a death layer with a variant selector.
         * Useful for: front vs. back death.
         */
        public Builder addDeathLayer(
                String name,
                AnimationState animationState,
                Function<Entity, AnimationDefinition> variantSelector
        ) {
            return addDeathLayer(name, animationState, variantSelector, null);
        }

        public Builder addDeathLayer(
                String name,
                AnimationState animationState,
                Function<Entity, AnimationDefinition> variantSelector,
                @Nullable IAnimationCondition condition
        ) {
            DeathLayer layer = new DeathLayer(name, animationState, variantSelector, condition);
            managerBuilder.addAnimationLayer(layer);
            return this;
        }

        // PROCEDURAL LAYER

        /**
         * Adds a procedural layer (head tracking, arm aiming, etc.).
         */
        public Builder addProceduralLayer(IProceduralLayer layer) {
            managerBuilder.addProceduralLayer(layer);
            return this;
        }

        // BUILD
        public LayeredAnimationManager build() {
            return managerBuilder.build();
        }
    }

    // SUB-BUILDER FOR ACTION LAYER

    /**
     * Fluid builder to configure ActionLayer with all its options.
     */
    public static class ActionLayerBuilder {

        private final Builder parentBuilder;
        private final String name;
        private final AnimationState animationState;
        private final AnimationDefinition[] animationVariants;

        private AnimationPriority priority = AnimationPriority.MEDIUM;
        private float durationSeconds = 1.0f;
        private float speedFactor = 1.0f; // nuevo
        private ActionLayer.TriggerDetector triggerDetector = null;
        private IAnimationCondition condition = null;

        private ActionLayerBuilder(Builder parent, String name, AnimationState state,
                                   AnimationDefinition[] variants) {
            this.parentBuilder = parent;
            this.name = name;
            this.animationState = state;
            this.animationVariants = variants;
        }

        /**
         * Set the speed factor (e.g., 2.0f for double speed).
         */
        public ActionLayerBuilder speed(float factor) {
            this.speedFactor = factor;
            return this;
        }

        /**
         * Set the animation priority.
         */
        public ActionLayerBuilder priority(AnimationPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the duration in seconds.
         * If there are variations, use the duration of each AnimationDefinition.
         */
        public ActionLayerBuilder duration(float seconds) {
            this.durationSeconds = seconds;
            return this;
        }

        /**
         * Sets the trigger detector (e.g., entity.hurtTime).
         */
        public ActionLayerBuilder triggerOn(ActionLayer.TriggerDetector detector) {
            this.triggerDetector = detector;
            return this;
        }

        /**
         * Add a condition to be able to play.
         */
        public ActionLayerBuilder condition(IAnimationCondition condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Finish the configuration and return to the main builder.
         */
        public Builder build() {
            if (triggerDetector == null) {
                throw new IllegalStateException("ActionLayer '" + name + "' needs a trigger detector");
            }

            ActionLayer layer = new ActionLayer(
                    name, animationState, animationVariants,
                    priority, durationSeconds, triggerDetector, condition
            );


            parentBuilder.managerBuilder.addAnimationLayer(layer);
            return parentBuilder;
        }
    }
}
