package net.nekoyuni.SimpleEnemyMod.scenario;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;

/**
 * Bridges Forge runtime events into the scenario manager.
 *
 * <p>Unlike the public build, the current private test harness does not have scenario-owned
 * reinforcement spawners. EntityJoinLevelEvent tracking is intentionally disabled here so
 * pre-existing/manual SEM units cannot pollute scenario logs as EXTERNAL combatants.</p>
 */
@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SemScenarioEventHandler {
    private SemScenarioEventHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }
        SemScenarioRuntimeManager.get().tick(serverLevel);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        SemScenarioRuntimeManager.get().onEntityDeath(event.getEntity());
    }
}
