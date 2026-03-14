package net.nekoyuni.SimpleEnemyMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.ICommandableMob;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.OrderType;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;

import java.util.function.Supplier;

public class PacketIssueOrder {

    private final int entityId;
    private final OrderType order;
    private final Vec3 targetPos;
    private final int formationIndex;
    private final int targetEntityId;


    public PacketIssueOrder(int entityId, OrderType order, Vec3 targetPos, int formationIndex, int targetEntityId) {
        this.entityId = entityId;
        this.order = order;
        this.targetPos = targetPos;
        this.formationIndex = formationIndex;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(PacketIssueOrder msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeEnum(msg.order);
        buf.writeDouble(msg.targetPos.x);
        buf.writeDouble(msg.targetPos.y);
        buf.writeDouble(msg.targetPos.z);
        buf.writeInt(msg.formationIndex);
        buf.writeInt(msg.targetEntityId);
    }

    public static PacketIssueOrder decode(FriendlyByteBuf buf) {
        return new PacketIssueOrder(
                buf.readInt(),
                buf.readEnum(OrderType.class),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(PacketIssueOrder msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            Entity target = sender.level().getEntity(msg.entityId);
            if (!(target instanceof LivingEntity)) return;

            if (!(target instanceof ICommandableMob commandable)) return;

            if (!sender.getUUID().equals(commandable.getOwnerUUID())) {
                return;
            }

            if (target instanceof PmcUnitEntity pmcUnit) {

                if (msg.order == OrderType.ATTACK_THAT_TARGET) {
                    pmcUnit.setAttackTargetId(msg.targetEntityId);

                } else if (msg.order == OrderType.MOVE_TO_POSITION) {
                    pmcUnit.setMoveToTarget(msg.targetPos);
                }

                if (msg.order == OrderType.FREE_FIRE) {
                    pmcUnit.setAttackTargetId(-1);
                }

                if (msg.order == OrderType.CEASE_FIRE) {
                    pmcUnit.setTarget(null);
                    pmcUnit.setAttackTargetId(-1);
                    pmcUnit.setLastHurtByMob(null);
                }

                pmcUnit.releaseMovementLock();
                pmcUnit.setFormationIndex(msg.formationIndex);
                pmcUnit.resetCommanderGoalCooldown();
            }

            commandable.setOrder(msg.order);

        });
        ctx.get().setPacketHandled(true);
    }
}
