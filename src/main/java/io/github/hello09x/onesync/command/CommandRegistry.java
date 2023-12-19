package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import io.github.hello09x.onesync.command.impl.ReloadCommand;
import io.github.hello09x.onesync.command.impl.UnlockCommand;

import static io.github.hello09x.bedrock.command.Commands.command;
import static io.github.hello09x.bedrock.command.Commands.player;

public class CommandRegistry {

    public static void register() {
        command("onesync")
                .withSubcommands(
                        command("reload")
                                .withPermission(CommandPermission.OP)
                                .executes(ReloadCommand.instance::reload),
                        command("unlock")
                                .withPermission(CommandPermission.OP)
                                .withArguments(player("player"))
                                .executes(UnlockCommand.instance::unlock),
                        command("unlock-all")
                                .withPermission(CommandPermission.OP)
                                .executes(UnlockCommand.instance::unlockAll)
                ).register();
    }


}
