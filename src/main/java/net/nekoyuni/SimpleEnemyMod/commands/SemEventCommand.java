package net.nekoyuni.SimpleEnemyMod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nekoyuni.SimpleEnemyMod.procedural.events.DynamicEventManager;
import net.nekoyuni.SimpleEnemyMod.world.data.EventProbabilityData;

import java.util.concurrent.CompletableFuture;

public class SemEventCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("event")
                .then(Commands.argument("eventId", StringArgumentType.word())
                        .suggests(SemEventCommand::suggestEvents)

                        .then(Commands.literal("spawn")
                                .executes(SemEventCommand::forceSpawn) // Referencia al método
                        )

                        .then(Commands.literal("active")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(SemEventCommand::toggleEvent) // Referencia al método
                                )
                        )
                );
    }


    private static CompletableFuture<Suggestions> suggestEvents(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {

        return SharedSuggestionProvider.suggest(DynamicEventManager.getEventIds(), builder);
    }

    private static int forceSpawn(CommandContext<CommandSourceStack> context) {

        String eventId = StringArgumentType.getString(context, "eventId");
        ServerPlayer player = context.getSource().getPlayer();

        if (player == null) return 0;

        boolean success = DynamicEventManager.forceEvent(player.serverLevel(), player, eventId);

        if (success) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§a[SEM] §rEvent Forced: " + eventId), true);

            return 1;

        } else {
            context.getSource().sendFailure(Component.literal("§a[SEM] §cFailed to Force Event§r: " + eventId));

            return 0;
        }
    }

    private static int toggleEvent(CommandContext<CommandSourceStack> context) {

        String eventId = StringArgumentType.getString(context, "eventId");
        boolean active = BoolArgumentType.getBool(context, "value");
        ServerPlayer player = context.getSource().getPlayer();

        if (player == null) return 0;

        EventProbabilityData.get(player.serverLevel()).setEventActive(eventId, active);
        String state = active ? "§aEnabled" : "§cDisabled";

        context.getSource().sendSuccess(
                () -> Component.literal("§a[SEM] §rEvent " + eventId + ": " + state), true);

        return 1;
    }
}