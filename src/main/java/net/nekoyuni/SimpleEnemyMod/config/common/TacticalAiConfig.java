package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Neutral tactical AI switches migrated from the public AI branch.
 *
 * Keep this file focused on generic NPC behavior; threat scoring, loot,
 * bandits and scenario systems belong to later migration phases.
 */
public class TacticalAiConfig {

    public static ForgeConfigSpec.BooleanValue ENABLE_SOFT_OBSTACLE_BREAKING;
    public static ForgeConfigSpec.BooleanValue ENABLE_LADDER_FALLBACK;
    public static ForgeConfigSpec.BooleanValue ENABLE_PRECISE_FOLIAGE_LOS;
    public static ForgeConfigSpec.BooleanValue ENABLE_HAZARDOUS_COVER_REJECTION;
    public static ForgeConfigSpec.IntValue AI_LOS_CACHE_TICKS;

    public static ForgeConfigSpec.BooleanValue ENABLE_SMART_COVER_SEARCH;
    public static ForgeConfigSpec.BooleanValue ENABLE_COVER_PATH_VALIDATION;
    public static ForgeConfigSpec.BooleanValue ENABLE_FALLBACK_EVASION;
    public static ForgeConfigSpec.IntValue COVER_SEARCH_COOLDOWN_TICKS;
    public static ForgeConfigSpec.IntValue COVER_RELAXED_EXTRA_RADIUS;
    public static ForgeConfigSpec.IntValue FALLBACK_EVASION_RADIUS;
    public static ForgeConfigSpec.IntValue COVER_SEARCH_MAX_CANDIDATES;
    public static ForgeConfigSpec.IntValue COVER_SEARCH_MAX_PATH_CHECKS;
    public static ForgeConfigSpec.IntValue COVER_SEARCHES_PER_TICK;
    public static ForgeConfigSpec.IntValue COVER_SEARCH_BUDGET_JITTER_TICKS;

    public static ForgeConfigSpec.BooleanValue ENABLE_TACTICAL_SUPPRESSION;
    public static ForgeConfigSpec.DoubleValue SUPPRESSION_IMPACT_RADIUS;
    public static ForgeConfigSpec.DoubleValue SUPPRESSION_IMPACT_AMOUNT;
    public static ForgeConfigSpec.DoubleValue SUPPRESSION_DECAY_PER_TICK;
    public static ForgeConfigSpec.DoubleValue SUPPRESSION_STRONG_THRESHOLD;
    public static ForgeConfigSpec.DoubleValue SUPPRESSION_ACCURACY_SPREAD_PENALTY;
    public static ForgeConfigSpec.IntValue SUPPRESSION_COVER_LOCK_TICKS;
    public static ForgeConfigSpec.IntValue SUPPRESSION_SOURCE_DEBOUNCE_TICKS;

    public static ForgeConfigSpec.BooleanValue ENABLE_TACTICAL_HEARING;
    public static ForgeConfigSpec.DoubleValue GUNSHOT_HEARING_RADIUS;
    public static ForgeConfigSpec.DoubleValue SOUND_MIN_STRENGTH;
    public static ForgeConfigSpec.IntValue GUNSHOT_MEMORY_TICKS;
    public static ForgeConfigSpec.DoubleValue SOUND_INVESTIGATE_SPEED;
    public static ForgeConfigSpec.DoubleValue SOUND_INVESTIGATE_REACHED_DISTANCE;

