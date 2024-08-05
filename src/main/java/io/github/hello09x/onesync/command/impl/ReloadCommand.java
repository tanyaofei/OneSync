package io.github.hello09x.onesync.command.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.config.OneSyncConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@Singleton
public class ReloadCommand {

    private final OneSyncConfig config;

    @Inject
    public ReloadCommand(OneSyncConfig config) {
        this.config = config;
    }

    public void reload(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        config.reload();
        sender.sendMessage(text("重载成功", GRAY));
    }

}
