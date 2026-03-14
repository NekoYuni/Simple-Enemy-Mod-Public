package net.nekoyuni.SimpleEnemyMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.nekoyuni.SimpleEnemyMod.network.ClientPacketHandler;

import java.util.function.Supplier;

public class PacketSuppression {
    private final float amount;

    public PacketSuppression(float amount) {
        this.amount = amount;
    }

    public static void encode(PacketSuppression msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.amount);
    }

    public static PacketSuppression decode(FriendlyByteBuf buf) {
        return new PacketSuppression(buf.readFloat());
    }

    public static void handle(PacketSuppression msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientPacketHandler.handleSuppression(msg)
            );
        });
        context.setPacketHandled(true);
    }

    public float getAmount() {
        return amount;
    }
}
