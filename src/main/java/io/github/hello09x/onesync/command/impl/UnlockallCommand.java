package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.LockingManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UnlockallCommand {

    public final static UnlockallCommand instance = new UnlockallCommand();
    private final LockingManager manager = LockingManager.instance;

    public void unlockall(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        manager.clear();
        sender.sendMessage(text("ok!", GRAY));
    }


}
