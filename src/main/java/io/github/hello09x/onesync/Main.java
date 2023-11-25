package io.github.hello09x.onesync;

import io.github.hello09x.onesync.command.CommandRegistry;
import io.github.hello09x.onesync.listener.PlayerListener;
import io.github.hello09x.onesync.listener.ServerListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    @Override
    public void onLoad() {
        instance = this;
        CommandRegistry.register();
    }

    @Override
    public void onEnable() {
        {
            var mgr = super.getServer().getPluginManager();
            mgr.registerEvents(PlayerListener.instance, this);
            mgr.registerEvents(ServerListener.instance, this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