    public static ForgeConfigSpec.BooleanValue ENABLE_TACTICAL_DECISION_LAYER;
    public static ForgeConfigSpec.IntValue TACTICAL_DECISION_INTERVAL_TICKS;
    public static ForgeConfigSpec.IntValue TACTICAL_DECISION_COVER_LOCK_TICKS;
    public static ForgeConfigSpec.DoubleValue TACTICAL_RETREAT_PRESSURE_THRESHOLD;
    public static ForgeConfigSpec.DoubleValue TACTICAL_COVER_PRESSURE_THRESHOLD;
    public static ForgeConfigSpec.DoubleValue TACTICAL_PUSH_CONFIDENCE_THRESHOLD;
    public static ForgeConfigSpec.BooleanValue ENABLE_SQUAD_CONTEXT;
    public static ForgeConfigSpec.BooleanValue ENABLE_SQUAD_COMBAT_MEMORY;
    public static ForgeConfigSpec.DoubleValue SQUAD_CONTEXT_RADIUS;
    public static ForgeConfigSpec.DoubleValue SQUAD_MEMORY_SHARE_RADIUS;
    public static ForgeConfigSpec.IntValue SQUAD_MEMORY_TICKS;
    public static ForgeConfigSpec.IntValue SQUAD_MEMORY_GRID_SIZE;
    public static ForgeConfigSpec.BooleanValue ENABLE_DISTANCE_TARGET_PRIORITY;
    public static ForgeConfigSpec.DoubleValue CLOSE_THREAT_SWITCH_DISTANCE;
    public static ForgeConfigSpec.DoubleValue CLOSE_THREAT_SWITCH_RATIO;
    public static ForgeConfigSpec.IntValue CLOSE_THREAT_SWITCH_SCAN_INTERVAL_TICKS;
    public static ForgeConfigSpec.BooleanValue ENABLE_SEM_LOOP_SPARK_PROFILING;
    public static ForgeConfigSpec.IntValue SEM_LOOP_SPARK_WARMUP_TICKS;

    public static ForgeConfigSpec.BooleanValue ENABLE_SEM_TICK_PROFILER;
    public static ForgeConfigSpec.BooleanValue ENABLE_SEM_TICK_SAMPLES;
    public static ForgeConfigSpec.IntValue SEM_TICK_SAMPLE_INTERVAL_TICKS;
    public static ForgeConfigSpec.DoubleValue SEM_TICK_WARN_THRESHOLD_MS;
    public static ForgeConfigSpec.DoubleValue SEM_TICK_SEVERE_THRESHOLD_MS;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("tactical_ai_phase_1");

        ENABLE_SOFT_OBSTACLE_BREAKING = builder
                .comment("Allow NPCs to break navigation clutter such as grass, leaves and glass while following a path.")
                .define("enableSoftObstacleBreaking", true);

        ENABLE_LADDER_FALLBACK = builder
                .comment("Allow NPCs to use a ladder-aware fallback route when vanilla navigation cannot resolve vertical movement.")
                .define("enableLadderFallback", true);

        ENABLE_PRECISE_FOLIAGE_LOS = builder
                .comment("Use tactical line-of-sight checks so foliage, bushes, glass and other soft visual clutter do not block target acquisition or firing.")
                .define("enablePreciseFoliageLos", true);

        ENABLE_HAZARDOUS_COVER_REJECTION = builder
                .comment("Reject tactical movement and cover positions that require standing in dangerous blocks such as lava, fire, cactus or magma.")
                .define("enableHazardousCoverRejection", true);

        AI_LOS_CACHE_TICKS = builder
                .comment("Minimum ticks between repeated line-of-sight checks against the same target.")
                .defineInRange("aiLosCacheTicks", 4, 1, 40);

        builder.pop();

        builder.push("tactical_ai_phase_2");

        ENABLE_SMART_COVER_SEARCH = builder
                .comment("Use scored tactical cover selection instead of returning the first adjacent blocking block.")
                .define("enableSmartCoverSearch", true);

        ENABLE_COVER_PATH_VALIDATION = builder
                .comment("Only select a cover or evasion position when the NPC can create a path to it.")
                .define("enableCoverPathValidation", true);

        ENABLE_FALLBACK_EVASION = builder
                .comment("When no real cover is reachable, allow NPCs to perform short fallback evasion instead of standing still.")
                .define("enableFallbackEvasion", true);

        COVER_SEARCH_COOLDOWN_TICKS = builder
                .comment("Cooldown applied after expensive cover searches to protect server performance.")
                .defineInRange("coverSearchCooldownTicks", 30, 5, 200);

        COVER_RELAXED_EXTRA_RADIUS = builder
                .comment("Additional radius used for the relaxed fallback cover pass after strict cover search fails.")
                .defineInRange("coverRelaxedExtraRadius", 4, 0, 12);

        FALLBACK_EVASION_RADIUS = builder
                .comment("Maximum local radius used when selecting fallback evasion positions.")
                .defineInRange("fallbackEvasionRadius", 5, 2, 12);

