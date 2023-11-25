package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import io.github.hello09x.onesync.command.impl.UnlockCommand;
import io.github.hello09x.onesync.command.impl.UnlockallCommand;

import static io.github.hello09x.bedrock.command.Commands.command;
import static io.github.hello09x.bedrock.command.Commands.offlinePlayer;

public class CommandRegistry {


    public static void register() {
        command("onesync")
                .withSubcommands(
                        command("unlock")
                                .withPermission(CommandPermission.OP)
                                .withArguments(offlinePlayer("player"))
                                .executes(UnlockCommand.instance::unlock),
                        command("unlockall")
                                .withPermission(CommandPermission.OP)
                                .executes(UnlockallCommand.instance::unlockall)
                ).register();
    }


}
