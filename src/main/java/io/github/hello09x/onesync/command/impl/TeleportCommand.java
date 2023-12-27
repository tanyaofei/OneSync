package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.TeleportManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeleportCommand {

    public final static TeleportCommand instance = new TeleportCommand();

    private final TeleportManager manager = TeleportManager.instance;

    public void tp(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        manager.tp(sender, receiver);
    }

    public void tphere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        manager.tphere(sender, receiver);
    }

    public void tpaccept(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        manager.tpaccept(sender, requester);
    }

    public void tpdeny(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        // todo
    }

}
