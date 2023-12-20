package io.github.hello09x.onesync;

import io.github.hello09x.bedrock.menu.ChestMenuRegistry;
import io.github.hello09x.onesync.command.CommandRegistry;
import io.github.hello09x.onesync.listener.PlayerListener;
import io.github.hello09x.onesync.listener.SnapshotListener;
import io.github.hello09x.onesync.manager.LockingManager;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    @Getter
    private static ChestMenuRegistry menuRegistry;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        menuRegistry = new ChestMenuRegistry(this);
        CommandRegistry.register();
        {
            var pm = super.getServer().getPluginManager();
            pm.registerEvents(PlayerListener.instance, this);
            pm.registerEvents(SnapshotListener.instance, this);
        }

        {
            var messenger = getServer().getMessenger();
            messenger.registerIncomingPluginChannel(this, LockingManager.CHANNEL, LockingManager.instance);
            messenger.registerOutgoingPluginChannel(this, LockingManager.CHANNEL);
        }

        LockingManager.instance.relock();   // 热重载
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        super.onDisable();
        {
            var messenger = getServer().getMessenger();
            messenger.unregisterIncomingPluginChannel(this);
            messenger.unregisterOutgoingPluginChannel(this);
        }

        SynchronizeManager.instance.saveAll(SnapshotCause.PLUGIN_DISABLE);  // 关闭服务器不会调用 PlayerQuitEvent 事件, 因此需要全量保存一次
    }
}
