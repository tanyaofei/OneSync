package io.github.hello09x.onesync;

import io.github.hello09x.onesync.listener.PlayerListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        {
            var mgr = super.getServer().getPluginManager();
            mgr.registerEvents(PlayerListener.instance, this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
