package net.nekoyuni.SimpleEnemyMod.entity.ai.profile.role;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

import java.util.Locale;

/**
 * Generic tactical behavior profile for the current private repository architecture.
 *
 * <p>This is adapted from the public AI branch but intentionally avoids public-only dependencies such as bandit types,
 * scenario loadout services, loot roles or faction profile classes.</p>
 */
public record RoleBehaviorProfile(
        UnitRole role,
        Archetype archetype,
        boolean canLeadSquad,
        float confidenceMultiplier,
        float threatMultiplier,
        float advanceBias,
        float coverBias,
        float suppressionSensitivity,
        float preferredMinRange,
        float preferredMaxRange,
        boolean prefersAmbush,
        boolean frontline
) {
    public enum Archetype {
        RIFLEMAN,
        MG,
        SHOTGUN,
        DMR,
        PISTOL,
        SMG,
        SUPPORT
    }

    public static RoleBehaviorProfile forUnit(AbstractUnit unit) {
        UnitRole role = unit != null ? unit.getRole() : UnitRole.DEFAULT;
        return of(role, inferArchetype(unit));
    }

    public static RoleBehaviorProfile of(UnitRole role, Archetype archetype) {
        if (role == null) role = UnitRole.DEFAULT;
        if (archetype == null) archetype = Archetype.RIFLEMAN;

        boolean leader = role == UnitRole.SQUAD_LEADER || role == UnitRole.FRIENDLY_SQUAD_LEADER;
        boolean squadUnit = role == UnitRole.SQUAD_UNIT || role == UnitRole.FRIENDLY_SQUAD_UNIT;
        float leaderConfidence = leader ? 1.06F : 1.0F;
        float squadCoverBias = squadUnit ? 0.06F : 0.0F;

        return switch (archetype) {
            case MG -> new RoleBehaviorProfile(role, archetype, leader, 0.92F * leaderConfidence, 0.96F, -0.10F, 0.18F + squadCoverBias, 0.82F, 14.0F, 42.0F, false, true);
            case SHOTGUN -> new RoleBehaviorProfile(role, archetype, leader, 0.88F * leaderConfidence, 1.08F, 0.10F, 0.06F + squadCoverBias, 1.10F, 4.0F, 13.0F, true, true);
            case DMR -> new RoleBehaviorProfile(role, archetype, leader, 0.96F * leaderConfidence, 0.90F, -0.18F, 0.12F + squadCoverBias, 0.72F, 22.0F, 58.0F, false, true);
            case PISTOL -> new RoleBehaviorProfile(role, archetype, leader, 0.74F * leaderConfidence, 1.12F, -0.12F, 0.18F + squadCoverBias, 1.18F, 5.0F, 16.0F, true, false);
            case SMG -> new RoleBehaviorProfile(role, archetype, leader, 0.90F * leaderConfidence, 1.04F, 0.05F, 0.08F + squadCoverBias, 1.04F, 6.0F, 22.0F, true, true);
            case SUPPORT -> new RoleBehaviorProfile(role, archetype, leader, 0.62F * leaderConfidence, 1.15F, -0.24F, 0.24F + squadCoverBias, 1.22F, 10.0F, 28.0F, false, false);
            case RIFLEMAN -> new RoleBehaviorProfile(role, archetype, leader, 1.00F * leaderConfidence, 1.00F, leader ? 0.04F : 0.0F, squadCoverBias, 1.00F, 10.0F, 34.0F, false, true);
        };
    }

    private static Archetype inferArchetype(AbstractUnit unit) {
        if (unit == null) return Archetype.RIFLEMAN;

        String scenarioProfile = unit.getPersistentData().getString("SemScenarioLoadoutProfile").toLowerCase(Locale.ROOT);
        if (!scenarioProfile.isBlank()) {
            Archetype fromScenario = inferFromText(scenarioProfile);
            if (fromScenario != Archetype.RIFLEMAN) {
                return fromScenario;
            }
        }

        String source = unit.getClass().getSimpleName().toLowerCase(Locale.ROOT) + " " + weaponId(unit.getMainHandItem());
        return inferFromText(source);
    }

    private static Archetype inferFromText(String source) {
        if (source == null) return Archetype.RIFLEMAN;
        String text = source.toLowerCase(Locale.ROOT);
        if (text.contains("m249") || text.contains("rpk") || text.contains("lmg") || text.contains("mg")) return Archetype.MG;
        if (text.contains("shotgun") || text.contains("m1014") || text.contains("aa12") || text.contains("m870") || text.contains("db_short")) return Archetype.SHOTGUN;
        if (text.contains("dmr") || text.contains("mk14") || text.contains("msks")) return Archetype.DMR;
        if (text.contains("pistol") || text.contains("m9") || text.contains("m1911") || text.contains("glock") || text.contains("taurus")) return Archetype.PISTOL;
        if (text.contains("smg") || text.contains("mp5") || text.contains("ump") || text.contains("p90")) return Archetype.SMG;
        if (text.contains("support")) return Archetype.SUPPORT;
        return Archetype.RIFLEMAN;
    }

    private static String weaponId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
    }
}
