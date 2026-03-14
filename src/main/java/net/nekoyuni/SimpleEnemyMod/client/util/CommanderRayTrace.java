package net.nekoyuni.SimpleEnemyMod.client.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;

import java.util.Optional;

public class CommanderRayTrace {

    public static BlockHitResult rayTrace(Player player, double distance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 targetVec = eyePos.add(viewVec.scale(distance));

        return player.level().clip(new ClipContext(
                eyePos,
                targetVec,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    public static boolean isValidMoveTarget(BlockHitResult result) {
        return result.getType() == HitResult.Type.BLOCK;
    }

    public static Entity rayTraceEntity(Player player, double distance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(viewVec.scale(distance));

        AABB searchBox =
                player.getBoundingBox()
                .expandTowards(viewVec.scale(distance))
                .inflate(1.0D, 1.0D, 1.0D);

        Entity target = null;
        double closestDistance = distance;

        for (Entity entity : player.level().getEntities(player, searchBox)) {

            if (entity == player || entity instanceof PmcUnitEntity) continue;

            AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = bb.clip(eyePos, reachVec);

            if (hit.isPresent()) {
                double distToEntity = eyePos.distanceTo(hit.get());

                if (distToEntity < closestDistance) {
                    target = entity;
                    closestDistance = distToEntity;
                }
            }
        }
        return target;
    }

}
