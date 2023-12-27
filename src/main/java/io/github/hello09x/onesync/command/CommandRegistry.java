package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import io.github.hello09x.onesync.command.impl.ReloadCommand;
import io.github.hello09x.onesync.command.impl.SnapshotCommand;
import io.github.hello09x.onesync.command.impl.TeleportCommand;
import io.github.hello09x.onesync.command.impl.UnlockCommand;
import io.github.hello09x.onesync.manager.PlayerManager;
import org.jetbrains.annotations.NotNull;

import static io.github.hello09x.bedrock.command.Commands.*;

public class CommandRegistry {

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

//            if (name.equals(info.sender().getName())) {
//                throw CustomArgument.CustomArgumentException.fromString("该命令不能对自己使用");
//            }

            return name;
        }).replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
            var senderName = info.sender().getName();
            var input = info.currentArg().toLowerCase();

            return PlayerManager.instance
                    .getPlayers()
                    .stream()
                    .filter(n -> n.toLowerCase().contains(input) /*&& !n.equals(senderName)*/)
                    .toList();
        }));
    }

    public static void register() {
        command("onesync")
                .withSubcommands(
                        command("reload")
                                .withPermission(CommandPermission.OP)
                                .executes(ReloadCommand.instance::reload),
                        command("unlock")
                                .withPermission(CommandPermission.OP)
                                .withArguments(offlinePlayer("player"))
                                .executes(UnlockCommand.instance::unlock),
                        command("unlock-all")
                                .withPermission(CommandPermission.OP)
                                .executes(UnlockCommand.instance::unlockAll),
                        command("snapshot")
                                .withPermission(CommandPermission.OP)
                                .withArguments(offlinePlayer("player"))
                                .withOptionalArguments(int32("page", 1))
                                .executesPlayer(SnapshotCommand.instance::snapshot),
                        command("save")
                                .withPermission(CommandPermission.OP)
                                .withArguments(new EntitySelectorArgument.ManyPlayers("players"))
                                .executes(SnapshotCommand.instance::save)
                ).register();

        command("tpa")
                .withAliases("stpa")
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpa)
                .register();

        command("tpahere")
                .withAliases("stpahere")
                .withArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpahere)
                .register();

        command("tpaccept")
                .withAliases("stpaccept")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpaccept)
                .register();

        command("tpadeny")
                .withAliases("stpadeny")
                .withOptionalArguments(globalPlayer("player"))
                .executesPlayer(TeleportCommand.instance::tpadeny)
                .register();

    }


}
