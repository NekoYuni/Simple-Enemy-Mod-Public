package net.nekoyuni.SimpleEnemyMod.entity.ai.decision;

/**
 * High-level tactical intent selected by the Phase 4 confidence/threat layer.
 *
 * <p>This is deliberately separate from SoldierState: SoldierState remains the goal execution state, while
 * TacticalCombatState describes the unit's current combat intent.</p>
 */
public enum TacticalCombatState {
    HOLD,
    PUSH,
    COVER,
    RETREAT,
    AMBUSH
}
