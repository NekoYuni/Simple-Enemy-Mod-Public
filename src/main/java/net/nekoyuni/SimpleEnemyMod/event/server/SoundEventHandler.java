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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID)
public class SoundEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, Long> lastSoundTime = new ConcurrentHashMap<>();
    private static final long MIN_SOUND_INTERVAL_MS = 100;
    private static final long CLEANUP_INTERVAL = 3000;
    private static long lastCleanupTime = 0;

    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanup();
            lastCleanupTime = currentTime;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        Vec3 impactPos = event.getHitResult().getLocation();
        Entity shooter = event.getAmmo().getOwner();

        if (!(shooter instanceof USunitEntity || shooter instanceof RUunitEntity)) {
            return;
        }

        boolean hasNearbyPlayers = level.players().stream()
                .anyMatch(player -> player.distanceToSqr(impactPos) < 64 * 64);

        if (!hasNearbyPlayers) {
            return;
        }

        String locationKey = getLocationKey(impactPos, 2.0);
        Long lastTime = lastSoundTime.get(locationKey);

        if (lastTime != null && (currentTime - lastTime) < MIN_SOUND_INTERVAL_MS) {
            return;
        }

        lastSoundTime.put(locationKey, currentTime);

        for (ServerPlayer player : level.players()) {
            double distanceSqr = player.distanceToSqr(impactPos);

            if (distanceSqr < 64 * 64) {
                float distance = (float) Math.sqrt(distanceSqr);
                float volume = Math.max(0.1f, 2.0f - (distance / 64.0f));
                float pitch = 0.8f + level.random.nextFloat() * 0.4f;

                PacketPlayImpactSound packet = new PacketPlayImpactSound(
                        impactPos.x,
                        impactPos.y,
                        impactPos.z,
                        volume,
                        pitch,
                        SoundSource.NEUTRAL,
                        currentTime
                );

                try {
                    ModNetworking.sendToPlayer(packet, player);
                    /*
                     LOGGER.debug("Sound packet sent to player {} at distance {}",
                            player.getName().getString(), distance);
                     */
                } catch (Exception e) {
                    LOGGER.error("Failed to send sound packet to player {}: {}",
                            player.getName().getString(), e.getMessage());
                }
            }
        }
    }


    private static String getLocationKey(Vec3 pos, double precision) {
        int x = (int) (pos.x / precision);
        int y = (int) (pos.y / precision);
        int z = (int) (pos.z / precision);
        return x + ":" + y + ":" + z;
    }


    public static void cleanup() {
        long now = System.currentTimeMillis();
        lastSoundTime.entrySet().removeIf(entry ->
                now - entry.getValue() > 5000);

        /*
        LOGGER.debug("Sound cache cleanup completed. Remaining entries: {}",
                lastSoundTime.size());

         */
    }

}
