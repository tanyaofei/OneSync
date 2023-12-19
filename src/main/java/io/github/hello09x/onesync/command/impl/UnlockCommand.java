package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.repository.LockingRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class UnlockCommand {

    public final static UnlockCommand instance = new UnlockCommand();
    private final LockingRepository synchronizer = LockingRepository.instance;

    public void unlock(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var player = (OfflinePlayer) Objects.requireNonNull(args.get("player"));
        synchronizer.setLock(player.getUniqueId(), false);
        sender.sendMessage(text("解锁成功", GRAY));
    }

    public void unlockAll(@NotNull CommandSender sender, @NotNull CommandArguments args) {

    }


}
