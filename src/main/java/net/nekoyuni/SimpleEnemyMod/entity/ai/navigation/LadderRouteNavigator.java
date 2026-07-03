package net.nekoyuni.SimpleEnemyMod.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;

import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ladder traversal fallback for humanoid units.
 *
 * Vanilla GroundPathNavigation can handle stairs, but it often treats ladders as non-routable vertical geometry.
 * This helper detects vertical targets, finds a nearby ladder column, moves the unit to the ladder entry, performs
 * the climb manually, and then hands control back to ordinary navigation after reaching a valid exit landing.
 */
public final class LadderRouteNavigator {

    private static final int SEARCH_MARGIN = 24;
    private static final int MAX_SEARCH_SPAN = 96;
    private static final int LANDING_SEARCH_RADIUS = 3;
    private static final int FAILED_ROUTE_COOLDOWN_TICKS = 20 * 6;
    private static final int SELECTED_ROUTE_COOLDOWN_TICKS = 20 * 2;
    private static final int SUCCESS_ROUTE_COOLDOWN_TICKS = 20 * 4;
    private static final int ROUTE_TIMEOUT_TICKS = 20 * 25;
    private static final double MIN_VERTICAL_DELTA = 3.00D;
    private static final double MIN_FORCED_VERTICAL_DELTA = 4.25D;
    private static final double MAX_LADDER_HORIZONTAL_DISTANCE_SQR = 26.0D * 26.0D;
    private static final double MIN_LOGGED_NO_LADDER_VERTICAL_DELTA = 5.50D;
    private static final double MAX_LOGGED_NO_LADDER_HORIZONTAL_DISTANCE_SQR = 18.0D * 18.0D;
    private static final double MAX_ENTRY_DISTANCE_SQR = 18.0D * 18.0D;
    private static final double MAX_EXIT_TARGET_DISTANCE_SQR = 10.0D * 10.0D;
    private static final double MAX_ROUTE_SCORE = 620.0D;
    private static final double ENTRY_REACHED_DISTANCE_SQR = 2.75D;
    private static final double EXIT_REACHED_DISTANCE_SQR = 3.25D;
    private static final double LADDER_CENTERING_SPEED = 0.30D;
    private static final double LADDER_CLIMB_SPEED = 0.25D;
    private static final int EXIT_COMMIT_TICKS = 15;
    private static final double EXIT_COMMIT_SPEED = 0.30D;

    private static final Map<UUID, ActiveRoute> ACTIVE_ROUTES = new HashMap<>();
    private static final Map<UUID, Integer> FAILED_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> SELECTED_COOLDOWNS = new HashMap<>();

    private static int routesSelected;
    private static int routesFailed;
    private static int climbStarts;
    private static int climbEnds;
    private static int dismountStarts;
    private static int dismountSuccesses;
    private static int dismountFailures;
    private static int routeLockSkips;
    private static int cooldownSkips;
    private static int terrainNoiseSkips;
    private static int exitCommitStarts;
    private static int exitCommitEnds;

    public static void resetSessionCounters() {
        routesSelected = 0;
        routesFailed = 0;
        climbStarts = 0;
        climbEnds = 0;
        dismountStarts = 0;
        dismountSuccesses = 0;
        dismountFailures = 0;
        routeLockSkips = 0;
        cooldownSkips = 0;
        terrainNoiseSkips = 0;
        exitCommitStarts = 0;
        exitCommitEnds = 0;
        ACTIVE_ROUTES.clear();
        FAILED_COOLDOWNS.clear();
        SELECTED_COOLDOWNS.clear();
    }