        COVER_SEARCH_MAX_CANDIDATES = builder
                .comment("Maximum number of cheap cover candidates scored per search pass before path validation. Lower values protect TPS in large fights.")
                .defineInRange("coverSearchMaxCandidates", 96, 8, 512);

        COVER_SEARCH_MAX_PATH_CHECKS = builder
                .comment("Maximum number of vanilla createPath checks allowed per cover/evasion search pass. Pathfinding is intentionally deferred to the top-scored candidates only.")
                .defineInRange("coverSearchMaxPathChecks", 3, 1, 32);

        COVER_SEARCHES_PER_TICK = builder
                .comment("Global server-thread budget for expensive cover searches per tick. Extra units retry with a short jittered delay.")
                .defineInRange("coverSearchesPerTick", 2, 1, 20);

        COVER_SEARCH_BUDGET_JITTER_TICKS = builder
                .comment("Small per-unit retry jitter when the global cover-search budget is exhausted, preventing synchronized pathfinding spikes.")
                .defineInRange("coverSearchBudgetJitterTicks", 6, 1, 40);

        builder.pop();


        builder.push("tactical_ai_phase_3");

        ENABLE_TACTICAL_SUPPRESSION = builder
                .comment("Enable generic tactical suppression for SEM units from nearby bullet impacts and sustained gunfire.")
                .define("enableTacticalSuppression", true);

        SUPPRESSION_IMPACT_RADIUS = builder
                .comment("Radius where bullet impacts or suppressive fire add pressure to tactical units.")
                .defineInRange("suppressionImpactRadius", 7.0D, 0.5D, 48.0D);

        SUPPRESSION_IMPACT_AMOUNT = builder
                .comment("Base suppression amount before distance falloff.")
                .defineInRange("suppressionImpactAmount", 0.24D, 0.0D, 5.0D);

        SUPPRESSION_DECAY_PER_TICK = builder
                .comment("Amount of suppression cleared from each unit every server tick.")
                .defineInRange("suppressionDecayPerTick", 0.018D, 0.0D, 1.0D);

        SUPPRESSION_STRONG_THRESHOLD = builder
                .comment("Suppression level where units request emergency cover using the Phase 2 cover system.")
                .defineInRange("suppressionStrongThreshold", 0.90D, 0.0D, 10.0D);

        SUPPRESSION_ACCURACY_SPREAD_PENALTY = builder
                .comment("Additional base spread degrees applied per suppression point while a unit keeps firing under pressure.")
                .defineInRange("suppressionAccuracySpreadPenalty", 2.0D, 0.0D, 20.0D);

        SUPPRESSION_COVER_LOCK_TICKS = builder
                .comment("Movement ownership duration used when strong suppression forces a SEEK_COVER request.")
                .defineInRange("suppressionCoverLockTicks", 70, 10, 20 * 20);

        SUPPRESSION_SOURCE_DEBOUNCE_TICKS = builder
                .comment("Minimum ticks between repeated suppression applications from the same source once a unit is already strongly suppressed.")
                .defineInRange("suppressionSourceDebounceTicks", 6, 0, 80);

        ENABLE_TACTICAL_HEARING = builder
                .comment("Enable generic combat sound awareness for SEM units.")
                .define("enableTacticalHearing", true);

        GUNSHOT_HEARING_RADIUS = builder
                .comment("Radius where hostile gunfire can be heard and investigated by SEM units without direct line of sight.")
                .defineInRange("gunshotHearingRadius", 42.0D, 1.0D, 128.0D);

        SOUND_MIN_STRENGTH = builder
                .comment("Minimum attenuated sound strength required for an idle unit to investigate hostile gunfire.")
                .defineInRange("soundMinStrength", 0.18D, 0.0D, 1.0D);

        GUNSHOT_MEMORY_TICKS = builder
                .comment("Ticks an idle unit remembers a heard hostile gunshot position.")
                .defineInRange("gunshotMemoryTicks", 160, 10, 20 * 60);

        SOUND_INVESTIGATE_SPEED = builder
                .comment("Navigation speed used while moving to investigate a heard hostile gunshot.")
                .defineInRange("soundInvestigateSpeed", 1.05D, 0.1D, 3.0D);

