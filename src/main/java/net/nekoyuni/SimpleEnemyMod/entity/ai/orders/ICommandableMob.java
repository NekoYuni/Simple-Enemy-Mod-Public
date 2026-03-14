package net.nekoyuni.SimpleEnemyMod.entity.ai.orders;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface ICommandableMob {

    OrderType getOrder();
    void setOrder(OrderType order);
    UUID getOwnerUUID();
    Vec3 getMoveToTarget();

}
