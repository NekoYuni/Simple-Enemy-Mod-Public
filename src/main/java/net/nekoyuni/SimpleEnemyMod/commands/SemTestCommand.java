package net.nekoyuni.SimpleEnemyMod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioDefinition;
import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;
import net.nekoyuni.SimpleEnemyMod.scenario.SemTestSuite;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Developer-only commands for repeatable in-game AI test scenarios.
 */
public final class SemTestCommand {
    private static final int DEFAULT_COUNT = 5;
    private static final int DEFAULT_CLEAR_RADIUS = 128;
    private static final int SPAWN_SPREAD_RADIUS = 4;
    private static final int DEFAULT_LOOP_DURATION_TICKS = 20 * 60;

    private SemTestCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("test")
                .then(Commands.literal("init")
                        .executes(SemTestCommand::initTestWorld))
                .then(Commands.literal("list")
                        .executes(SemTestCommand::listScenarios))
                .then(Commands.literal("loop_all")
                        .executes(context -> startLoop(context, null, DEFAULT_LOOP_DURATION_TICKS))
                        .then(Commands.argument("durationTicks", IntegerArgumentType.integer(20, 20 * 60 * 10))
                                .executes(context -> startLoop(context, null, IntegerArgumentType.getInteger(context, "durationTicks")))))
                .then(Commands.literal("ts")
                        .then(Commands.argument("suite", StringArgumentType.word())
                                .suggests(SemTestCommand::suggestSuites)
                                .executes(context -> startTestSuite(context, DEFAULT_LOOP_DURATION_TICKS))
                                .then(Commands.argument("durationTicks", IntegerArgumentType.integer(20, 20 * 60 * 10))
                                        .executes(context -> startTestSuite(context, IntegerArgumentType.getInteger(context, "durationTicks"))))))
                .then(Commands.literal("loop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SemTestCommand::suggestScenarios)
                                .executes(context -> startLoop(context, StringArgumentType.getString(context, "name"), DEFAULT_LOOP_DURATION_TICKS))
                                .then(Commands.argument("durationTicks", IntegerArgumentType.integer(20, 20 * 60 * 10))
                                        .executes(context -> startLoop(context, StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "durationTicks"))))))
                .then(Commands.literal("stop_loop")
                        .executes(SemTestCommand::stopLoop))
                .then(Commands.literal("stop")
                        .executes(SemTestCommand::stopScenario))
                .then(Commands.literal("cleanup")
                        .executes(SemTestCommand::cleanupScenario))
                .then(Commands.literal("force_engage")
                        .executes(SemTestCommand::forceEngage))
                .then(Commands.literal("status")
                        .executes(SemTestCommand::status))
                .then(Commands.literal("spawn_us")
                        .executes(ctx -> spawn(ctx.getSource(), ModEntities.USUNIT.get(), DEFAULT_COUNT, "US units"))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> spawn(ctx.getSource(), ModEntities.USUNIT.get(), IntegerArgumentType.getInteger(ctx, "count"), "US units"))))
                .then(Commands.literal("spawn_ru")
                        .executes(ctx -> spawn(ctx.getSource(), ModEntities.RUUNIT.get(), DEFAULT_COUNT, "RU units"))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> spawn(ctx.getSource(), ModEntities.RUUNIT.get(), IntegerArgumentType.getInteger(ctx, "count"), "RU units"))))
                .then(Commands.literal("spawn_pmc")
                        .executes(ctx -> spawn(ctx.getSource(), ModEntities.PMCUNIT.get(), DEFAULT_COUNT, "PMC units"))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> spawn(ctx.getSource(), ModEntities.PMCUNIT.get(), IntegerArgumentType.getInteger(ctx, "count"), "PMC units"))))
                .then(Commands.literal("clear_units")
                        .executes(ctx -> clear(ctx.getSource(), DEFAULT_CLEAR_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
                                .executes(ctx -> clear(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> clear(ctx.getSource(), DEFAULT_CLEAR_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
                                .executes(ctx -> clear(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(SemTestCommand::suggestScenarios)
                        .executes(context -> startScenario(context, null))
                        .then(Commands.argument("durationTicks", IntegerArgumentType.integer(20, 20 * 60 * 10))
                                .executes(context -> startScenario(context, IntegerArgumentType.getInteger(context, "durationTicks")))));
    }

    private static int initTestWorld(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        MinecraftServer server = source.getServer();

        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setDayTime(6000L);
        level.setWeatherParameters(6000, 0, false, false);
        int removedEntities = SemScenarioRuntimeManager.get().clearNonPlayerEntities(level);

        source.sendSuccess(() -> Component.literal("§a[SEM] Test world initialized: daylightCycle=false, weatherCycle=false, mobGriefing=false, doMobSpawning=false, time=day, weather=clear, removedNonPlayerEntities=" + removedEntities + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestScenarios(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(SemScenarioDefinition.values()).map(SemScenarioDefinition::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestSuites(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(SemTestSuite.values()).map(SemTestSuite::id), builder);
    }

    private static int startScenario(CommandContext<CommandSourceStack> context, Integer durationTicks) {
        String scenarioId = StringArgumentType.getString(context, "name");
        SemScenarioDefinition scenario = SemScenarioDefinition.byId(scenarioId);
        CommandSourceStack source = context.getSource();
        if (scenario == null) {
            source.sendFailure(Component.literal("§c[SEM] Unknown scenario: " + scenarioId));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        boolean started = SemScenarioRuntimeManager.get().start(level, center, scenario, durationTicks);
        if (!started) {
            source.sendFailure(Component.literal("§c[SEM] A test scenario is already running or the logger could not be created. Use /sem test stop first."));
            return 0;
        }

        Path logFile = SemScenarioRuntimeManager.get().getCurrentLogFile();
        source.sendSuccess(() -> Component.literal("§a[SEM] Test scenario started: " + scenario.id()
                + " session=" + SemScenarioRuntimeManager.get().getSessionId()
                + " log=" + (logFile != null ? logFile.toString() : "unknown")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int startTestSuite(CommandContext<CommandSourceStack> context, int durationTicks) {
        String suiteId = StringArgumentType.getString(context, "suite");
        SemTestSuite suite = SemTestSuite.byId(suiteId);
        CommandSourceStack source = context.getSource();
        if (suite == null) {
            source.sendFailure(Component.literal("§c[SEM] Unknown test suite: " + suiteId + ". Available: " + SemTestSuite.ids()));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        boolean started = SemScenarioRuntimeManager.get().startSuite(level, center, durationTicks, suite, source);
        if (!started) {
            source.sendFailure(Component.literal("§c[SEM] A test scenario is already running. Use /sem test stop first."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§a[SEM] One-shot test suite started: " + suite.id()
                + " scenarios=" + suite.scenarios().size()
                + " durationTicks=" + durationTicks
                + " directory=" + suite.directoryName()
                + ". Use /sem test stop_loop to stop."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int startLoop(CommandContext<CommandSourceStack> context, String scenarioId, int durationTicks) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        SemScenarioDefinition scenario = null;
        if (scenarioId != null) {
            scenario = SemScenarioDefinition.byId(scenarioId);
            if (scenario == null) {
                source.sendFailure(Component.literal("§c[SEM] Unknown scenario: " + scenarioId));
                return 0;
            }
        }

        boolean started = SemScenarioRuntimeManager.get().startLoop(level, center, durationTicks, scenario, source);
        if (!started) {
            source.sendFailure(Component.literal("§c[SEM] A test scenario is already running. Use /sem test stop first."));
            return 0;
        }
        if (scenario != null) {
            String mode = scenario.id();
            source.sendSuccess(() -> Component.literal("§a[SEM] Test scenario loop started for " + mode + ". Each run lasts " + durationTicks + " ticks. Use /sem test stop_loop to stop."), true);
        } else {
            source.sendSuccess(() -> Component.literal("§a[SEM] One-shot scenario suite started. Each scenario will run once for up to " + durationTicks + " ticks. Use /sem test stop_loop to stop."), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int stopLoop(CommandContext<CommandSourceStack> context) {
        SemScenarioRuntimeManager.get().stopLoop("manual_loop_stop");
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Test scenario loop stopped."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopScenario(CommandContext<CommandSourceStack> context) {
        if (!SemScenarioRuntimeManager.get().isRunning()) {
            context.getSource().sendFailure(Component.literal("§c[SEM] No test scenario is running."));
            return 0;
        }
        SemScenarioRuntimeManager.get().stop("manual_stop");
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Test scenario stopped and cleaned up."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int cleanupScenario(CommandContext<CommandSourceStack> context) {
        int removed;
        if (SemScenarioRuntimeManager.get().isRunning()) {
            removed = SemScenarioRuntimeManager.get().cleanup(true);
        } else {
            removed = SemScenarioRuntimeManager.get().clearNonPlayerEntities(context.getSource().getLevel());
        }
        int finalRemoved = removed;
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Test cleanup complete. removedEntities=" + finalRemoved), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceEngage(CommandContext<CommandSourceStack> context) {
        int assigned = SemScenarioRuntimeManager.get().forceEngagement();
        if (assigned <= 0) {
            context.getSource().sendFailure(Component.literal("§c[SEM] No active scenario targets were assigned."));
            return 0;
        }
        int result = assigned;
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Forced scenario engagement. assignedTargets=" + result), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        boolean running = SemScenarioRuntimeManager.get().isRunning();
        Path logFile = SemScenarioRuntimeManager.get().getCurrentLogFile();
        long now = context.getSource().getLevel().getGameTime();
        long elapsed = running ? Math.max(0L, now - SemScenarioRuntimeManager.get().getStartTick()) : 0L;
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Test scenario running=" + running
                + " state=" + SemScenarioRuntimeManager.get().getState()
                + " active=" + (SemScenarioRuntimeManager.get().getActiveScenario() != null ? SemScenarioRuntimeManager.get().getActiveScenario().id() : "none")
                + " loop=" + SemScenarioRuntimeManager.get().isLoopMode()
                + " elapsed=" + elapsed + "/" + SemScenarioRuntimeManager.get().getDurationTicks()
                + " session=" + SemScenarioRuntimeManager.get().getSessionId()
                + " log=" + (logFile != null ? logFile.toString() : "none")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int listScenarios(CommandContext<CommandSourceStack> context) {
        String scenarios = String.join(", ", Arrays.stream(SemScenarioDefinition.values()).map(SemScenarioDefinition::id).toList());
        String suites = SemTestSuite.ids();
        context.getSource().sendSuccess(() -> Component.literal("§a[SEM] Test scenarios: §r" + scenarios + "\n§a[SEM] Test suites: §r" + suites), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int spawn(CommandSourceStack source, EntityType<? extends AbstractUnit> type, int count, String label) {
        ServerLevel level = source.getLevel();
        Vec3 center = source.getPosition();
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            AbstractUnit unit = type.create(level);
            if (unit == null) {
                continue;
            }

            double angle = (Math.PI * 2.0D * i) / Math.max(1, count);
            double distance = count == 1 ? 0.0D : 1.5D + (i % SPAWN_SPREAD_RADIUS);
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = center.y;

            unit.moveTo(x, y, z, Mth.wrapDegrees(source.getRotation().y + 180.0F), 0.0F);
            unit.finalizeSpawn(level, level.getCurrentDifficultyAt(unit.blockPosition()), MobSpawnType.COMMAND, (SpawnGroupData) null, null);
            unit.setPersistenceRequired();
            level.addFreshEntity(unit);
            spawned++;
        }

        int result = spawned;
        source.sendSuccess(() -> Component.literal("Spawned " + result + " " + label + "."), true);
        return spawned;
    }

    private static int clear(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        Vec3 center = source.getPosition();
        AABB box = AABB.ofSize(center, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        int removed = 0;

        for (AbstractUnit unit : level.getEntitiesOfClass(AbstractUnit.class, box, unit -> unit.isAlive() && !unit.isRemoved())) {
            unit.discard();
            removed++;
        }

        int result = removed;
        source.sendSuccess(() -> Component.literal("Removed " + result + " SEM units within " + radius + " blocks."), true);
        return removed;
    }
}