        SOUND_INVESTIGATE_REACHED_DISTANCE = builder
                .comment("Distance used to decide that a heard gunshot investigation point has been reached.")
                .defineInRange("soundInvestigateReachedDistance", 3.0D, 0.5D, 16.0D);

        builder.pop();

        builder.push("tactical_ai_phase_4");

        ENABLE_TACTICAL_DECISION_LAYER = builder
                .comment("Enable Phase 4 threat-pressure/combat-confidence tactical decision layer.")
                .define("enableTacticalDecisionLayer", true);

        TACTICAL_DECISION_INTERVAL_TICKS = builder
                .comment("Per-unit interval between expensive tactical confidence evaluations. Higher values reduce CPU and log churn.")
                .defineInRange("tacticalDecisionIntervalTicks", 12, 2, 100);

        TACTICAL_DECISION_COVER_LOCK_TICKS = builder
                .comment("Movement ownership duration when Phase 4 pressure/confidence requests cover or retreat.")
                .defineInRange("tacticalDecisionCoverLockTicks", 70, 10, 20 * 20);

        TACTICAL_RETREAT_PRESSURE_THRESHOLD = builder
                .comment("Threat pressure above which low-confidence units prefer emergency cover/retreat behavior.")
                .defineInRange("tacticalRetreatPressureThreshold", 0.88D, 0.0D, 1.0D);

        TACTICAL_COVER_PRESSURE_THRESHOLD = builder
                .comment("Threat pressure above which units prefer cover instead of active maneuvering.")
                .defineInRange("tacticalCoverPressureThreshold", 0.62D, 0.0D, 1.0D);

        TACTICAL_PUSH_CONFIDENCE_THRESHOLD = builder
                .comment("Combat confidence above which units may push or reposition aggressively when pressure is manageable.")
                .defineInRange("tacticalPushConfidenceThreshold", 0.72D, 0.0D, 1.0D);

        ENABLE_SQUAD_CONTEXT = builder
                .comment("Enable lightweight local squad context for Phase 4 decisions: nearby allies, engaged allies, suppressed allies and local numerical advantage.")
                .define("enableSquadContext", true);

        ENABLE_SQUAD_COMBAT_MEMORY = builder
                .comment("Enable shared squad combat memory for recently observed/heard/suppression-derived enemy positions.")
                .define("enableSquadCombatMemory", true);

        SQUAD_CONTEXT_RADIUS = builder
                .comment("Radius used to build a local squad context around each unit.")
                .defineInRange("squadContextRadius", 18.0D, 4.0D, 96.0D);

        SQUAD_MEMORY_SHARE_RADIUS = builder
                .comment("Maximum distance where units of the same faction can reuse shared last-known enemy memory.")
                .defineInRange("squadMemoryShareRadius", 36.0D, 4.0D, 128.0D);

        SQUAD_MEMORY_TICKS = builder
                .comment("Ticks before shared squad enemy memory becomes stale.")
                .defineInRange("squadMemoryTicks", 120, 10, 20 * 60);

        SQUAD_MEMORY_GRID_SIZE = builder
                .comment("Coarse grid size used to group squad combat memory entries by faction and area.")
                .defineInRange("squadMemoryGridSize", 24, 8, 96);

        ENABLE_DISTANCE_TARGET_PRIORITY = builder
                .comment("Enable lightweight distance-priority target selection and close-threat retargeting. This is intentionally simple and can be expanded later with threat scoring.")
                .define("enableDistanceTargetPriority", true);

        CLOSE_THREAT_SWITCH_DISTANCE = builder
                .comment("Distance in blocks where a newly visible hostile is considered an immediate close threat and can steal target priority from a farther target.")
                .defineInRange("closeThreatSwitchDistance", 8.0D, 1.0D, 48.0D);

        CLOSE_THREAT_SWITCH_RATIO = builder
                .comment("A visible candidate may replace the current target when its distance squared is below currentDistanceSquared * this ratio. Lower values make retargeting more conservative.")
                .defineInRange("closeThreatSwitchRatio", 0.72D, 0.05D, 1.0D);

