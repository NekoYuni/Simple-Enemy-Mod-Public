package net.nekoyuni.SimpleEnemyMod.entity.ai.navigation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/**
 * Shared movement entry point for tactical AI goals.
 *
 * Phase 1 keeps this helper deliberately small: it adds ladder fallback routing while
 * preserving vanilla navigation for ordinary movement and avoiding scenario/squad dependencies.
 */
public final class TacticalNavigationHelper {

    private TacticalNavigationHelper() {
        // Utility class.
    }

    public static boolean moveTo(Mob mob, LivingEntity target, double speed) {
        if (target == null) {
            return false;
        }
        if (tryLadderRoute(mob, target.position(), speed)) {
            return true;
        }
        return mob.getNavigation().moveTo(target, speed);
    }

    public static boolean moveTo(Mob mob, Vec3 target, double speed) {
        if (target == null) {
            return false;
        }
        if (tryLadderRoute(mob, target, speed)) {
            return true;
        }
        return mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    public static boolean moveTo(Mob mob, double x, double y, double z, double speed) {
        return moveTo(mob, new Vec3(x, y, z), speed);
    }

    private static boolean tryLadderRoute(Mob mob, Vec3 target, double speed) {
        return mob instanceof PathfinderMob pathfinderMob && LadderRouteNavigator.moveTo(pathfinderMob, target, speed);
    }
}

