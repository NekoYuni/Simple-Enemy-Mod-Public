package net.nekoyuni.SimpleEnemyMod.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.nekoyuni.SimpleEnemyMod.commands.SemEventCommand;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {


        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("sem")
                .requires(source -> source.hasPermission(2));

        rootCommand.then(SemEventCommand.register());

        dispatcher.register(rootCommand);
    }
}