        CLOSE_THREAT_SWITCH_SCAN_INTERVAL_TICKS = builder
                .comment("Per-unit interval for scanning close-threat target switches while already engaged.")
                .defineInRange("closeThreatSwitchScanIntervalTicks", 6, 1, 40);

        ENABLE_SEM_LOOP_SPARK_PROFILING = builder
                .comment("Automatically run Spark profiler for each /sem test loop_all scenario and attach the Spark URL to the scenario JSONL log. Requires Spark to be installed.")
                .define("enableSemLoopSparkProfiling", true);

        SEM_LOOP_SPARK_WARMUP_TICKS = builder
                .comment("Ticks to wait after scenario setup before starting Spark profiling in /sem test loop_all. This keeps arena setup, spawning, loadout assignment and first-tick initialization out of the profiler.")
                .defineInRange("semLoopSparkWarmupTicks", 40, 0, 20 * 30);

        ENABLE_SEM_TICK_PROFILER = builder
                .comment("Log lightweight server-tick timing samples and slow-tick warnings into active /sem scenario JSONL logs.")
                .define("enableSemTickProfiler", true);

        ENABLE_SEM_TICK_SAMPLES = builder
                .comment("Write periodic TICK_SAMPLE entries while a /sem scenario is running. TICK_WARN entries are still written when the threshold is exceeded.")
                .define("enableSemTickSamples", true);

        SEM_TICK_SAMPLE_INTERVAL_TICKS = builder
                .comment("Interval for periodic TICK_SAMPLE entries in active /sem scenario logs.")
                .defineInRange("semTickSampleIntervalTicks", 20, 1, 20 * 60);

        SEM_TICK_WARN_THRESHOLD_MS = builder
                .comment("Server tick duration in milliseconds that creates a TICK_WARN entry in active /sem scenario logs.")
                .defineInRange("semTickWarnThresholdMs", 50.0D, 1.0D, 10000.0D);

        SEM_TICK_SEVERE_THRESHOLD_MS = builder
                .comment("Server tick duration in milliseconds that marks a TICK_WARN entry as severe.")
                .defineInRange("semTickSevereThresholdMs", 100.0D, 1.0D, 10000.0D);

