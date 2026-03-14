package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.SquadUnitHandshakeFollowGoal;

public class FriendlySquadUnitGoals extends AbstractFriendlyGoals{

    @Override
    protected void addSpecificGoals(PathfinderMob entity) {
        entity.goalSelector.addGoal(2, new SquadUnitHandshakeFollowGoal(entity, 1.1, 15));
        entity.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(entity, 1.0D));
    }


}
