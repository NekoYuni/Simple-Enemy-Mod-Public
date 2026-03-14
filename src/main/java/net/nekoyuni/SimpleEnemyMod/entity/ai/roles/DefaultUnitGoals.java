package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;

public class DefaultUnitGoals extends AbstractUnitGoals  {

    @Override
    public void addSpecificGoals(PathfinderMob entity) {
        entity.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(entity, 1.0D));

    }
}
