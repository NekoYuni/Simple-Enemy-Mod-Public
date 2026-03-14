package net.nekoyuni.SimpleEnemyMod.entity.client.animation.core;

import net.minecraft.world.entity.Entity;

public interface IAnimationManager {
    void update(Entity entity, int tickCount);
    void reset();
    boolean isPlaying(String layerName);
}
