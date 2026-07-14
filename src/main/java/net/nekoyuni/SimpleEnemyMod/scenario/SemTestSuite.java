package net.nekoyuni.SimpleEnemyMod.scenario;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Named groups of SEM scenarios for targeted regression and performance profiling.
 */
public enum SemTestSuite {
    HIGH_LOAD("high_load", "Large or expensive firefights used for TPS/Spark regression checks.", List.of(
            SemScenarioDefinition.URBAN_10V10,
            SemScenarioDefinition.LONG_RANGE_10V10,
            SemScenarioDefinition.LONG_RANGE_SUPPRESSION_TEST,
            SemScenarioDefinition.ROOFTOP_5V5
    )),

    PATHFINDING("pathfinding", "Movement, vertical navigation, soft-obstacle and hazard regression checks.", List.of(
            SemScenarioDefinition.LADDER_TEST,
            SemScenarioDefinition.NAVIGATION_POLISH_TEST,
            SemScenarioDefinition.SOFT_OBSTACLE_BREAK_TEST,
            SemScenarioDefinition.HAZARD_AVOIDANCE_TEST
    )),

    COVER("cover", "Cover selection, evasion, confidence and killzone behavior.", List.of(
            SemScenarioDefinition.COVER_CONFIDENCE_TEST,
            SemScenarioDefinition.RETREAT_TEST,
            SemScenarioDefinition.KILLZONE_AWARENESS_TEST,
            SemScenarioDefinition.URBAN_10V10
    )),

    SUPPRESSION("suppression", "Suppression, bullet-impact pressure and gunshot awareness scenarios.", List.of(
            SemScenarioDefinition.SUPPRESSION_TEST,
            SemScenarioDefinition.LONG_RANGE_SUPPRESSION_TEST,
            SemScenarioDefinition.SOUND_AWARENESS_IDLE
    )),

    SQUAD_MEMORY("squad_memory", "Shared enemy memory and last-known-position behavior.", List.of(
            SemScenarioDefinition.SQUAD_MEMORY_TEST,
            SemScenarioDefinition.SQUAD_MEMORY_LOS_LOST_TEST,
            SemScenarioDefinition.SOUND_AWARENESS_IDLE
    )),

    TARGETING("targeting", "Distance-priority target selection and close-threat retargeting.", List.of(
            SemScenarioDefinition.TARGET_DISTANCE_PRIORITY_TEST,
            SemScenarioDefinition.CLOSE_THREAT_SWITCH_TEST,
            SemScenarioDefinition.GLASS_FOLIAGE_LOS_TEST
    )),

    CQB("cqb", "Indoor, doorway and close-range decision behavior.", List.of(
            SemScenarioDefinition.INDOOR_CQB,
            SemScenarioDefinition.DOORWAY_CQB_TEST,
            SemScenarioDefinition.CLOSE_THREAT_SWITCH_TEST
    ));

    private final String id;
    private final String description;
    private final List<SemScenarioDefinition> scenarios;

    SemTestSuite(String id, String description, List<SemScenarioDefinition> scenarios) {
        this.id = id;
        this.description = description;
        this.scenarios = scenarios;
    }

    public String id() {
        return this.id;
    }

    public String description() {
        return this.description;
    }

    public List<SemScenarioDefinition> scenarios() {
        return this.scenarios;
    }

    public String directoryName() {
        return "_" + this.id;
    }

    @Nullable
    public static SemTestSuite byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = normalize(id);
        if ("pathfindig".equals(normalized)) {
            normalized = "pathfinding";
        }
        for (SemTestSuite suite : values()) {
            if (suite.id.equals(normalized)) {
                return suite;
            }
        }
        return null;
    }

    public static String ids() {
        return String.join(", ", Arrays.stream(values()).map(SemTestSuite::id).toList());
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
