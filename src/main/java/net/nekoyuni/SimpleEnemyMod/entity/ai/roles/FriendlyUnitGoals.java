package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.*;

public class FriendlyUnitGoals extends AbstractFriendlyGoals {


    @Override
    protected void addSpecificGoals(PathfinderMob entity) {
        entity.goalSelector.addGoal(3, new CommanderOrderGoal(entity, 1.1D, 10.0F, 3.0F));
        entity.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(entity, 1.0D));
    }


}
