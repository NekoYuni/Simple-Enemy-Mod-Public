package net.nekoyuni.SimpleEnemyMod.entity.ai.tactical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.TacticalHazardHelper;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.StrategyType;
import net.nekoyuni.SimpleEnemyMod.scenario.SemTickProfiler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generic Phase 2 cover search adapted from the public AI branch.
 *
 * This class deliberately stays independent of bandits, loot, scenario logging,
 * faction profiles and suppression systems. It scores real hard cover for the
 * current AbstractUnit architecture and exposes a short fallback evasion result
 * when no reachable hard-cover position exists.
 */
public final class TacticalCoverFinder {

    private static final double RAY_STEP = 0.25D;

    private static long budgetTick = Long.MIN_VALUE;
    private static int searchesThisTick = 0;

    private TacticalCoverFinder() {
        // Utility class.
    }

    /**
     * Server-thread budget guard for expensive cover searches.
     *
     * Cover search can call vanilla pathfinding, which is one of the most
     * expensive operations on the server thread. Keeping a tiny global per-tick
     * budget prevents 10+ units from synchronizing and running full searches in
     * the same tick.
     */
    public static boolean tryAcquireCoverSearchSlot(AbstractUnit unit) {
        long now = unit.level().getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            searchesThisTick = 0;
        }

        int max = Math.max(1, TacticalAiConfig.coverSearchesPerTick());
        if (searchesThisTick >= max) {
            SemTickProfiler.recordCoverBudgetDenied();
            return false;
        }

