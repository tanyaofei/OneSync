package io.github.hello09x.onesync.command.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.teleport.PlayerManager;
import io.github.hello09x.onesync.manager.teleport.TeleportManager;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@Singleton
public class TeleportCommand {

    public final static String ALL_PLAYERS = "-a";

    private final TeleportManager teleportManager;
    private final PlayerManager playerManager;

    @Inject
    public TeleportCommand(TeleportManager teleportManager, PlayerManager playerManager) {
        this.teleportManager = teleportManager;
        this.playerManager = playerManager;
    }

    public void tp(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        if (!receiver.equals(ALL_PLAYERS)) {
            teleportManager.ask(sender, receiver, TeleportType.TP, true);
            return;
        }

        for (var r : playerManager.getPlayers()) {
            if (r.equals(sender.getName())) {
                continue;
            }
            teleportManager.ask(sender, r, TeleportType.TP, true);
        }
    }

    public void tpa(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        if (!receiver.equals(ALL_PLAYERS)) {
            sender.sendMessage(teleportManager.ask(sender, receiver, TeleportType.TP, false));
            return;
        }

        int count = 0;
        for (var r : playerManager.getPlayers()) {
            if (r.equals(sender.getName())) {
                continue;
            }
            teleportManager.ask(sender, r, TeleportType.TP, false);
            count++;
        }

        if (count == 0) {
            sender.sendMessage(text("没有发送传送请求给任何玩家", GRAY));
        } else {
            sender.sendMessage(textOfChildren(
                    text("传送请求已发送给 ", GRAY),
                    text(count),
                    text(" 名玩家, 等待他们接受", GRAY))
            );
        }
    }

    public void tphere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        if (!receiver.equals(ALL_PLAYERS)) {
            teleportManager.ask(sender, receiver, TeleportType.TPHERE, true);
            return;
        }

        for (var r : playerManager.getPlayers()) {
            if (r.equals(sender.getName())) {
                continue;
            }
            teleportManager.ask(sender, r, TeleportType.TPHERE, true);
        }
    }

    public void tpahere(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) Objects.requireNonNull(args.get("player"));
        if (!receiver.equals(ALL_PLAYERS)) {
            sender.sendMessage(teleportManager.ask(sender, receiver, TeleportType.TPHERE, false));
            return;
        }

        int count = 0;
        for (var r : playerManager.getPlayers()) {
            if (r.equals(sender.getName())) {
                continue;
            }
            teleportManager.ask(sender, r, TeleportType.TPHERE, false);
            count++;
        }

        if (count == 0) {
            sender.sendMessage(text("没有发送传送请求给任何玩家", GRAY));
        } else {
            sender.sendMessage(textOfChildren(
                    text("传送请求已发送给 ", GRAY),
                    text(count),
                    text(" 名玩家, 等待他们接受", GRAY))
            );
        }
    }

    public void tpaccept(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        var message = teleportManager.answer(sender, requester, true);
        sender.sendMessage(message);
    }

    public void tpdeny(@NotNull Player sender, @NotNull CommandArguments args) {
        var requester = (String) args.get("player");
        var message = teleportManager.answer(sender, requester, false);
        sender.sendMessage(message);
    }

    public void tpcancel(@NotNull Player sender, @NotNull CommandArguments args) {
        var receiver = (String) args.get("player");
        var message = teleportManager.cancel(sender, receiver);
        sender.sendMessage(message);
    }

}
