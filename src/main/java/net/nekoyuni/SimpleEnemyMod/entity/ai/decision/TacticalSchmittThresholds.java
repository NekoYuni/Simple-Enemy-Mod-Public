package net.nekoyuni.SimpleEnemyMod.entity.ai.decision;

import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;

/**
 * Hysteresis thresholds for tactical state transitions.
 *
 * <p>The enter/exit gap prevents a unit from oscillating every few ticks when pressure/confidence values hover near a
 * boundary.</p>
 */
public record TacticalSchmittThresholds(
        float enterPush,
        float exitPush,
        float enterCover,
        float exitCover,
        float enterRetreat,
        float exitRetreat,
        float enterAmbush,
        float exitAmbush
) {
    public static TacticalSchmittThresholds forRole(UnitRole role) {
        if (role == UnitRole.SQUAD_LEADER || role == UnitRole.FRIENDLY_SQUAD_LEADER) {
            return new TacticalSchmittThresholds(
                    0.70F, 0.52F,
                    0.50F, 0.42F,
                    0.22F, 0.36F,
                    0.42F, 0.58F
            );
        }
        if (role == UnitRole.SQUAD_UNIT || role == UnitRole.FRIENDLY_SQUAD_UNIT) {
            return new TacticalSchmittThresholds(
                    0.74F, 0.55F,
                    0.48F, 0.40F,
                    0.20F, 0.34F,
                    0.45F, 0.60F
            );
        }
        return new TacticalSchmittThresholds(
                0.76F, 0.57F,
                0.46F, 0.38F,
                0.18F, 0.32F,
                0.48F, 0.62F
        );
    }
}
