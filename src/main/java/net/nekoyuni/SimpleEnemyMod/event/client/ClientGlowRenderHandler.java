package net.nekoyuni.SimpleEnemyMod.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.client.system.ClientGlowManager;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = "simpleenemymod", value = Dist.CLIENT)
public class ClientGlowRenderHandler {

    private static final int PARTICLE_COUNT = 12;
    private static final double RADIUS = 0.6;
    private static final int TICK_INTERVAL = 5;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        for (int id : ClientGlowManager.getAll()) {
            Entity entity = mc.level.getEntity(id);
            if (entity == null) continue;

            spawnCircle(mc.level, entity);
        }
    }

    private static void spawnCircle(Level level, Entity entity) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {

            double angle = (2 * Math.PI / PARTICLE_COUNT) * i;
            double offsetX = Math.cos(angle) * RADIUS;
            double offsetZ = Math.sin(angle) * RADIUS;

            level.addParticle(
                    new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.3f), 1.0f), // R, G, B | tamaño
                    entity.getX() + offsetX,
                    entity.getY() + 0.05,
                    entity.getZ() + offsetZ,
                    0, 0, 0
            );
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (ClientGlowManager.getAll().contains(event.getEntity().getId())) {
            System.out.println("Glow event triggered for: " + event.getEntity().getId());
            event.getEntity().setGlowingTag(true);
        }
    }

}
