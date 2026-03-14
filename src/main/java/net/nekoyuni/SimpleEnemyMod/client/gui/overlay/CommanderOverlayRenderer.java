package net.nekoyuni.SimpleEnemyMod.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.util.CommanderRayTrace;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CommanderOverlayRenderer {

    public static boolean isSelectingPosition = false;
    public static Set<Integer> selectedUnitsSnapshot = new HashSet<>();
    public static boolean isSelectingTarget = false;

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (!isSelectingPosition && !isSelectingTarget) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;


        if (isSelectingPosition) {
            BlockHitResult result = CommanderRayTrace.rayTrace(player, 45.0); // Add to Config

            if (CommanderRayTrace.isValidMoveTarget(result)) {
                Vec3 hitPos = result.getLocation();

                // refactor para despues, si el de abajo funciona
                if (player.tickCount % 2 == 0) {
                    player.level().addParticle(
                            ParticleTypes.HAPPY_VILLAGER,
                            hitPos.x, hitPos.y + 0.1, hitPos.z,
                            0, 0.1, 0
                    );
                }
            }
        }

        if (isSelectingTarget) {
            Entity target = CommanderRayTrace.rayTraceEntity(player, 64.0); // Add to Config

            if (target == null) {
                return;
            }

            if (player.tickCount % 2 == 0) {
                return;
            }

            double targetX = target.getX();
            double targetY = target.getY() + (target.getBbHeight() / 2);
            double targetZ = target.getZ();

            player.level().addParticle(
                    ParticleTypes.FLAME,
                    targetX, targetY, targetZ,
                    0, 0.1, 0
            );

        }
    }

}
