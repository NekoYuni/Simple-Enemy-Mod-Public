package net.nekoyuni.SimpleEnemyMod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.client.system.SuppressionManager;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketPlayImpactSound;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketSuppression;
import net.nekoyuni.SimpleEnemyMod.registry.ModSounds;
import net.nekoyuni.SimpleEnemyMod.sound.BulletImpactSoundInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPacketHandler {

    private static final Map<String, Long> soundHistory = new ConcurrentHashMap<>();
    private static final long DUPLICATE_THRESHOLD_MS = 1000;
    private static long lastCleanupTime = 0;

    public static void handleImpactSound(PacketPlayImpactSound msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            System.out.println("\u001B[33m[SoundDebug] Client not ready to reproduce\u001B[0m");
            return;
        }

        long clientTime = System.currentTimeMillis();
        long latency = clientTime - msg.getTimestamp();
        String soundKey = generateSoundKey(msg.getX(), msg.getY(), msg.getZ(), msg.getVolume(), msg.getPitch());

        if (clientTime - lastCleanupTime > 2000) {
            cleanupSoundHistory(clientTime);
            lastCleanupTime = clientTime;
        }

        Long lastPlayTime = soundHistory.get(soundKey);
        if (lastPlayTime != null) {
            if ((clientTime - lastPlayTime) < 1000) return;
        }

        if (latency > 300) return;

        Vec3 impactPos = new Vec3(msg.getX(), msg.getY(), msg.getZ());
        if (mc.player.position().distanceTo(impactPos) > 60) return;

        soundHistory.put(soundKey, clientTime);
        try {
            BulletImpactSoundInstance soundInstance = new BulletImpactSoundInstance(
                    ModSounds.SOUND_BULLET_IMPACT.get(),
                    msg.getSource(),
                    msg.getVolume(),
                    msg.getPitch(),
                    impactPos
            );
            mc.getSoundManager().play(soundInstance);

        } catch (Exception e) {
            System.out.printf("\u001B[31m[SoundDebug] Error triying to rerproduce the sound: %s\u001B[0m%n", e.getMessage());
            soundHistory.remove(soundKey);
        }
    }

    public static void handleSuppression(PacketSuppression msg) {
        SuppressionManager.addSuppression(msg.getAmount());
    }

    private static String generateSoundKey(double x, double y, double z, float volume, float pitch) {
        return String.format("%d:%d:%d:%d:%d",
                (int)Math.round(x*10), (int)Math.round(y*10), (int)Math.round(z*10),
                (int)Math.round(volume*100), (int)Math.round(pitch*100));
    }

    private static void cleanupSoundHistory(long currentTime) {
        soundHistory.entrySet().removeIf(entry -> currentTime - entry.getValue() > DUPLICATE_THRESHOLD_MS);
    }
}
