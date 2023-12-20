package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.MenuManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;

public class SnapshotCommand {

    public final static SnapshotCommand instance = new SnapshotCommand();
    private final MenuManager manager = MenuManager.instance;

    public void snapshot(@NotNull Player sender, @NotNull CommandArguments args) {
        var player = (OfflinePlayer) Objects.requireNonNull(args.get("player"));
        var page = (int) args.getOptional("page").orElse(1);

        manager.openSnapshotPage(sender, page, player);
    }

}
