package net.nekoyuni.SimpleEnemyMod.scenario;

/**
 * Built-in scenario metadata for reproducible in-game AI integration tests.
 *
 * <p>The current list intentionally keeps only scenarios that validate generic Arma-like SEM AI systems.
 * Public-branch gameplay scenarios tied to bandits, reinforcements, loot, ammo-bearers or melee-only behavior were
 * removed from the active suite so loop_all and test suites stay focused on the migrated architecture.</p>
 */
public enum SemScenarioDefinition {
    // Phase 1 / movement / perception regression coverage.
    LADDER_TEST("ladder_test", 900),
    NAVIGATION_POLISH_TEST("navigation_polish_test", 1400),
    GLASS_FOLIAGE_LOS_TEST("glass_foliage_los_test", 1000),
    SOFT_OBSTACLE_BREAK_TEST("soft_obstacle_break_test", 1000),
    HAZARD_AVOIDANCE_TEST("hazard_avoidance_test", 1200),

    // Phase 2 cover and tactical movement.
    ROOFTOP_5V5("rooftop_5v5", 1200),
    URBAN_10V10("urban_10v10", 1800),
    RETREAT_TEST("retreat_test", 1200),
    COVER_CONFIDENCE_TEST("cover_confidence_test", 1400),
    KILLZONE_AWARENESS_TEST("killzone_awareness_test", 1400),

    // Phase 3 suppression and sound awareness.
    SUPPRESSION_TEST("suppression_test", 1400),
    SOUND_AWARENESS_IDLE("sound_awareness_idle", 1200),
    LONG_RANGE_SUPPRESSION_TEST("long_range_suppression_test", 2000),

    // Phase 4 decision layer, target selection and squad memory.
    TARGET_DISTANCE_PRIORITY_TEST("target_distance_priority_test", 1000),
    CLOSE_THREAT_SWITCH_TEST("close_threat_switch_test", 1200),
    SQUAD_MEMORY_TEST("squad_memory_test", 1400),
    SQUAD_MEMORY_LOS_LOST_TEST("squad_memory_los_lost_test", 1400),
    AMBUSH_TEST("ambush_test", 1400),
    ROLE_PROFILE_TEST("role_profile_test", 1400),
    TACTICAL_SPEED_TEST("tactical_speed_test", 1400),

    // CQB and high-load scenarios.
    INDOOR_CQB("indoor_cqb", 1200),
    DOORWAY_CQB_TEST("doorway_cqb_test", 1400),
    LONG_RANGE_5V5("long_range_5v5", 1800),
    LONG_RANGE_10V10("long_range_10v10", 2200);

    private final String id;
    private final int defaultDurationTicks;

    SemScenarioDefinition(String id, int defaultDurationTicks) {
        this.id = id;
        this.defaultDurationTicks = defaultDurationTicks;
    }

    public String id() {
        return this.id;
    }

    public int defaultDurationTicks() {
        return this.defaultDurationTicks;
    }

    public static SemScenarioDefinition byId(String id) {
        for (SemScenarioDefinition scenario : values()) {
            if (scenario.id.equalsIgnoreCase(id)) {
                return scenario;
            }
        }
        return null;
    }
}
