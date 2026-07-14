package net.nekoyuni.SimpleEnemyMod.scenario;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;

/**
 * Lightweight SEM scenario tick profiler.
 *
 * <p>This is intentionally independent from Spark: Spark explains where CPU time
 * went, while this profiler writes scenario-local TICK_SAMPLE/TICK_WARN entries
 * so lag spikes can be correlated with scenario tick, combat state and tactical
 * counters.</p>
 */
@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SemTickProfiler {
    private static final int ROLLING_WINDOW_SIZE = 20;
    private static final double[] ROLLING_TICK_MS = new double[ROLLING_WINDOW_SIZE];

    private static long startNanos;
    private static int rollingIndex;
    private static int rollingCount;
    private static double rollingSum;
    private static double rollingMax;

    private static int coverSearchesThisTick;
    private static int coverCandidatesScoredThisTick;
    private static int coverPathChecksThisTick;
    private static int coverBudgetDeniedThisTick;
    private static int suppressionEventsThisTick;
    private static int gunfireEventsThisTick;
    private static int soundAwarenessEventsThisTick;

    private SemTickProfiler() {
        // Utility class.
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!TacticalAiConfig.enableSemTickProfiler()) {
            resetPerTickCounters();
            startNanos = 0L;
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            startNanos = System.nanoTime();
            resetPerTickCounters();
            return;
        }

        if (event.phase != TickEvent.Phase.END || startNanos <= 0L) {
            return;
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        startNanos = 0L;
        double tickMs = elapsedNanos / 1_000_000.0D;
        updateRollingWindow(tickMs);

        TickMetrics metrics = new TickMetrics(
                tickMs,
                rollingAverage(),
                rollingMax,
                coverSearchesThisTick,
                coverCandidatesScoredThisTick,
                coverPathChecksThisTick,
                coverBudgetDeniedThisTick,
                suppressionEventsThisTick,
                gunfireEventsThisTick,
                soundAwarenessEventsThisTick
        );

        SemScenarioRuntimeManager.get().logTickProfile(metrics);
    }

    public static void recordCoverSearchStarted() {
        coverSearchesThisTick++;
    }

    public static void recordCoverCandidateScored() {
        coverCandidatesScoredThisTick++;
    }

    public static void recordCoverPathCheck() {
        coverPathChecksThisTick++;
    }

    public static void recordCoverBudgetDenied() {
        coverBudgetDeniedThisTick++;
    }

    public static void recordSuppressionEvent() {
        suppressionEventsThisTick++;
    }

    public static void recordGunfireEvent() {
        gunfireEventsThisTick++;
    }

    public static void recordSoundAwarenessEvent() {
        soundAwarenessEventsThisTick++;
    }

    private static void updateRollingWindow(double tickMs) {
        if (rollingCount < ROLLING_WINDOW_SIZE) {
            rollingCount++;
        } else {
            rollingSum -= ROLLING_TICK_MS[rollingIndex];
        }

        ROLLING_TICK_MS[rollingIndex] = tickMs;
        rollingSum += tickMs;
        rollingIndex = (rollingIndex + 1) % ROLLING_WINDOW_SIZE;

        rollingMax = 0.0D;
        for (int i = 0; i < rollingCount; i++) {
            rollingMax = Math.max(rollingMax, ROLLING_TICK_MS[i]);
        }
    }

    private static double rollingAverage() {
        return rollingCount <= 0 ? 0.0D : rollingSum / rollingCount;
    }

    private static void resetPerTickCounters() {
        coverSearchesThisTick = 0;
        coverCandidatesScoredThisTick = 0;
        coverPathChecksThisTick = 0;
        coverBudgetDeniedThisTick = 0;
        suppressionEventsThisTick = 0;
        gunfireEventsThisTick = 0;
        soundAwarenessEventsThisTick = 0;
    }

    public static final class TickMetrics {
        public final double tickMs;
        public final double avg20TickMs;
        public final double max20TickMs;
        public final int coverSearchesThisTick;
        public final int coverCandidatesScoredThisTick;
        public final int coverPathChecksThisTick;
        public final int coverBudgetDeniedThisTick;
        public final int suppressionEventsThisTick;
        public final int gunfireEventsThisTick;
        public final int soundAwarenessEventsThisTick;

        private TickMetrics(double tickMs,
                            double avg20TickMs,
                            double max20TickMs,
                            int coverSearchesThisTick,
                            int coverCandidatesScoredThisTick,
                            int coverPathChecksThisTick,
                            int coverBudgetDeniedThisTick,
                            int suppressionEventsThisTick,
                            int gunfireEventsThisTick,
                            int soundAwarenessEventsThisTick) {
            this.tickMs = tickMs;
            this.avg20TickMs = avg20TickMs;
            this.max20TickMs = max20TickMs;
            this.coverSearchesThisTick = coverSearchesThisTick;
            this.coverCandidatesScoredThisTick = coverCandidatesScoredThisTick;
            this.coverPathChecksThisTick = coverPathChecksThisTick;
            this.coverBudgetDeniedThisTick = coverBudgetDeniedThisTick;
            this.suppressionEventsThisTick = suppressionEventsThisTick;
            this.gunfireEventsThisTick = gunfireEventsThisTick;
            this.soundAwarenessEventsThisTick = soundAwarenessEventsThisTick;
        }
    }
}
