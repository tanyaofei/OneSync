package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.config.OneSyncConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class ReloadCommand {

    public final static ReloadCommand instance = new ReloadCommand();
    private final OneSyncConfig config = OneSyncConfig.instance;

    public void reload(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        config.reload(true);
        sender.sendMessage(text("重载成功", GRAY));
    }

}
