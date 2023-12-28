package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hello09x.onesync.command.impl.TeleportCommand;
import io.github.hello09x.onesync.manager.PlayerManager;
import org.jetbrains.annotations.NotNull;

import static io.github.hello09x.bedrock.command.Commands.command;

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
        command("stpa")
                .withAliases("tpa")
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpa)
                .override();

        command("stpahere")
                .withAliases("tpahere")
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpahere)
                .override();

        command("stpaccept")
                .withAliases("tpaccept")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpaccept)
                .override();

        command("stpdeny")
                .withAliases("tpdeny")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpdeny)
                .override();

        command("stpcancel")
                .withAliases("tpcancel")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpcancel)
                .override();

        command("tp")
                .withAliases("stp")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tp)
                .override();

        command("tphere")
                .withAliases("stphere")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tphere)
                .override();

    }
}
