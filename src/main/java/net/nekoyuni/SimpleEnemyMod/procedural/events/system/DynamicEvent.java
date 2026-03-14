package net.nekoyuni.SimpleEnemyMod.procedural.events.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class DynamicEvent {

    private final String eventId;

    public DynamicEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getId() {
        return eventId;
    }

    public abstract double getBaseChance();

    public abstract double getFailureMultiplier();

    public abstract int getMinDistance();

    public abstract int getMaxDistance();

    public double getBiomeModifier(ServerLevel level, BlockPos pos) {
        return 1.0;
    }

    public boolean canExecute(ServerLevel level, ServerPlayer player) {
        return true;
    }

    public abstract boolean execute(ServerLevel level, ServerPlayer player, BlockPos pos);
}
