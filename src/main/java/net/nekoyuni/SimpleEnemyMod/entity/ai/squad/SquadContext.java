package net.nekoyuni.SimpleEnemyMod.entity.ai.squad;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Lightweight local squad context for Phase 4 decisions.
 *
 * <p>This is intentionally not a heavy squad-order system. It is a generic, tag-based context snapshot that lets
 * individual units bias their tactical decisions using nearby allies and recently shared enemy memory.</p>
 */
public record SquadContext(
        int nearbyAllies,
        int engagedAllies,
        int suppressedAllies,
        int nearbyHostiles,
        float averageAllyPressure,
        float averageAllyConfidence,
        boolean hasSharedEnemyMemory,
        @Nullable Vec3 sharedEnemyPosition,
        long sharedEnemyAgeTicks,
        float sharedDangerPressure,
        String memorySource
) {
    public static SquadContext empty() {
        return new SquadContext(0, 0, 0, 0, 0.0F, 0.50F, false, null, Long.MAX_VALUE, 0.0F, "none");
    }

    public int localFireteamSize() {
        return this.nearbyAllies + 1;
    }

    public boolean hasFireSupport() {
        return this.engagedAllies >= 1;
    }

    public boolean isLocallyOutnumbered() {
        return this.nearbyHostiles > this.localFireteamSize();
    }

    public boolean hasFreshSharedEnemyMemory(long maxAgeTicks) {
        return this.hasSharedEnemyMemory && this.sharedEnemyPosition != null && this.sharedEnemyAgeTicks <= maxAgeTicks;
    }
}