    public static void appendSessionCounters(Map<String, Object> summary) {
        summary.put("ladderRoutesSelected", routesSelected);
        summary.put("ladderRoutesFailed", routesFailed);
        summary.put("ladderClimbStarts", climbStarts);
        summary.put("ladderClimbEnds", climbEnds);
        summary.put("ladderDismountStarts", dismountStarts);
        summary.put("ladderDismountSuccesses", dismountSuccesses);
        summary.put("ladderDismountFailures", dismountFailures);
        summary.put("ladderRouteLockSkips", routeLockSkips);
        summary.put("ladderRouteCooldownSkips", cooldownSkips);
        summary.put("ladderRouteTerrainNoiseSkips", terrainNoiseSkips);
        summary.put("ladderExitCommitStarts", exitCommitStarts);
        summary.put("ladderExitCommitEnds", exitCommitEnds);
        summary.put("ladderRouteSuccessRate", routesSelected <= 0 ? 0.0D : (double) dismountSuccesses / (double) routesSelected);
    }

    private LadderRouteNavigator() {
        // Utility class.
    }

    /**
     * Called from AbstractUnit every server tick. It only advances already selected ladder routes.
     * Starting a new ladder route remains the responsibility of TacticalNavigationHelper.moveTo(...),
     * so this fallback does not silently override commander, cover or attack-range movement goals.
     */
    public static void tickUnit(AbstractUnit unit) {
        if (!TacticalAiConfig.enableLadderFallback() || unit.level().isClientSide || !unit.isAlive()) {
            return;
        }

        tickCooldown(unit.getUUID());

        ActiveRoute active = ACTIVE_ROUTES.get(unit.getUUID());
        if (active != null && !active.tick(unit)) {
            active.cleanup(unit);
            ACTIVE_ROUTES.remove(unit.getUUID());
        }
    }

    /**
     * Attempts ladder-aware routing to a target. Returns true when ladder routing is active and the caller should not
     * issue a normal moveTo for this tick.
     */
    public static boolean moveTo(PathfinderMob mob, Vec3 target, double speed) {
        if (!TacticalAiConfig.enableLadderFallback() || !(mob instanceof AbstractUnit unit) || mob.level().isClientSide || !mob.isAlive()) {
            return false;
        }

        double verticalDelta = Math.abs(target.y - mob.getY());
        if (verticalDelta < MIN_VERTICAL_DELTA) {
            clear(unit);
            return false;
        }

        double horizontalDistanceSqr = horizontalDistanceSqr(mob.position(), target);
        if (verticalDelta < MIN_FORCED_VERTICAL_DELTA && horizontalDistanceSqr > MAX_LADDER_HORIZONTAL_DISTANCE_SQR) {
            // A small Y difference across a long firefight is usually terrain noise, not a ladder problem. Let vanilla
            // navigation/positioning handle it and avoid noisy NO_LADDER_COLUMN diagnostics.
            return false;
        }

        if (shouldHoldHighGround(unit, target)) {
            clear(unit);
            unit.getNavigation().stop();
            return true;
        }

        ActiveRoute active = ACTIVE_ROUTES.get(unit.getUUID());
        if (active != null) {
            if (active.acceptsTarget(target)) {
                active.target = target;
                active.speed = speed;
            }
            routeLockSkips++;
            if (!active.tick(unit)) {
                active.cleanup(unit);
                ACTIVE_ROUTES.remove(unit.getUUID());
                return false;
            }
            return true;
        }

        if (SELECTED_COOLDOWNS.getOrDefault(unit.getUUID(), 0) > 0 || FAILED_COOLDOWNS.getOrDefault(unit.getUUID(), 0) > 0) {
            cooldownSkips++;
            return false;
        }

        RouteSearchResult result = findRoute(unit, target, speed);
        if (result.route() == null) {
            FAILED_COOLDOWNS.put(unit.getUUID(), FAILED_ROUTE_COOLDOWN_TICKS);
            if (isLikelyTerrainNoiseFailure(result.reason(), verticalDelta, horizontalDistanceSqr)) {
                terrainNoiseSkips++;
                return false;
            }
            routesFailed++;
            String message = "reason=" + result.reason()
                    + " vertical=" + String.format(java.util.Locale.ROOT, "%.2f", verticalDelta)
                    + " horizontal=" + String.format(java.util.Locale.ROOT, "%.2f", Math.sqrt(horizontalDistanceSqr))
                    + " target=" + shortVec(target);
            return false;
        }

        ActiveRoute route = result.route();

        ACTIVE_ROUTES.put(unit.getUUID(), route);
        SELECTED_COOLDOWNS.put(unit.getUUID(), SELECTED_ROUTE_COOLDOWN_TICKS);
        routesSelected++;
        String selectedMessage = "entry=" + route.entryApproach + " ladder=" + route.entryLadder + " exit=" + route.exitLanding + " target=" + shortVec(target);
        route.tick(unit);
        return true;
    }

