package net.nekoyuni.SimpleEnemyMod.event.server;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.network.ModNetworking;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketPlayImpactSound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID)
public class SoundEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Long, Long> lastSoundTime = new HashMap<>();
    private static final long MIN_SOUND_INTERVAL_MS = 100;
    private static final long CLEANUP_INTERVAL_MS = 3000;
    private static long lastCleanupTime = 0;

    private static final double RANGE_SQ = 64.0 * 64.0;

    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        Entity shooter = event.getAmmo().getOwner();
        if (!(shooter instanceof USunitEntity || shooter instanceof RUunitEntity)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        Vec3 impactPos = event.getHitResult().getLocation();

        List<ServerPlayer> nearbyPlayers = level.players().stream()
                .filter(p -> p.distanceToSqr(impactPos) < RANGE_SQ)
                .collect(Collectors.toList());

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        long locationKey = getLocationKeyLong(impactPos, 2.0);
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastSoundTime.get(locationKey);

        if (lastTime != null && (currentTime - lastTime) < MIN_SOUND_INTERVAL_MS) {
            return;
        }

        lastSoundTime.put(locationKey, currentTime);

        for (ServerPlayer player : nearbyPlayers) {
            double distanceSqr = player.distanceToSqr(impactPos);
            float distance = (float) Math.sqrt(distanceSqr);
            float normalized = distance / 64.0f;
            float volume = Math.max(0.1f, 2.5f * (1.0f - normalized * normalized));
            float pitch = 0.8f + level.random.nextFloat() * 0.4f;

            PacketPlayImpactSound packet = new PacketPlayImpactSound(
                    impactPos.x, impactPos.y, impactPos.z,
                    volume, pitch,
                    SoundSource.NEUTRAL,
                    currentTime
            );

            try {
                ModNetworking.sendToPlayer(packet, player);
            } catch (Exception e) {
                LOGGER.error("Failed to send sound packet to player {}: {}",
                        player.getName().getString(), e.getMessage());
            }
        }

        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanup(currentTime);
            lastCleanupTime = currentTime;
        }
    }

    private static long getLocationKeyLong(Vec3 pos, double precision) {
        int x = (int) (pos.x / precision) & 0xFFFFF;
        int y = (int) (pos.y / precision) & 0xFFF;
        int z = (int) (pos.z / precision) & 0xFFFFF;
        return ((long) x << 32) | ((long) y << 20) | z;
    }

    public static void cleanup(long now) {
        lastSoundTime.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
    }
}
