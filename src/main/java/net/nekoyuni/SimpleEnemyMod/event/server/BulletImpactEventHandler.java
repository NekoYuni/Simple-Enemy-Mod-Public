package net.nekoyuni.SimpleEnemyMod.event.server;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.spawn.TacticalReactionManager;

import java.util.List;

@Mod.EventBusSubscriber
public class BulletImpactEventHandler {

    private static final double SUPPRESSION_RADIUS = 7.0D;

    @SubscribeEvent
    public static void onBulletHitBlock(AmmoHitBlockEvent event) {

        EntityKineticBullet bullet = event.getAmmo();
        Entity owner = bullet.getOwner();

        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }

        double x = event.getHitResult().getLocation().x;
        double y = event.getHitResult().getLocation().y;
        double z = event.getHitResult().getLocation().z;

        AABB impactBox = new AABB(
                x - SUPPRESSION_RADIUS, y - SUPPRESSION_RADIUS, z - SUPPRESSION_RADIUS,
                x + SUPPRESSION_RADIUS, y + SUPPRESSION_RADIUS, z + SUPPRESSION_RADIUS
        );

        List<PathfinderMob> nearbyNPCs = event.getLevel().getEntitiesOfClass(PathfinderMob.class, impactBox);

        for (PathfinderMob npc : nearbyNPCs) {

            if (!npc.isAlive() || npc == shooter) continue;

            if (npc.getClass() == shooter.getClass()) {
                continue;
            }

            if (npc instanceof PmcUnitEntity && shooter instanceof Player) {
                continue;
            }

            if (npc.getTarget() == null) {

                TacticalReactionManager.queueReaction(() -> {

                    if (npc.isAlive()) {
                        npc.setTarget(shooter);

                        // DEBUG
                        npc.setGlowingTag(false);

                    }
                });


            }

        }
    }
}
