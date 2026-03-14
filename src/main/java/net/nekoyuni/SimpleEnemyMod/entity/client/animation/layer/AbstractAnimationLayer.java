package net.nekoyuni.SimpleEnemyMod.entity.client.animation.layer;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.condition.IAnimationCondition;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.AnimationPriority;

import javax.annotation.Nullable;

public abstract class AbstractAnimationLayer implements IAnimationLayer {

    protected final String name;
    protected final AnimationState animationState;
    protected final AnimationDefinition animationDefinition;
    protected final AnimationPriority priority;
    protected final IAnimationCondition condition;

    private boolean isCurrentPlaying = false;
    private int startTick = -1;


    public AbstractAnimationLayer(
            String name, AnimationState animationState,
            AnimationDefinition animationDefinition,
            AnimationPriority priority,
            @Nullable IAnimationCondition condition

    ) {
        this.name = name;
        this.animationState = animationState;
        this.animationDefinition = animationDefinition;
        this.priority = priority;
        this.condition = condition != null ? condition : (entity, tick) -> true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AnimationPriority getPriority() {
        return priority;
    }

    @Override
    public boolean canPlay(Entity entity, int tickCount) {
        return condition.test(entity, tickCount);
    }

    @Override
    public void play(Entity entity, int tickCount) {
        if (!isCurrentPlaying){
            animationState.stop();
            animationState.start(tickCount);
            isCurrentPlaying = true;
            startTick = tickCount;
            onStart(entity, tickCount);

        } else {
            onUpdate(entity, tickCount);
        }

    }

    @Override
    public boolean isPlaying() {
        return isCurrentPlaying && animationState.isStarted();
    }

    @Override
    public void stop() {
        this.isCurrentPlaying = false;
        this.startTick = -1;

        if (this.animationState.isStarted()) {
            this.animationState.stop();
        }

        onStop();
    }

    protected int getTicksSinceStart(int currentTick) {
        return startTick >= 0 ? currentTick - startTick : 0;
    }

    protected AnimationState getAnimationState() {
        return animationState;
    }

    protected AnimationDefinition getAnimationDefinition() {
        return animationDefinition;
    }

    protected void onStart(Entity entity, int tickCount) {
    }

    protected void onUpdate(Entity entity, int tickCount) {
    }

    protected void onStop() {
    }


}
