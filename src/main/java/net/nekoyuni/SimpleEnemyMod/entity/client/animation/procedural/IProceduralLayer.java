package net.nekoyuni.SimpleEnemyMod.entity.client.animation.procedural;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public interface IProceduralLayer {
    void apply(ModelPart root, Entity entity, float partialTick);
    boolean isEnabled();
}
