package net.nekoyuni.SimpleEnemyMod.event.server;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.awareness.CombatSoundAwarenessManager;
import net.nekoyuni.SimpleEnemyMod.entity.ai.suppression.CombatSuppressionManager;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.spawn.TacticalReactionManager;

import java.util.List;

@Mod.EventBusSubscriber
public class BulletImpactEventHandler {

    @SubscribeEvent
    public static void onBulletHitBlock(AmmoHitBlockEvent event) {

        EntityKineticBullet bullet = event.getAmmo();
        Entity owner = bullet.getOwner();

        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }

        Vec3 impactPos = event.getHitResult().getLocation();
        double x = impactPos.x;
        double y = impactPos.y;
        double z = impactPos.z;
        double radius = TacticalAiConfig.suppressionImpactRadius();

        CombatSuppressionManager.emitGunfireSuppression(event.getLevel(), shooter, impactPos, radius);
        if (!(shooter instanceof AbstractUnit)) {
            CombatSoundAwarenessManager.emitGunshot(event.getLevel(), shooter, shooter.position(), TacticalAiConfig.gunshotHearingRadius());
        }

        AABB impactBox = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        List<PathfinderMob> nearbyNPCs = event.getLevel().getEntitiesOfClass(PathfinderMob.class, impactBox);

        for (PathfinderMob npc : nearbyNPCs) {

            if (!npc.isAlive() || npc == shooter) continue;
            if (!(npc instanceof AbstractUnit)) continue;
            if (UnitFactionTags.isFriendlyFire(npc, shooter)) continue;


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
