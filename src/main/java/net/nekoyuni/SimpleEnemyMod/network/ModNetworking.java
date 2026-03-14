package net.nekoyuni.SimpleEnemyMod.network;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketIssueOrder;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketPlayImpactSound;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketSuppression;
import org.slf4j.Logger;

public class ModNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PROTOCOL_VERSION = "1.0.1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("simpleenemymod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        LOGGER.info("Registrando paquetes de red para SimpleEnemyMod...");

        CHANNEL.registerMessage(id++,
                PacketPlayImpactSound.class,
                PacketPlayImpactSound::encode,
                PacketPlayImpactSound::decode,
                PacketPlayImpactSound::handle
        );

        CHANNEL.registerMessage(id++,
                PacketIssueOrder.class,
                PacketIssueOrder::encode,
                PacketIssueOrder::decode,
                PacketIssueOrder::handle
        );

        CHANNEL.registerMessage(id++,
                PacketSuppression.class,
                PacketSuppression::encode,
                PacketSuppression::decode,
                PacketSuppression::handle
        );

        LOGGER.info("[SEM] Network packets successfully logged. Protocol version: {}", PROTOCOL_VERSION);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } catch (Exception e) {
            LOGGER.error("Error sending package to player {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }
}


