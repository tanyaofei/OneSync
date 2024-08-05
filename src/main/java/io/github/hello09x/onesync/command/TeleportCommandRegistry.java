package io.github.hello09x.onesync.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.hello09x.onesync.command.impl.TeleportCommand;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.teleport.PlayerManager;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static io.github.hello09x.devtools.command.Commands.command;


@Singleton
public class TeleportCommandRegistry {

    @Inject
    private TeleportCommand teleportCommand;

    @Inject
    private PlayerManager playerManager;

    @Inject
    private OneSyncConfig.TeleportConfig teleportConfig;

    public @NotNull Argument<String> globalPlayer(@NotNull String nodeName, boolean all) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            var input = info.currentInput();
            if (all && info.sender().isOp() && input.equals(io.github.hello09x.onesync.command.impl.TeleportCommand.ALL_PLAYERS)) {
                return io.github.hello09x.onesync.command.impl.TeleportCommand.ALL_PLAYERS;
            }

            var name = playerManager
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

            var suggestions = playerManager
                    .getPlayers()
                    .stream()
                    .filter(n -> n.toLowerCase().contains(input) && !n.equals(senderName))
                    .collect(Collectors.toList());

            if (all && info.sender().isOp()) {
                suggestions.add(io.github.hello09x.onesync.command.impl.TeleportCommand.ALL_PLAYERS);
            }

            return suggestions;
        }));
    }


    public void register() {
        var commands = teleportConfig.getCommands();
        command(commands.getOrDefault("stpa", "stpa"))
                .withAliases("stpa", "onesync:tpa")
                .withPermission(Permissions.TPA)
                .withArguments(globalPlayer("player", true))
                .executesPlayer(teleportCommand::tpa)
                .override();

        command(commands.getOrDefault("stp", "stp"))
                .withAliases("stp", "onesync:tp")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player", true))
                .executesPlayer(teleportCommand::tp)
                .override();

        command(commands.getOrDefault("stphere", "stphere"))
                .withAliases("stphere", "onesync:tphere")
                .withPermission(CommandPermission.OP)
                .withArguments(globalPlayer("player", true))
                .executesPlayer(teleportCommand::tphere)
                .override();

        command(commands.getOrDefault("stpahere", "stpahere"))
                .withAliases("stpahere", "onesync:tpahere")
                .withPermission(Permissions.TPAHERE)
                .withArguments(globalPlayer("player", true))
                .executesPlayer(teleportCommand::tpahere)
                .override();

        command(commands.getOrDefault("stpaccept", "stpaccept"))
                .withAliases("stpaccept", "onesync:tpaccept")
                .withOptionalArguments(globalPlayer("player", false))
                .executesPlayer(teleportCommand::tpaccept)
                .override();

        command(commands.getOrDefault("stpdeny", "stpdeny"))
                .withAliases("stpdeny", "onesync:tpdeny")
                .withOptionalArguments(globalPlayer("player", false))
                .executesPlayer(teleportCommand::tpdeny)
                .override();

        command(commands.getOrDefault("stpcancel", "stpcancel"))
                .withAliases("stpcancel", "onesync:tpcancel")
                .withOptionalArguments(globalPlayer("player", false))
                .executesPlayer(teleportCommand::tpcancel)
                .override();


    }
}
