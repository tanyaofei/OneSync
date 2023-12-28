package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.PlayerManager;
import io.github.hello09x.onesync.manager.TeleportManager;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeleportCommand {

    public final static TeleportCommand instance = new TeleportCommand();

    private final TeleportManager manager = TeleportManager.instance;

    public void tp(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        manager.ask(sender, receiver, TeleportType.TP, true);
    }

    public void tphere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        manager.ask(sender, receiver, TeleportType.TPHERE, true);
    }

    public void tpa(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        var message = manager.ask(sender, receiver, TeleportType.TP, false);
        sender.sendMessage(message);
    }

    public void tpahere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        var message = manager.ask(sender, receiver, TeleportType.TPHERE, false);
        sender.sendMessage(message);
    }

    public void tpahereall(@NotNull Player sender, @NotNull CommandArguments args) {
        var receivers = PlayerManager.instance.getPlayers();
        for (var receiver : receivers) {
            if (receiver.equals(sender.getName())) {
                continue;
            }
            manager.ask(sender, receiver, TeleportType.TPHERE, false);
        }
    }

    public void tphereall(@NotNull Player sender, @NotNull CommandArguments args) {
        var receivers = PlayerManager.instance.getPlayers();
        for (var receiver : receivers) {
            if (receiver.equals(sender.getName())) {
                continue;
            }
            manager.ask(sender, receiver, TeleportType.TPHERE, true);
        }
    }

    public void tpaccept(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        var message = manager.answer(sender, requester, true);
        sender.sendMessage(message);
    }

    public void tpdeny(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        var message = manager.answer(sender, requester, false);
        sender.sendMessage(message);
    }

    public void tpcancel(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) args.get("player");
        var message = manager.cancel(sender, receiver);
        sender.sendMessage(message);
    }

}