        builder.pop();
    }

    public static boolean enableSoftObstacleBreaking() {
        return ENABLE_SOFT_OBSTACLE_BREAKING == null || ENABLE_SOFT_OBSTACLE_BREAKING.get();
    }

    public static boolean enableLadderFallback() {
        return ENABLE_LADDER_FALLBACK == null || ENABLE_LADDER_FALLBACK.get();
    }

    public static boolean enablePreciseFoliageLos() {
        return ENABLE_PRECISE_FOLIAGE_LOS == null || ENABLE_PRECISE_FOLIAGE_LOS.get();
    }

    public static boolean enableHazardousCoverRejection() {
        return ENABLE_HAZARDOUS_COVER_REJECTION == null || ENABLE_HAZARDOUS_COVER_REJECTION.get();
    }

    public static int aiLosCacheTicks() {
        return AI_LOS_CACHE_TICKS == null ? 4 : AI_LOS_CACHE_TICKS.get();
    }

    public static boolean enableSmartCoverSearch() {
        return ENABLE_SMART_COVER_SEARCH == null || ENABLE_SMART_COVER_SEARCH.get();
    }

    public static boolean enableCoverPathValidation() {
        return ENABLE_COVER_PATH_VALIDATION == null || ENABLE_COVER_PATH_VALIDATION.get();
    }

    public static boolean enableFallbackEvasion() {
        return ENABLE_FALLBACK_EVASION == null || ENABLE_FALLBACK_EVASION.get();
    }

    public static int coverSearchCooldownTicks() {
        return COVER_SEARCH_COOLDOWN_TICKS == null ? 30 : COVER_SEARCH_COOLDOWN_TICKS.get();
    }

    public static int coverRelaxedExtraRadius() {
        return COVER_RELAXED_EXTRA_RADIUS == null ? 4 : COVER_RELAXED_EXTRA_RADIUS.get();
    }

    public static int fallbackEvasionRadius() {
        return FALLBACK_EVASION_RADIUS == null ? 5 : FALLBACK_EVASION_RADIUS.get();
    }

    public static int coverSearchMaxCandidates() {
        return COVER_SEARCH_MAX_CANDIDATES == null ? 96 : COVER_SEARCH_MAX_CANDIDATES.get();
    }

    public static int coverSearchMaxPathChecks() {
        return COVER_SEARCH_MAX_PATH_CHECKS == null ? 3 : COVER_SEARCH_MAX_PATH_CHECKS.get();
    }

    public static int coverSearchesPerTick() {
        return COVER_SEARCHES_PER_TICK == null ? 2 : COVER_SEARCHES_PER_TICK.get();
    }

    public static int coverSearchBudgetJitterTicks() {
        return COVER_SEARCH_BUDGET_JITTER_TICKS == null ? 6 : COVER_SEARCH_BUDGET_JITTER_TICKS.get();
    }

    public static boolean enableTacticalSuppression() {
        return ENABLE_TACTICAL_SUPPRESSION == null || ENABLE_TACTICAL_SUPPRESSION.get();
    }

    public static double suppressionImpactRadius() {
        return SUPPRESSION_IMPACT_RADIUS == null ? 7.0D : SUPPRESSION_IMPACT_RADIUS.get();
    }

    public static double suppressionImpactAmount() {
        return SUPPRESSION_IMPACT_AMOUNT == null ? 0.24D : SUPPRESSION_IMPACT_AMOUNT.get();
    }

    public static double suppressionDecayPerTick() {
        return SUPPRESSION_DECAY_PER_TICK == null ? 0.018D : SUPPRESSION_DECAY_PER_TICK.get();
    }

    public static double suppressionStrongThreshold() {
        return SUPPRESSION_STRONG_THRESHOLD == null ? 0.90D : SUPPRESSION_STRONG_THRESHOLD.get();
    }

    public static double suppressionAccuracySpreadPenalty() {
        return SUPPRESSION_ACCURACY_SPREAD_PENALTY == null ? 2.0D : SUPPRESSION_ACCURACY_SPREAD_PENALTY.get();
    }

    public static int suppressionCoverLockTicks() {
        return SUPPRESSION_COVER_LOCK_TICKS == null ? 70 : SUPPRESSION_COVER_LOCK_TICKS.get();
    }

    public static int suppressionSourceDebounceTicks() {
        return SUPPRESSION_SOURCE_DEBOUNCE_TICKS == null ? 6 : SUPPRESSION_SOURCE_DEBOUNCE_TICKS.get();
    }

    public static boolean enableTacticalHearing() {
        return ENABLE_TACTICAL_HEARING == null || ENABLE_TACTICAL_HEARING.get();
    }

    public static double gunshotHearingRadius() {
        return GUNSHOT_HEARING_RADIUS == null ? 42.0D : GUNSHOT_HEARING_RADIUS.get();
    }

    public static double soundMinStrength() {
        return SOUND_MIN_STRENGTH == null ? 0.18D : SOUND_MIN_STRENGTH.get();
    }

    public static int gunshotMemoryTicks() {
        return GUNSHOT_MEMORY_TICKS == null ? 160 : GUNSHOT_MEMORY_TICKS.get();
    }

    public static double soundInvestigateSpeed() {
        return SOUND_INVESTIGATE_SPEED == null ? 1.05D : SOUND_INVESTIGATE_SPEED.get();
    }

    public static double soundInvestigateReachedDistance() {
        return SOUND_INVESTIGATE_REACHED_DISTANCE == null ? 3.0D : SOUND_INVESTIGATE_REACHED_DISTANCE.get();
    }

    public static boolean enableTacticalDecisionLayer() {
        return ENABLE_TACTICAL_DECISION_LAYER == null || ENABLE_TACTICAL_DECISION_LAYER.get();
    }

    public static int tacticalDecisionIntervalTicks() {
        return TACTICAL_DECISION_INTERVAL_TICKS == null ? 12 : TACTICAL_DECISION_INTERVAL_TICKS.get();
    }

    public static int tacticalDecisionCoverLockTicks() {
        return TACTICAL_DECISION_COVER_LOCK_TICKS == null ? 70 : TACTICAL_DECISION_COVER_LOCK_TICKS.get();
    }

    public static double tacticalRetreatPressureThreshold() {
        return TACTICAL_RETREAT_PRESSURE_THRESHOLD == null ? 0.88D : TACTICAL_RETREAT_PRESSURE_THRESHOLD.get();
    }

    public static double tacticalCoverPressureThreshold() {
        return TACTICAL_COVER_PRESSURE_THRESHOLD == null ? 0.62D : TACTICAL_COVER_PRESSURE_THRESHOLD.get();
    }

    public static double tacticalPushConfidenceThreshold() {
        return TACTICAL_PUSH_CONFIDENCE_THRESHOLD == null ? 0.72D : TACTICAL_PUSH_CONFIDENCE_THRESHOLD.get();
    }

    public static boolean enableSquadContext() {
        return ENABLE_SQUAD_CONTEXT == null || ENABLE_SQUAD_CONTEXT.get();
    }

    public static boolean enableSquadCombatMemory() {
        return ENABLE_SQUAD_COMBAT_MEMORY == null || ENABLE_SQUAD_COMBAT_MEMORY.get();
    }

    public static double squadContextRadius() {
        return SQUAD_CONTEXT_RADIUS == null ? 18.0D : SQUAD_CONTEXT_RADIUS.get();
    }

    public static double squadMemoryShareRadius() {
        return SQUAD_MEMORY_SHARE_RADIUS == null ? 36.0D : SQUAD_MEMORY_SHARE_RADIUS.get();
    }

    public static int squadMemoryTicks() {
        return SQUAD_MEMORY_TICKS == null ? 120 : SQUAD_MEMORY_TICKS.get();
    }

    public static int squadMemoryGridSize() {
        return SQUAD_MEMORY_GRID_SIZE == null ? 24 : SQUAD_MEMORY_GRID_SIZE.get();
    }


    public static boolean enableDistanceTargetPriority() {
        return ENABLE_DISTANCE_TARGET_PRIORITY == null || ENABLE_DISTANCE_TARGET_PRIORITY.get();
    }

    public static double closeThreatSwitchDistance() {
        return CLOSE_THREAT_SWITCH_DISTANCE == null ? 8.0D : CLOSE_THREAT_SWITCH_DISTANCE.get();
    }

    public static double closeThreatSwitchRatio() {
        return CLOSE_THREAT_SWITCH_RATIO == null ? 0.72D : CLOSE_THREAT_SWITCH_RATIO.get();
    }

    public static int closeThreatSwitchScanIntervalTicks() {
        return CLOSE_THREAT_SWITCH_SCAN_INTERVAL_TICKS == null ? 6 : CLOSE_THREAT_SWITCH_SCAN_INTERVAL_TICKS.get();
    }

    public static boolean enableSemLoopSparkProfiling() {
        return ENABLE_SEM_LOOP_SPARK_PROFILING == null || ENABLE_SEM_LOOP_SPARK_PROFILING.get();
    }

    public static int semLoopSparkWarmupTicks() {
        return SEM_LOOP_SPARK_WARMUP_TICKS == null ? 40 : SEM_LOOP_SPARK_WARMUP_TICKS.get();
    }

    public static boolean enableSemTickProfiler() {
        return ENABLE_SEM_TICK_PROFILER == null || ENABLE_SEM_TICK_PROFILER.get();
    }

    public static boolean enableSemTickSamples() {
        return ENABLE_SEM_TICK_SAMPLES == null || ENABLE_SEM_TICK_SAMPLES.get();
    }

    public static int semTickSampleIntervalTicks() {
        return SEM_TICK_SAMPLE_INTERVAL_TICKS == null ? 20 : SEM_TICK_SAMPLE_INTERVAL_TICKS.get();
    }

    public static double semTickWarnThresholdMs() {
        return SEM_TICK_WARN_THRESHOLD_MS == null ? 50.0D : SEM_TICK_WARN_THRESHOLD_MS.get();
    }

    public static double semTickSevereThresholdMs() {
        return SEM_TICK_SEVERE_THRESHOLD_MS == null ? 100.0D : SEM_TICK_SEVERE_THRESHOLD_MS.get();
    }

}
