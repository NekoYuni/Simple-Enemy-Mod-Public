package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.*;

public class FriendlySquadLeaderGoals extends AbstractFriendlyGoals{

    @Override
    protected void addSpecificGoals(PathfinderMob entity) {
        entity.goalSelector.addGoal(1, new SquadLeaderHandshakeGoal(entity, 5, 15));
        entity.goalSelector.addGoal(3, new CommanderOrderGoal(entity, 1.1D, 10.0F, 3.0F));
        entity.goalSelector.addGoal(7, new LongPatrolGoal(entity, 1.1D, 30));
    }


}
