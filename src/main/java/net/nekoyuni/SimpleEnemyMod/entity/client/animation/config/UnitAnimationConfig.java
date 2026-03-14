package net.nekoyuni.SimpleEnemyMod.entity.client.animation.config;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.ModAnimationsDefinitions;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.LayeredAnimationManager;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer.IAnimationLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural.AdvancedWeaponPoseLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural.HeadTrackingLayer;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import org.slf4j.Logger;

public class UnitAnimationConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean isDebug = false;

    /**
     * Creates the AnimationManager for a Unit entity.
     *
     * @param entity The AbstractUnit entity
     * @param head ModelPart of the head (used for head tracking)
     * @param rightArm ModelPart of the right arm (used for arm aiming)
     * @param leftArm ModelPart of the left arm (used for arm aiming)
     * @return Configured LayeredAnimationManager instance
     */
    public static LayeredAnimationManager create(
            AbstractUnit entity,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm
    ) {

        LayeredAnimationManager manager = AnimationConfig.builder()

                // DEATH
                .addDeathLayer(
                        "death",
                        entity.deathAnimationState,
                        e -> {
                            if (e instanceof AbstractUnit unit) {
                                boolean backDeath = unit.getEntityData().get(AbstractUnit.BACK_DEATH);
                                return backDeath ? ModAnimationsDefinitions.UNIT_DEATH_BACK : ModAnimationsDefinitions.UNIT_DEATH;
                            }
                            return ModAnimationsDefinitions.UNIT_DEATH;
                        }
                )

                // HURT
                .addActionLayer(
                        "hurt",
                        entity.hurtAnimationState,
                        ModAnimationsDefinitions.UNIT_HURT_VARIANTS
                )
                .priority(AnimationPriority.HIGH)
                .speed(2.0f)
                .duration(1.0f)
                .triggerOn(e -> {
                    if (e instanceof AbstractUnit unit) {
                        int consistentTrigger = unit.getEntityData().get(AbstractUnit.DAMAGE_ANIMATION_TICKS);


                        if (consistentTrigger > 0) {
                            // TODO
                        }

                        return consistentTrigger;
                    }
                    return 0;
                })
                .build()

                // LOCOMOTION: Idle + Walk
                .addLocomotionLayer(
                        "locomotion",
                        entity.idleAnimationState,
                        ModAnimationsDefinitions.UNIT_IDLE,
                        entity.walkAnimationState,
                        ModAnimationsDefinitions.UNIT_WALK,
                        AnimationPriority.MEDIUM
                )

                // PROCEDURAL: Head Tracking
                .addProceduralLayer(
                        new HeadTrackingLayer(
                                head,
                                30.0f,
                                -25.0f,
                                45.0f
                        )
                )

                // PROCEDURAL: Arm Aiming
                .addProceduralLayer(
                        new AdvancedWeaponPoseLayer(
                                head,
                                rightArm,
                                leftArm
                        )
                )

                .build();


        debug("==================================================");
        debug("[UnitAnimationConfig] AnimationManager created with {} animation layers:",
                manager.getAllAnimationLayers().size());

        for (IAnimationLayer layer : manager.getAllAnimationLayers()) {
            debug("    -> {} (priority: {})",
                    layer.getName(),
                    layer.getPriority());
        }

        debug("[UnitAnimationConfig] Procedural layers: {}",
                manager.getAllProceduralLayers().size());

        debug("==================================================");

        return manager;
    }

    private static void debug(String message, Object... args) {
        if (isDebug && LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

}
