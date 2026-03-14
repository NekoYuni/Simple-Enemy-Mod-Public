package net.nekoyuni.SimpleEnemyMod.entity.ai.roles;

import net.minecraft.world.entity.PathfinderMob;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.*;

public class DefaultSquadLeaderGoals extends AbstractUnitGoals  {

    @Override
    public void addSpecificGoals(PathfinderMob entity) {
        entity.goalSelector.addGoal(1, new SquadLeaderHandshakeGoal(entity, 5, 15));
        entity.goalSelector.addGoal(7, new LongPatrolGoal(entity, 1.1D, 30));
    }

}
