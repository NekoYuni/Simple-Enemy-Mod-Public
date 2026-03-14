package net.nekoyuni.SimpleEnemyMod.entity.client.animation.core;

/**
 * Interface implemented by entities that need to know
 which animation variant is playing.
 */
public interface IAnimatedEntity {

    /**
     * Called when an animation layer selects a variant.
     *
     * @param layerName Layer name (e.g., "hurt", "attack")
     * @param variantIndex Index of the selected variant
     */
    void onAnimationVariantSelected(String layerName, int variantIndex);
}