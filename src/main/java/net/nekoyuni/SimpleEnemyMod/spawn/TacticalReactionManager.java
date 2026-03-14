package net.nekoyuni.SimpleEnemyMod.spawn;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public class TacticalReactionManager {

    private static final Queue<Runnable> pendingReactions = new ConcurrentLinkedQueue<>();
    private static final int MAX_REACTIONS_PER_TICK = 5;

    public static void queueReaction(Runnable reaction) {
        pendingReactions.add(reaction);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processQueue();
        }
    }

    private static void processQueue() {
        int processedCount = 0;

        while (!pendingReactions.isEmpty() && processedCount < MAX_REACTIONS_PER_TICK) {
            Runnable task = pendingReactions.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Exception e) {
                }
            }
            processedCount++;
        }
    }
}
