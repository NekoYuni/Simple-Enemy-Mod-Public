package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;

public interface IRoleGoals {

    /**
     * Defines and adds behavioral goals for an entity.
     * @param entity The entity to which the goals will be assigned.
     */
    void addGoals(PathfinderMob entity);

}
