package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.LockingManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UnlockCommand {

    public final static UnlockCommand instance = new UnlockCommand();
    private final LockingManager lockingManager = LockingManager.instance;

    public void unlock(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var player = Objects.requireNonNull((OfflinePlayer) args.get("player"));
        lockingManager.setLock(player, false);
        sender.sendMessage(text("ok!", GRAY));
    }

}
