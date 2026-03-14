package net.nekoyuni.SimpleEnemyMod.event.server;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import net.nekoyuni.SimpleEnemyMod.network.ModNetworking;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketSuppression;

import java.util.List;

@Mod.EventBusSubscriber
public class PlayerSuppressionEventHandler {

    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {

        if (!CommonConfig.ENABLE_SUPPRESSION.get()) {
            return;
        }

        if (event.getLevel().isClientSide) return;

        Entity shooter = event.getAmmo().getOwner();
        if (shooter == null) return;

        boolean isEnemy = (shooter instanceof RUunitEntity) ||
                (shooter instanceof USunitEntity);

        if (!isEnemy) {
            return;
        }

        Vec3 hitPos = event.getHitResult().getLocation();
        double radius = 7.0;

        List<ServerPlayer> nearbyPlayers = event.getLevel().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(hitPos, hitPos).inflate(radius)
        );

        for (ServerPlayer player : nearbyPlayers) {
            double distanceSq = player.distanceToSqr(hitPos);
            double radiusSq = radius * radius;

            if (distanceSq < radiusSq) {
                float intensity = (float) (1.0 - (distanceSq / radiusSq));
                float finalAmount = intensity * 0.3f;

                ModNetworking.sendToPlayer(new PacketSuppression(finalAmount), player);
            }
        }
    }

}
