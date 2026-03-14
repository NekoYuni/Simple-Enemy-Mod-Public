package net.nekoyuni.SimpleEnemyMod.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.nekoyuni.SimpleEnemyMod.network.ClientPacketHandler;

import java.util.function.Supplier;

public class PacketPlayImpactSound {
    private final double x, y, z;
    private final SoundSource source;
    private final float volume;
    private final float pitch;
    private final long timestamp;

    public PacketPlayImpactSound(double x, double y, double z, float volume, float pitch, SoundSource source, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.source = source;
        this.timestamp = timestamp;
    }

    public static void encode(PacketPlayImpactSound msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
        buf.writeEnum(msg.source);
        buf.writeLong(msg.timestamp);
    }

    public static PacketPlayImpactSound decode(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        SoundSource source = buf.readEnum(SoundSource.class);
        long timestamp = buf.readLong();

        return new PacketPlayImpactSound(x, y, z, volume, pitch, source, timestamp);
    }

    public static void handle(PacketPlayImpactSound msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> ClientPacketHandler.handleImpactSound(msg)
            );
        });
        context.setPacketHandled(true);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public SoundSource getSource() {
        return source;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public long getTimestamp() {
        return timestamp;
    }
}


