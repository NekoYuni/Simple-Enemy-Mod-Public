package net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer;

import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;

public interface IAnimationLayer {
    String getName();
    AnimationPriority getPriority();
    boolean canPlay(Entity entity, int tickCount);
    void play(Entity entity, int tickCount);
    void stop();
    boolean isPlaying();
}
