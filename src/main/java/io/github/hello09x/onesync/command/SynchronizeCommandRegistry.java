package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import io.github.hello09x.onesync.command.impl.ReloadCommand;
import io.github.hello09x.onesync.command.impl.SnapshotCommand;
import io.github.hello09x.onesync.command.impl.UnlockCommand;

import static io.github.hello09x.bedrock.command.Commands.*;

public class SynchronizeCommandRegistry {

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
    }


}