        searchesThisTick++;
        SemTickProfiler.recordCoverSearchStarted();
        return true;
    }

    public static CoverSearchResult findCoverOrEvasion(AbstractUnit unit, LivingEntity threat, int searchRadius) {
        if (unit == null || threat == null || !unit.isAlive() || !threat.isAlive()) {
            return CoverSearchResult.none();
        }

        if (!TacticalAiConfig.enableSmartCoverSearch()) {
            BlockPos legacy = findFirstReachableHardCover(unit, threat, searchRadius);
            return legacy == null ? CoverSearchResult.none() : CoverSearchResult.cover(legacy, 0.0D, "legacy");
        }

        BlockPos origin = unit.getCoverSearchOrigin() != null ? unit.getCoverSearchOrigin() : unit.blockPosition();
        Vec3 threatEyes = threat.getEyePosition();

        CoverCandidate strictBest = scanCover(unit, threat, searchRadius, origin, threatEyes, true);
        if (strictBest != null) {
            return CoverSearchResult.cover(strictBest.pos, strictBest.score, "strict");
        }

        int relaxedRadius = Math.max(searchRadius, searchRadius + TacticalAiConfig.coverRelaxedExtraRadius());
        CoverCandidate relaxedBest = scanCover(unit, threat, relaxedRadius, origin, threatEyes, false);
        if (relaxedBest != null) {
            return CoverSearchResult.cover(relaxedBest.pos, relaxedBest.score, "relaxed");
        }

        if (TacticalAiConfig.enableFallbackEvasion()) {
            BlockPos evasion = findFallbackEvasion(unit, threat, TacticalAiConfig.fallbackEvasionRadius());
            if (evasion != null) {
                return CoverSearchResult.evasion(evasion, "fallback_evasion");
            }
        }

        return CoverSearchResult.none();
    }

    @Nullable
    public static BlockPos findBestCover(AbstractUnit unit, LivingEntity threat, int searchRadius) {
        CoverSearchResult result = findCoverOrEvasion(unit, threat, searchRadius);
        return result.isCover() ? result.position() : null;
    }

    @Nullable
    private static CoverCandidate scanCover(AbstractUnit unit, LivingEntity threat, int searchRadius,
                                            BlockPos origin, Vec3 threatEyes, boolean strict) {
        List<CoverCandidate> candidates = new ArrayList<>();
        int maxCandidates = Math.max(8, TacticalAiConfig.coverSearchMaxCandidates());

        scan:
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        CoverCandidate scored = evaluateCoverCandidate(unit, threat, candidate, origin, threatEyes, strict);
                        if (scored != null) {
                            SemTickProfiler.recordCoverCandidateScored();
                            candidates.add(scored);
                            if (candidates.size() >= maxCandidates) {
                                break scan;
                            }
                        }
                    }
                }
            }
        }

        return selectReachableCandidate(unit, candidates);
    }

    @Nullable
    private static CoverCandidate selectReachableCandidate(AbstractUnit unit, List<CoverCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingDouble(CoverCandidate::score).reversed());

        if (!TacticalAiConfig.enableCoverPathValidation()) {
            return candidates.get(0);
        }

        int maxPathChecks = Math.max(1, TacticalAiConfig.coverSearchMaxPathChecks());
        int checks = 0;

        for (CoverCandidate candidate : candidates) {
            if (isReachable(unit, candidate.pos())) {
                return candidate;
            }

            checks++;
            if (checks >= maxPathChecks) {
                break;
            }
        }

        return null;
    }

    @Nullable
    private static CoverCandidate evaluateCoverCandidate(AbstractUnit unit, LivingEntity threat, BlockPos candidate,
                                                         BlockPos origin, Vec3 threatEyes, boolean strict) {
        Level level = unit.level();
        if (!isCandidateStandable(unit, candidate) || isOccupiedByUnit(unit, candidate)) {
            return null;
        }

        BlockPos blockingBlock = findHardBlockingCoverBlock(level, candidate, threatEyes, unit);
        boolean hardBlocked = blockingBlock != null;
        if (strict && !hardBlocked) {
            return null;
        }

        double blockerStrength = hardBlocked
                ? TacticalBlockClassifier.hardCoverStrength(level.getBlockState(blockingBlock), level, blockingBlock)
                : partialHardCoverStrength(level, candidate, threat, unit);

        // Relaxed cover may use partial geometry, but never vegetation/glass/soft cover only.
        if (!strict && blockerStrength < 0.30D) {
            return null;
        }

        double distanceFromOrigin = Math.sqrt(candidate.distSqr(origin));
        double distanceFromThreat = Math.sqrt(candidate.distSqr(threat.blockPosition()));
        double idealDistance = idealCoverDistance(unit.getStrategy(), unit.distanceTo(threat));
        boolean hasPeekLane = hasPeekLane(level, unit, candidate, threat);
        double exposurePenalty = exposurePenalty(level, candidate, threat, unit);
        double allySpacing = allySpacingScore(unit, candidate);
        double lateralOffset = lateralOffsetScore(candidate, origin, threat.blockPosition());

        if (!strict && exposurePenalty > 0.72D) {
            return null;
        }

        double score = 0.0D;
        score += blockerStrength * (strict ? 14.0D : 10.0D);
        score += distanceFromThreat * 0.90D;
        score -= distanceFromOrigin * (strict ? 1.30D : 0.90D);
        score -= Math.abs(distanceFromThreat - idealDistance) * 0.45D;
        score += hasPeekLane ? 7.0D : (strict ? -4.0D : -1.0D);
        score += lateralOffset * 3.5D;
        score += allySpacing;
        score -= exposurePenalty * (strict ? 9.0D : 5.5D);

        return new CoverCandidate(candidate, score);
    }

    private static double idealCoverDistance(@Nullable StrategyType strategy, double currentDistance) {
        StrategyType resolved = strategy == null ? StrategyType.MID_RANGE : strategy;
        return switch (resolved) {
            case CLOSE_RANGE -> Math.min(Math.max(currentDistance, 8.0D), 14.0D);
            case LONG_RANGE -> Math.max(Math.min(currentDistance, 42.0D), 24.0D);
            case MID_RANGE -> Math.max(Math.min(currentDistance, 28.0D), 14.0D);
        };
    }

    private static boolean isCandidateStandable(AbstractUnit unit, BlockPos pos) {
        return TacticalHazardHelper.isSafeStandable(unit.level(), pos);
    }

    private static boolean isReachable(AbstractUnit unit, BlockPos candidate) {
        SemTickProfiler.recordCoverPathCheck();
        Path path = unit.getNavigation().createPath(candidate, 0);
        return path != null;
    }

    private static boolean isOccupiedByUnit(AbstractUnit seeker, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.35D, 0.25D, 0.35D);
        return !seeker.level().getEntitiesOfClass(AbstractUnit.class, box,
                entity -> entity != seeker && entity.isAlive()).isEmpty();
    }

    @Nullable
    private static BlockPos findHardBlockingCoverBlock(Level level, BlockPos candidate, Vec3 threatEyes, AbstractUnit unit) {
        Vec3 candidateEyes = Vec3.atBottomCenterOf(candidate).add(0.0D, unit.getEyeHeight(), 0.0D);
        Vec3 delta = candidateEyes.subtract(threatEyes);
        double distance = delta.length();
        if (distance < 0.001D) {
            return null;
        }

        Vec3 step = delta.normalize().scale(RAY_STEP);
        int steps = Math.max(1, (int) Math.ceil(distance / RAY_STEP));
        BlockPos lastPos = null;

        for (int i = 1; i <= steps; i++) {
            Vec3 sample = threatEyes.add(step.scale(i));
            BlockPos pos = BlockPos.containing(sample);
            if (pos.equals(lastPos)) {
                continue;
            }
            lastPos = pos;

            if (pos.equals(candidate)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || TacticalBlockClassifier.isSoftCover(state)) {
                continue;
            }
            if (TacticalBlockClassifier.isHardCover(state, level, pos)) {
                return pos;
            }
        }

        return null;
    }

    private static boolean hasPeekLane(Level level, AbstractUnit unit, BlockPos coverPos, LivingEntity threat) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos peek = coverPos.relative(direction);
            if (!isCandidateStandable(unit, peek) || isOccupiedByUnit(unit, peek)) continue;
            Vec3 peekEyes = Vec3.atBottomCenterOf(peek).add(0.0D, unit.getEyeHeight(), 0.0D);
            if (hasClearHardCoverAwareRay(level, peekEyes, threat.getEyePosition(), unit)) {
                return true;
            }
        }
        return false;
    }

    private static double exposurePenalty(Level level, BlockPos candidate, LivingEntity threat, AbstractUnit unit) {
        double exposedSamples = 0.0D;
        double totalSamples = 0.0D;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos sample = candidate.relative(direction);
            if (!isCandidateStandable(unit, sample)) continue;
            totalSamples += 1.0D;
            Vec3 sampleEyes = Vec3.atBottomCenterOf(sample).add(0.0D, unit.getEyeHeight(), 0.0D);
            if (hasClearHardCoverAwareRay(level, threat.getEyePosition(), sampleEyes, unit)) {
                exposedSamples += 1.0D;
            }
        }

        return totalSamples <= 0.0D ? 0.25D : exposedSamples / totalSamples;
    }

    private static double partialHardCoverStrength(Level level, BlockPos candidate, LivingEntity threat, AbstractUnit unit) {
        double blocked = 0.0D;
        double samples = 0.0D;
        Vec3 threatEyes = threat.getEyePosition();
        Vec3 candidateEyes = Vec3.atBottomCenterOf(candidate).add(0.0D, unit.getEyeHeight(), 0.0D);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = candidate.relative(direction);
            BlockState state = level.getBlockState(adjacent);
            if (!TacticalBlockClassifier.isHardCover(state, level, adjacent)) {
                continue;
            }

            samples += 1.0D;
            Vec3 sideSample = candidateEyes.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(0.45D));
            if (!hasClearHardCoverAwareRay(level, threatEyes, sideSample, unit)) {
                blocked += 1.0D;
            }
        }

        return samples <= 0.0D ? 0.0D : Math.min(0.65D, 0.25D + blocked / samples * 0.40D);
    }

    private static boolean hasClearHardCoverAwareRay(Level level, Vec3 from, Vec3 to, AbstractUnit unit) {
        HitResult vanillaHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, unit));
        if (vanillaHit.getType() == HitResult.Type.MISS) {
            return true;
        }

        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 0.001D) {
            return true;
        }

        Vec3 step = delta.normalize().scale(RAY_STEP);
        int steps = Math.max(1, (int) Math.ceil(distance / RAY_STEP));
        BlockPos lastPos = null;

        for (int i = 1; i <= steps; i++) {
            BlockPos pos = BlockPos.containing(from.add(step.scale(i)));
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || TacticalBlockClassifier.isSoftCover(state)) {
                continue;
            }
            if (TacticalBlockClassifier.isHardCover(state, level, pos)) {
                return false;
            }
        }

        return true;
    }

    private static double allySpacingScore(AbstractUnit unit, BlockPos candidate) {
        double nearbyRadius = unit.getStrategy() == StrategyType.CLOSE_RANGE ? 3.0D : 2.25D;
        AABB close = new AABB(candidate).inflate(1.35D, 1.0D, 1.35D);
        AABB nearby = new AABB(candidate).inflate(nearbyRadius, 1.0D, nearbyRadius);
        int closeAllies = unit.level().getEntitiesOfClass(AbstractUnit.class, close,
                entity -> entity != unit && entity.isAlive()).size();
        int nearbyAllies = unit.level().getEntitiesOfClass(AbstractUnit.class, nearby,
                entity -> entity != unit && entity.isAlive()).size();
        double penalty = closeAllies * (unit.getStrategy() == StrategyType.CLOSE_RANGE ? 7.0D : 5.0D);
        penalty += Math.max(0, nearbyAllies - closeAllies) * (unit.getStrategy() == StrategyType.CLOSE_RANGE ? 2.5D : 1.5D);
        return -penalty;
    }

    private static double lateralOffsetScore(BlockPos candidate, BlockPos origin, BlockPos threat) {
        Vec3 threatToOrigin = Vec3.atCenterOf(origin).subtract(Vec3.atCenterOf(threat));
        Vec3 threatToCandidate = Vec3.atCenterOf(candidate).subtract(Vec3.atCenterOf(threat));
        if (threatToOrigin.lengthSqr() < 0.001D || threatToCandidate.lengthSqr() < 0.001D) return 0.0D;
        Vec3 a = threatToOrigin.normalize();
        Vec3 b = threatToCandidate.normalize();
        double dot = Math.max(-1.0D, Math.min(1.0D, a.dot(b)));
        return Math.sqrt(Math.max(0.0D, 1.0D - dot * dot));
    }

    @Nullable
    private static BlockPos findFallbackEvasion(AbstractUnit unit, LivingEntity threat, int radius) {
        Level level = unit.level();
        BlockPos origin = unit.blockPosition();
        Vec3 awayFromThreat = unit.position().subtract(threat.position());
        if (awayFromThreat.lengthSqr() < 0.001D) {
            awayFromThreat = unit.getViewVector(1.0F).scale(-1.0D);
        }
        awayFromThreat = new Vec3(awayFromThreat.x, 0.0D, awayFromThreat.z).normalize();
        Vec3 lateral = new Vec3(-awayFromThreat.z, 0.0D, awayFromThreat.x);

        List<CoverCandidate> candidates = new ArrayList<>();

        for (int r = 2; r <= radius; r++) {
            for (int side = -2; side <= 2; side++) {
                Vec3 offset = awayFromThreat.scale(r).add(lateral.scale(side));
                BlockPos candidate = BlockPos.containing(unit.position().add(offset));
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos adjusted = candidate.offset(0, dy, 0);
                    if (!TacticalHazardHelper.isSafeStandable(level, adjusted) || isOccupiedByUnit(unit, adjusted)) {
                        continue;
                    }

                    double distanceGain = Math.sqrt(adjusted.distSqr(threat.blockPosition())) - unit.distanceTo(threat);
                    double originCost = Math.sqrt(adjusted.distSqr(origin));
                    double exposure = exposurePenalty(level, adjusted, threat, unit);
                    double score = distanceGain * 2.0D - originCost * 0.45D - exposure * 2.5D;

                    SemTickProfiler.recordCoverCandidateScored();
                    candidates.add(new CoverCandidate(adjusted, score));
                }
            }
        }

        CoverCandidate best = selectReachableCandidate(unit, candidates);
        return best == null ? null : best.pos();
    }

    @Nullable
    private static BlockPos findFirstReachableHardCover(AbstractUnit unit, LivingEntity threat, int searchRadius) {
        BlockPos origin = unit.blockPosition();
        Vec3 threatEyes = threat.getEyePosition();
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (!isCandidateStandable(unit, candidate)) continue;
                        if (findHardBlockingCoverBlock(unit.level(), candidate, threatEyes, unit) == null) continue;
                        if (!TacticalAiConfig.enableCoverPathValidation() || isReachable(unit, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private record CoverCandidate(BlockPos pos, double score) {}

    public record CoverSearchResult(@Nullable BlockPos position, boolean cover, boolean evasion,
                                    double score, String mode) {
        public static CoverSearchResult cover(BlockPos position, double score, String mode) {
            return new CoverSearchResult(position, true, false, score, mode);
        }

        public static CoverSearchResult evasion(BlockPos position, String mode) {
            return new CoverSearchResult(position, false, true, 0.0D, mode);
        }

        public static CoverSearchResult none() {
            return new CoverSearchResult(null, false, false, 0.0D, "none");
        }

        public boolean hasPosition() {
            return position != null;
        }

        public boolean isCover() {
            return cover && position != null;
        }

        public boolean isEvasion() {
            return evasion && position != null;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "CoverSearchResult[%s score=%.2f pos=%s]", mode, score, position);
        }
    }
}
