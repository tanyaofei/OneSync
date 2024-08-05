package io.github.hello09x.onesync.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import io.github.hello09x.onesync.command.impl.ReloadCommand;
import io.github.hello09x.onesync.command.impl.SnapshotCommand;
import io.github.hello09x.onesync.command.impl.UnlockCommand;

import static io.github.hello09x.devtools.command.Commands.*;


@Singleton
public class SynchronizeCommandRegistry {

    @Inject
    private ReloadCommand reloadCommand;

    @Inject
    private UnlockCommand unlockCommand;

    @Inject
    private SnapshotCommand snapshotCommand;

    public void register() {
        command("onesync")
                .withSubcommands(
                        command("reload")
                                .withPermission(CommandPermission.OP)
                                .executes(reloadCommand::reload),
                        command("unlock")
                                .withPermission(CommandPermission.OP)
                                .withArguments(offlinePlayer("player"))
                                .executes(unlockCommand::unlock),
                        command("unlock-all")
                                .withPermission(CommandPermission.OP)
                                .executes(unlockCommand::unlockAll),
                        command("snapshot")
                                .withPermission(CommandPermission.OP)
                                .withArguments(offlinePlayer("player"))
                                .withOptionalArguments(int32("page", 1))
                                .executesPlayer(snapshotCommand::snapshot),
                        command("save")
                                .withPermission(CommandPermission.OP)
                                .withArguments(new EntitySelectorArgument.ManyPlayers("players"))
                                .executes(snapshotCommand::save)
                ).register();
    }


}
