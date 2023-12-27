package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.TeleportManager;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeleportCommand {

    public final static TeleportCommand instance = new TeleportCommand();

    private final TeleportManager manager = TeleportManager.instance;

    public void tpa(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        var message = manager.ask(sender, receiver, TeleportType.TP);
        sender.sendMessage(message);
    }

    public void tpahere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        var message = manager.ask(sender, receiver, TeleportType.TPHERE);
        sender.sendMessage(message);
    }

    public void tpaccept(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        var message = manager.accept(sender, requester);
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    public void tpadeny(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        // todo
    }

}
