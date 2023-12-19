package io.github.hello09x.onesync.command;

import dev.jorel.commandapi.CommandPermission;
import io.github.hello09x.onesync.command.impl.ReloadCommand;

import static io.github.hello09x.bedrock.command.Commands.command;

public class CommandRegistry {

    public void register() {
        command("onesync")
                .withSubcommands(
                        command("reload")
                                .withPermission(CommandPermission.OP)
                                .executes(ReloadCommand.instance::reload),
                        command("unlock")
                                .withPermission(CommandPermission.OP)
                );
    }


}
