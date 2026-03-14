package net.nekoyuni.SimpleEnemyMod.entity.client.animation.core;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer.IAnimationLayer;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural.IProceduralLayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LayeredAnimationManager implements IAnimationManager {

    private final List<IAnimationLayer> animationLayers;
    private final List<IProceduralLayer> proceduralLayers;

    private IAnimationLayer currentActiveLayer = null;
    private boolean debugMode = false;

    private LayeredAnimationManager(List<IAnimationLayer> animationLayers,
                                    List<IProceduralLayer> proceduralLayers) {
        this.animationLayers = new ArrayList<>(animationLayers);
        this.proceduralLayers = new ArrayList<>(proceduralLayers);

        sortLayersByPriority();
    }

    private void sortLayersByPriority() {
        animationLayers.sort(Comparator.comparing(IAnimationLayer::getPriority));
    }

    @Override
    public void update(Entity entity, int tickCount) {
        if (entity == null) {
            return;
        }

        IAnimationLayer layerToPlay = findHighestPriorityLayer(entity, tickCount);

        if (layerToPlay != currentActiveLayer) {

            if (currentActiveLayer != null) {
                if (debugMode) {
                    System.out.println("[Manager | Tick " + tickCount + "] Stopping inactive Layer: " + currentActiveLayer.getName());
                }

                currentActiveLayer.stop();
            }

            currentActiveLayer = layerToPlay;

            if (debugMode && layerToPlay != null) {
                System.out.println("[LayeredAnimationManager] Layer Active: "
                        + layerToPlay.getName() + " (Priority: " + layerToPlay.getPriority() + ")");
            }
        }

        if (layerToPlay != null) {
            layerToPlay.play(entity, tickCount);
        }
    }

    /**
     * Apply the procedural transformations (must be called from the model's setupAnim)
     */
    public void applyProceduralLayers(ModelPart root, Entity entity, float partialTick) {
        for (IProceduralLayer layer : proceduralLayers) {
            if (layer.isEnabled()) {
                layer.apply(root, entity, partialTick);
            }
        }
    }

    private IAnimationLayer findHighestPriorityLayer(Entity entity, int tickCount) {

        for (IAnimationLayer layer : animationLayers) {
            boolean canPlay = layer.canPlay(entity, tickCount);

            if (canPlay) {
                return layer;
            }
        }

        return null;
    }



    @Override
    public void reset() {
        for (IAnimationLayer layer : animationLayers) {
            layer.stop();
        }

        currentActiveLayer = null;

        if (debugMode) {
            System.out.println("[LayeredAnimationManager] RESET completo");
        }
    }

    @Override
    public boolean isPlaying(String layerName) {
        for (IAnimationLayer layer : animationLayers) {
            if (layer.getName().equals(layerName)) {
                return layer.isPlaying();
            }
        }
        return false;
    }

    public IAnimationLayer getCurrentActiveLayer() {
        return currentActiveLayer;
    }

    public List<IAnimationLayer> getAllAnimationLayers() {
        return new ArrayList<>(animationLayers);
    }

    public List<IProceduralLayer> getAllProceduralLayers() {
        return new ArrayList<>(proceduralLayers);
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    // BUILDER

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<IAnimationLayer> animationLayers = new ArrayList<>();
        private final List<IProceduralLayer> proceduralLayers = new ArrayList<>();

        public Builder addAnimationLayer(IAnimationLayer layer) {
            if (layer == null) {
                throw new IllegalArgumentException("Animation layer cannot be null");
            }
            animationLayers.add(layer);
            return this;
        }

        public Builder addProceduralLayer(IProceduralLayer layer) {
            if (layer == null) {
                throw new IllegalArgumentException("Procedural layer cannot be null");
            }
            proceduralLayers.add(layer);
            return this;
        }

        public Builder addAnimationLayers(List<IAnimationLayer> layers) {
            animationLayers.addAll(layers);
            return this;
        }

        public Builder addProceduralLayers(List<IProceduralLayer> layers) {
            proceduralLayers.addAll(layers);
            return this;
        }

        public LayeredAnimationManager build() {
            if (animationLayers.isEmpty()) {
                throw new IllegalStateException("Cannot build LayeredAnimationManager without animation layers");
            }

            return new LayeredAnimationManager(animationLayers, proceduralLayers);
        }
    }
}