    public static void clear(AbstractUnit unit) {
        ActiveRoute active = ACTIVE_ROUTES.remove(unit.getUUID());
        if (active != null) {
            active.cleanup(unit);
        }
    }

    private static boolean isLikelyTerrainNoiseFailure(String reason, double verticalDelta, double horizontalDistanceSqr) {
        if (reason == null || !reason.startsWith("NO_LADDER_COLUMN")) {
            return false;
        }
        // Scenario logs showed many NO_LADDER_COLUMN diagnostics when units were merely fighting across uneven
        // ground/rooftop edges. Do not treat that as a ladder failure unless the vertical gap is large or close enough
        // that a ladder would plausibly be the intended route.
        return verticalDelta < MIN_LOGGED_NO_LADDER_VERTICAL_DELTA
                && horizontalDistanceSqr > MAX_LOGGED_NO_LADDER_HORIZONTAL_DISTANCE_SQR;
    }

    private static boolean shouldHoldHighGround(AbstractUnit unit, Vec3 target) {
        return false; // Scenario high-ground orders are intentionally left for later migration phases.
    }

    public static String debugState(AbstractUnit unit) {
        ActiveRoute active = ACTIVE_ROUTES.get(unit.getUUID());
        if (active != null) {
            return active.stage.name() + ":entry=" + active.entryApproach + ":exit=" + active.exitLanding;
        }
        int failedCooldown = FAILED_COOLDOWNS.getOrDefault(unit.getUUID(), 0);
        if (failedCooldown > 0) {
            return "FAILED_COOLDOWN:" + failedCooldown;
        }
        int selectedCooldown = SELECTED_COOLDOWNS.getOrDefault(unit.getUUID(), 0);
        return selectedCooldown > 0 ? "SELECTED_COOLDOWN:" + selectedCooldown : "IDLE";
    }

    private static void tickCooldown(UUID uuid) {
        tickCooldownMap(FAILED_COOLDOWNS, uuid);
        tickCooldownMap(SELECTED_COOLDOWNS, uuid);
    }

    private static void tickCooldownMap(Map<UUID, Integer> map, UUID uuid) {
        Integer cooldown = map.get(uuid);
        if (cooldown == null) {
            return;
        }
        if (cooldown <= 1) {
            map.remove(uuid);
        } else {
            map.put(uuid, cooldown - 1);
        }
    }

