package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hello09x.onesync.command.impl.TeleportCommand;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.teleport.PlayerManager;
import org.jetbrains.annotations.NotNull;

import static io.github.hello09x.bedrock.command.Commands.command;
import static io.github.hello09x.bedrock.command.Commands.literal;

public class TeleportCommandRegistry {

    public static @NotNull Argument<String> globalPlayer(@NotNull String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            var input = info.currentInput();
            var name = PlayerManager.instance
                    .getPlayers()
                    .stream()
                    .filter(n -> n.equals(input))
                    .findFirst()
                    .orElse(null);

            if (name == null) {
                throw CustomArgument.CustomArgumentException.fromString("该玩家不在线");
            }

            if (name.equals(info.sender().getName())) {
                throw CustomArgument.CustomArgumentException.fromString("该命令不能对自己使用");
            }

            return name;
        }).replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
            var senderName = info.sender().getName();
            var input = info.currentArg().toLowerCase();

            return PlayerManager.instance
                    .getPlayers()
                    .stream()
                    .filter(n -> n.toLowerCase().contains(input) && !n.equals(senderName))
                    .toList();
        }));
    }


    public static void register() {
        var commands = OneSyncConfig.instance.getTeleport().getCommands();
        command(commands.getOrDefault("stpa", "stpa"))
                .withAliases("stpa")
                .withPermission(Permissions.TPA)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpa)
                .override();

        command(commands.getOrDefault("stpahere", "stpahere"))
                .withAliases("stpahere")
                .withPermission(Permissions.TPAHERE)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpahere)
                .override();

        command(commands.getOrDefault("stpaccept", "stpaccept"))
                .withAliases("stpaccept")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpaccept)
                .override();

        command(commands.getOrDefault("stpdeny", "stpdeny"))
                .withAliases("stpdeny")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpdeny)
                .override();

        command(commands.getOrDefault("stpcancel", "stpcancel"))
                .withAliases("stpcancel")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpcancel)
                .override();

        command(commands.getOrDefault("stp", "stp"))
                .withAliases("stp")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tp)
                .override();

        command(commands.getOrDefault("stphere", "stphere"))
                .withAliases("stphere")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tphere)
                .override();

        command(commands.getOrDefault("stpahereall", "stpahereall"))
                .withAliases("stpahereall")
                .withPermission(CommandPermission.OP)
                .withArguments(literal("confirm"))
                .executesPlayer(TeleportCommand.instance::tpahereall)
                .override();

        command(commands.getOrDefault("stphereall", "stphereall"))
                .withAliases("stphereall")
                .withPermission(CommandPermission.OP)
                .withArguments(literal("confirm"))
                .executesPlayer(TeleportCommand.instance::tphereall)
                .override();

    }
}
