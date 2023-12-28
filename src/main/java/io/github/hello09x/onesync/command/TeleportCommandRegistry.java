package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hello09x.onesync.command.impl.TeleportCommand;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.PlayerManager;
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

        var alias = OneSyncConfig.instance.getTeleport().getCommands();

        command("stpa")
                .withAliases(alias.getOrDefault("stpa", "stpa"))
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpa)
                .override();

        command("stpahere")
                .withAliases(alias.getOrDefault("stpahere", "stpahere"))
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpahere)
                .override();

        command("stpaccept")
                .withAliases(alias.getOrDefault("stpaccept", "stpaccept"))
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpaccept)
                .override();

        command("stpdeny")
                .withAliases(alias.getOrDefault("stpdeny", "stpdeny"))
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpdeny)
                .override();

        command("stpcancel")
                .withAliases(alias.getOrDefault("stpcancel", "stpcancel"))
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpcancel)
                .override();

        command("stp")
                .withAliases(alias.getOrDefault("stp", "stp"))
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tp)
                .override();

        command("stphere")
                .withAliases(alias.getOrDefault("stphere", "stphere"))
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tphere)
                .override();

        command("stpahereall")
                .withAliases(alias.getOrDefault("stpahereall", "stpahereall"))
                .withPermission(CommandPermission.OP)
                .withArguments(literal("confirm"))
                .executesPlayer(TeleportCommand.instance::tpahereall)
                .override();

        command("stphereall")
                .withAliases(alias.getOrDefault("stphereall", "stphereall"))
                .withPermission(CommandPermission.OP)
                .withArguments(literal("confirm"))
                .executesPlayer(TeleportCommand.instance::tphereall)
                .override();

    }
}