    private static RouteSearchResult findRoute(AbstractUnit unit, Vec3 target, double speed) {
        Level level = unit.level();
        BlockPos start = unit.blockPosition();
        BlockPos targetPos = BlockPos.containing(target);

        int minX = Math.min(start.getX(), targetPos.getX()) - SEARCH_MARGIN;
        int maxX = Math.max(start.getX(), targetPos.getX()) + SEARCH_MARGIN;
        int minZ = Math.min(start.getZ(), targetPos.getZ()) - SEARCH_MARGIN;
        int maxZ = Math.max(start.getZ(), targetPos.getZ()) + SEARCH_MARGIN;

        if (maxX - minX > MAX_SEARCH_SPAN || maxZ - minZ > MAX_SEARCH_SPAN) {
            minX = Math.min(start.getX(), targetPos.getX()) - MAX_SEARCH_SPAN / 2;
            maxX = Math.max(start.getX(), targetPos.getX()) + MAX_SEARCH_SPAN / 2;
            minZ = Math.min(start.getZ(), targetPos.getZ()) - MAX_SEARCH_SPAN / 2;
            maxZ = Math.max(start.getZ(), targetPos.getZ()) + MAX_SEARCH_SPAN / 2;
        }

        int minY = Math.min(start.getY(), targetPos.getY()) - 5;
        int maxY = Math.max(start.getY(), targetPos.getY()) + 5;
        boolean goingUp = target.y > unit.getY();

        ActiveRoute best = null;
        double bestScore = Double.MAX_VALUE;
        String rejectReason = "NO_LADDER_COLUMN";

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                LadderColumn column = scanColumn(level, x, z, minY, maxY);
                if (column == null) {
                    continue;
                }
                if (!column.canConnect(start.getY(), targetPos.getY(), goingUp)) {
                    rejectReason = "COLUMN_NOT_CONNECTED";
                    continue;
                }

                BlockPos ladderEntry = column.entryLadder(start.getY(), goingUp);
                BlockPos ladderExit = column.exitLadder(targetPos.getY(), goingUp);
                Direction facing = column.facing;

                BlockPos entryApproach = findEntryApproach(level, ladderEntry, facing, start);
                ExitLandingResult exitLandingResult = findExitLanding(level, ladderExit, facing, targetPos);
                BlockPos exitLanding = exitLandingResult.landing();
                if (entryApproach == null) {
                    rejectReason = "NO_ENTRY_APPROACH";
                    continue;
                }
                if (exitLanding == null) {
                    rejectReason = "NO_EXIT_LANDING " + exitLandingResult.debug();
                    continue;
                }

                double entryDistance = entryApproach.distSqr(start);
                double exitDistance = exitLanding.distSqr(targetPos);
                if (entryDistance > MAX_ENTRY_DISTANCE_SQR) {
                    rejectReason = "ENTRY_TOO_FAR";
                    continue;
                }
                if (exitDistance > MAX_EXIT_TARGET_DISTANCE_SQR) {
                    rejectReason = "EXIT_TOO_FAR";
                    continue;
                }

                double score = entryDistance + exitDistance * 2.0D;
                // Prefer ladders that actually sit between the current and target heights.
                score += Math.abs(column.minY - Math.min(start.getY(), targetPos.getY())) * 0.15D;
                score += Math.abs(column.maxY - Math.max(start.getY(), targetPos.getY())) * 0.15D;
                if (score < bestScore) {
                    bestScore = score;
                    best = new ActiveRoute(ladderEntry, ladderExit, entryApproach, exitLanding, facing, target, goingUp, speed);
                }
            }
        }

        if (best == null) {
            return new RouteSearchResult(null, rejectReason);
        }
        if (bestScore > MAX_ROUTE_SCORE) {
            return new RouteSearchResult(null, "ROUTE_SCORE_TOO_HIGH:" + String.format("%.1f", bestScore));
        }
        return new RouteSearchResult(best, "OK");
    }

    private static LadderColumn scanColumn(Level level, int x, int z, int minY, int maxY) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        Direction facing = null;

        for (int y = minY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LADDER) || !state.hasProperty(LadderBlock.FACING)) {
                continue;
            }
            Direction ladderFacing = state.getValue(LadderBlock.FACING);
            if (facing != null && facing != ladderFacing) {
                continue; // Treat mixed-facing ladder fragments as invalid for this simple route.
            }
            facing = ladderFacing;
            min = Math.min(min, y);
            max = Math.max(max, y);
        }

        if (facing == null || min == Integer.MAX_VALUE) {
            return null;
        }
        return new LadderColumn(x, z, min, max, facing);
    }

    private static BlockPos findEntryApproach(Level level, BlockPos ladderPos, Direction facing, BlockPos preferred) {
        BlockPos front = ladderPos.relative(facing);
        BlockPos[] priority = new BlockPos[] {
                front,
                front.below(),
                ladderPos,
                ladderPos.below(),
                front.relative(facing.getClockWise()),
                front.relative(facing.getCounterClockWise()),
                ladderPos.relative(facing.getClockWise()),
                ladderPos.relative(facing.getCounterClockWise())
        };

        BlockPos best = bestStandable(level, preferred, priority);
        if (best != null) {
            return best;
        }

        // Some valid ladder setups have the first ladder block one block above the floor. Search around the base.
        for (int dy = -2; dy <= 2; dy++) {
            BlockPos around = ladderPos.offset(0, dy, 0);
            BlockPos candidate = bestStandable(level, preferred,
                    around.relative(facing),
                    around,
                    around.relative(facing.getClockWise()),
                    around.relative(facing.getCounterClockWise()));
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static ExitLandingResult findExitLanding(Level level, BlockPos ladderPos, Direction facing, BlockPos preferred) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int checked = 0;
        int standable = 0;
        StringBuilder samples = new StringBuilder();

        // Scan a small area near the top/bottom of the ladder. This is more robust than fixed candidates because
        // player-built ladders often exit sideways onto a platform rather than directly forward.
        for (int dy = -1; dy <= 2; dy++) {
            int y = preferred.getY() + dy;
            for (int dx = -LANDING_SEARCH_RADIUS; dx <= LANDING_SEARCH_RADIUS; dx++) {
                for (int dz = -LANDING_SEARCH_RADIUS; dz <= LANDING_SEARCH_RADIUS; dz++) {
                    BlockPos candidate = new BlockPos(ladderPos.getX() + dx, y, ladderPos.getZ() + dz);
                    checked++;
                    if (!isStandable(level, candidate)) {
                        if (samples.length() < 240) {
                            samples.append(candidate).append(":" ).append(blockSummary(level, candidate)).append(";");
                        }
                        continue;
                    }
                    standable++;
                    double score = candidate.distSqr(preferred);
                    if (candidate.equals(ladderPos.relative(facing)) || candidate.equals(ladderPos.relative(facing).above())) {
                        score -= 3.0D;
                    }
                    if (hasClearLadderAccess(candidate, ladderPos)) {
                        score -= 1.0D;
                    }
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        if (best == null) {
            String debug = "ladder=" + ladderPos
                    + " facing=" + facing
                    + " preferred=" + preferred
                    + " checked=" + checked
                    + " standable=" + standable
                    + " samples=" + samples;
            return new ExitLandingResult(null, debug);
        }
        return new ExitLandingResult(best, "OK score=" + String.format("%.1f", bestScore));
    }

    private static BlockPos bestStandable(Level level, BlockPos preferred, BlockPos... candidates) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (!isStandable(level, candidate) && !isLadderCell(level, candidate)) {
                continue;
            }
            double score = candidate.distSqr(preferred);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasClearLadderAccess(BlockPos landing, BlockPos ladderPos) {
        return Math.abs(landing.getX() - ladderPos.getX()) <= 1 && Math.abs(landing.getZ() - ladderPos.getZ()) <= 1;
    }

    private static boolean isLadderCell(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.LADDER);
    }

    private static boolean isStandable(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return !feet.isSolid() && !head.isSolid() && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static String blockSummary(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return "feet=" + feet.getBlock().getName().getString()
                + ",head=" + head.getBlock().getName().getString()
                + ",floor=" + floor.getBlock().getName().getString()
                + ",floorSturdy=" + floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static String shortVec(Vec3 vec) {
        return String.format("%.1f,%.1f,%.1f", vec.x, vec.y, vec.z);
    }

    private enum Stage {
        MOVING_TO_ENTRY,
        CLIMBING,
        EXIT_COMMIT,
        TOP_DISMOUNT,
        EXITING
    }

    private static void failActiveRoute(AbstractUnit unit, String message) {
        routesFailed++;
        FAILED_COOLDOWNS.put(unit.getUUID(), FAILED_ROUTE_COOLDOWN_TICKS);
    }

    private static final class ActiveRoute {
        private final BlockPos entryLadder;
        private final BlockPos exitLadder;
        private final BlockPos entryApproach;
        private final BlockPos exitLanding;
        private final Direction facing;
        private final boolean goingUp;
        private Vec3 target;
        private double speed;
        private Stage stage = Stage.MOVING_TO_ENTRY;
        private int ageTicks = 0;
        private int lastMoveCommandTick = 0;
        private int dismountTicks = 0;
        private int exitCommitTicks = 0;
        private long lastAdvancedGameTick = -1L;
        private boolean aborted = false;

        private ActiveRoute(BlockPos entryLadder, BlockPos exitLadder, BlockPos entryApproach, BlockPos exitLanding,
                            Direction facing, Vec3 target, boolean goingUp, double speed) {
            this.entryLadder = entryLadder;
            this.exitLadder = exitLadder;
            this.entryApproach = entryApproach;
            this.exitLanding = exitLanding;
            this.facing = facing;
            this.target = target;
            this.goingUp = goingUp;
            this.speed = speed;
        }

        private boolean tick(AbstractUnit unit) {
            long gameTick = unit.level().getGameTime();
            if (this.lastAdvancedGameTick == gameTick) {
                return true;
            }
            this.lastAdvancedGameTick = gameTick;

            if (++this.ageTicks > ROUTE_TIMEOUT_TICKS || !unit.isAlive()) {
                FAILED_COOLDOWNS.put(unit.getUUID(), FAILED_ROUTE_COOLDOWN_TICKS);
                return false;
            }

            switch (this.stage) {
                case MOVING_TO_ENTRY -> tickMoveToEntry(unit);
                case CLIMBING -> tickClimbing(unit);
                case EXIT_COMMIT -> tickExitCommit(unit);
                case TOP_DISMOUNT -> tickTopDismount(unit);
                case EXITING -> tickExit(unit);
            }
            return !this.aborted;
        }

        private void tickMoveToEntry(AbstractUnit unit) {
            Vec3 entry = Vec3.atBottomCenterOf(this.entryApproach);
            double horizontalToLadder = unit.position().multiply(1.0D, 0.0D, 1.0D)
                    .distanceToSqr(Vec3.atBottomCenterOf(this.entryLadder).multiply(1.0D, 0.0D, 1.0D));
            if (unit.position().distanceToSqr(entry) <= ENTRY_REACHED_DISTANCE_SQR || horizontalToLadder <= 2.25D) {
                this.stage = Stage.CLIMBING;
                unit.getNavigation().stop();
                climbStarts++;
                String message = "ladder=" + this.entryLadder + " exit=" + this.exitLadder + " approach=" + this.entryApproach;
                return;
            }

            if (++this.lastMoveCommandTick >= 6 || unit.getNavigation().isDone()) {
                this.lastMoveCommandTick = 0;
                unit.getNavigation().moveTo(entry.x, entry.y, entry.z, this.speed);
            }
        }

        private void tickClimbing(AbstractUnit unit) {
            unit.getNavigation().stop();

            BlockPos expectedLadder = this.currentExpectedLadder(unit);
            if (!this.isExpectedLadderPresent(unit.level(), expectedLadder)) {
                String message = "reason=LADDER_BLOCK_MISSING stage=CLIMBING expected=" + expectedLadder
                        + " entityBlock=" + unit.blockPosition()
                        + " entry=" + this.entryLadder
                        + " exit=" + this.exitLadder
                        + " facing=" + this.facing;
                failActiveRoute(unit, message);
                this.aborted = true;
                return;
            }

            double centerX = this.entryLadder.getX() + 0.5D;
            double centerZ = this.entryLadder.getZ() + 0.5D;
            double dx = centerX - unit.getX();
            double dz = centerZ - unit.getZ();
            double vertical = this.goingUp ? LADDER_CLIMB_SPEED : -LADDER_CLIMB_SPEED;

            unit.setDeltaMovement(
                    clamp(dx, -LADDER_CENTERING_SPEED, LADDER_CENTERING_SPEED),
                    vertical,
                    clamp(dz, -LADDER_CENTERING_SPEED, LADDER_CENTERING_SPEED)
            );
            unit.hurtMarked = true;
            unit.hasImpulse = true;
            unit.fallDistance = 0.0F;

            boolean reachedVertical = this.goingUp
                    ? unit.getY() >= this.exitLanding.getY() - 0.45D
                    : unit.getY() <= this.exitLanding.getY() + 0.45D;

            if (reachedVertical) {
                this.stage = Stage.EXIT_COMMIT;
                this.lastMoveCommandTick = 6;
                this.dismountTicks = 0;
                this.exitCommitTicks = 0;
                climbEnds++;
                String message = "exitLanding=" + this.exitLanding + " ladder=" + this.exitLadder;

                exitCommitStarts++;
                String commitMessage = "exitLanding=" + this.exitLanding + " ladder=" + this.exitLadder + " ticks=" + EXIT_COMMIT_TICKS;
            }
        }


        private void tickExitCommit(AbstractUnit unit) {
            unit.getNavigation().stop();

            if (this.dismountTicks == 0) {
                dismountStarts++;
                String message = "exitLanding=" + this.exitLanding + " ladder=" + this.exitLadder + " commitTicks=" + EXIT_COMMIT_TICKS;
            }
            this.dismountTicks++;
            this.exitCommitTicks++;

            Vec3 exit = Vec3.atBottomCenterOf(this.exitLanding);
            forceTowardExit(unit, exit, EXIT_COMMIT_SPEED);

            if (this.exitCommitTicks < EXIT_COMMIT_TICKS) {
                return;
            }

            exitCommitEnds++;
            String commitMessage = "exitLanding=" + this.exitLanding + " target=" + shortVec(this.target) + " ticks=" + this.exitCommitTicks;

            Vec3 toExit = exit.subtract(unit.position());
            double horizontalDistanceSqr = toExit.x * toExit.x + toExit.z * toExit.z;
            boolean atLanding = unit.blockPosition().distSqr(this.exitLanding) <= 2.0D
                    || horizontalDistanceSqr <= EXIT_REACHED_DISTANCE_SQR;
            boolean safelyStanding = isStandable(unit.level(), unit.blockPosition())
                    || isStandable(unit.level(), unit.blockPosition().above());

            if (atLanding && safelyStanding) {
                finishDismount(unit);
                return;
            }

            // Commit gave the mob a deterministic step-off impulse. If it still is not safely on the landing,
            // fall back to the older dismount retry loop instead of immediately failing the route.
            this.stage = Stage.TOP_DISMOUNT;
            this.lastMoveCommandTick = 6;
        }

        private void tickTopDismount(AbstractUnit unit) {
            unit.getNavigation().stop();

            if (this.dismountTicks == 0) {
                dismountStarts++;
                String message = "exitLanding=" + this.exitLanding + " ladder=" + this.exitLadder;
            }
            this.dismountTicks++;

            Vec3 exit = Vec3.atBottomCenterOf(this.exitLanding);
            Vec3 toExit = exit.subtract(unit.position());
            double horizontalDistanceSqr = toExit.x * toExit.x + toExit.z * toExit.z;

            // The top of a ladder is the hard part for humanoid mobs: vanilla navigation often reaches the ladder
            // top but never performs the small horizontal step onto the floor. Keep applying a deterministic step-off
            // impulse while this active route owns movement.
            forceTowardExit(unit, exit, 0.26D);

            boolean atLanding = unit.blockPosition().distSqr(this.exitLanding) <= 2.0D || unit.position().distanceToSqr(exit) <= EXIT_REACHED_DISTANCE_SQR;
            boolean safelyStanding = isStandable(unit.level(), unit.blockPosition()) || isStandable(unit.level(), unit.blockPosition().above());
            if (atLanding && safelyStanding) {
                finishDismount(unit);
                return;
            }

            if (this.dismountTicks > 40) {
                dismountFailures++;
                String message = "exitLanding=" + this.exitLanding + " target=" + shortVec(this.target);
                FAILED_COOLDOWNS.put(unit.getUUID(), FAILED_ROUTE_COOLDOWN_TICKS);
                ACTIVE_ROUTES.remove(unit.getUUID());
            }
        }


        private void forceTowardExit(AbstractUnit unit, Vec3 exit, double horizontalSpeed) {
            Vec3 toExit = exit.subtract(unit.position());
            double horizontalDistanceSqr = toExit.x * toExit.x + toExit.z * toExit.z;
            double len = Math.sqrt(Math.max(0.0001D, horizontalDistanceSqr));
            double pushX = clamp(toExit.x / len * horizontalSpeed, -horizontalSpeed, horizontalSpeed);
            double pushZ = clamp(toExit.z / len * horizontalSpeed, -horizontalSpeed, horizontalSpeed);
            double pushY = this.goingUp ? 0.08D : 0.0D;
            unit.setDeltaMovement(pushX, pushY, pushZ);
            unit.hurtMarked = true;
            unit.hasImpulse = true;
            unit.fallDistance = 0.0F;
            unit.getMoveControl().setWantedPosition(exit.x, exit.y, exit.z, Math.max(1.0D, this.speed));
        }

        private void finishDismount(AbstractUnit unit) {
            this.stage = Stage.EXITING;
            this.lastMoveCommandTick = 6;
            dismountSuccesses++;
            String message = "exitLanding=" + this.exitLanding + " target=" + shortVec(this.target);
        }

        private BlockPos currentExpectedLadder(AbstractUnit unit) {
            int y = Math.max(Math.min(BlockPos.containing(unit.position()).getY(), this.exitLadder.getY()), this.entryLadder.getY());
            if (!this.goingUp) {
                y = Math.max(Math.min(BlockPos.containing(unit.position()).getY(), this.entryLadder.getY()), this.exitLadder.getY());
            }
            return new BlockPos(this.entryLadder.getX(), y, this.entryLadder.getZ());
        }

        private boolean isExpectedLadderPresent(Level level, BlockPos pos) {
            return isSameFacingLadder(level, pos)
                    || isSameFacingLadder(level, pos.above())
                    || isSameFacingLadder(level, pos.below());
        }

        private boolean isSameFacingLadder(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.LADDER)
                    && state.hasProperty(LadderBlock.FACING)
                    && state.getValue(LadderBlock.FACING) == this.facing;
        }

        private boolean acceptsTarget(Vec3 updatedTarget) {
            if (updatedTarget == null) {
                return false;
            }
            return Math.abs(updatedTarget.y - this.target.y) <= 4.0D
                    && updatedTarget.distanceToSqr(this.target) <= 18.0D * 18.0D;
        }

        private void tickExit(AbstractUnit unit) {
            Vec3 exit = Vec3.atBottomCenterOf(this.exitLanding);
            if (unit.position().distanceToSqr(exit) <= EXIT_REACHED_DISTANCE_SQR) {
                unit.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, this.speed);
                String message = "target=" + shortVec(this.target);
                SELECTED_COOLDOWNS.put(unit.getUUID(), SUCCESS_ROUTE_COOLDOWN_TICKS);
                ACTIVE_ROUTES.remove(unit.getUUID());
                return;
            }

            if (++this.lastMoveCommandTick >= 6 || unit.getNavigation().isDone()) {
                this.lastMoveCommandTick = 0;
                unit.getNavigation().moveTo(exit.x, exit.y, exit.z, this.speed);
            }
        }

        private void cleanup(AbstractUnit unit) {
            unit.fallDistance = 0.0F;
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private record RouteSearchResult(ActiveRoute route, String reason) {
    }

    private record ExitLandingResult(BlockPos landing, String debug) {
    }

    private record LadderColumn(int x, int z, int minY, int maxY, Direction facing) {
        private boolean canConnect(int startY, int targetY, boolean goingUp) {
            int low = Math.min(startY, targetY);
            int high = Math.max(startY, targetY);
            return this.minY <= low + 4 && this.maxY >= high - 4;
        }

        private BlockPos entryLadder(int startY, boolean goingUp) {
            int y = goingUp ? Math.max(this.minY, Math.min(this.maxY, startY + 1)) : Math.min(this.maxY, Math.max(this.minY, startY - 1));
            return new BlockPos(this.x, y, this.z);
        }

        private BlockPos exitLadder(int targetY, boolean goingUp) {
            int y = goingUp ? Math.min(this.maxY, Math.max(this.minY, targetY - 1)) : Math.max(this.minY, Math.min(this.maxY, targetY + 1));
            return new BlockPos(this.x, y, this.z);
        }
    }
}
