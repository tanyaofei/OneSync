package io.github.hello09x.onesync;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import io.github.hello09x.bedrock.menu.ChestMenuRegistry;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.command.CommandRegistry;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.handler.*;
import io.github.hello09x.onesync.listener.SnapshotListener;
import io.github.hello09x.onesync.listener.SynchronizeListener;
import io.github.hello09x.onesync.manager.LockingManager;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private final static Gson gson = new Gson();
    @Getter
    private static Main instance;
    @Getter
    private static ChestMenuRegistry chestMenuRegistry;

    @Override
    public void onLoad() {
        instance = this;
    }

    public static boolean isDebugging() {
        return OneSyncConfig.instance.isDebug();
    }

    @Override
    public void onEnable() {
        try {
            chestMenuRegistry = new ChestMenuRegistry(this);
            CommandRegistry.register();

            {
                var sm = Bukkit.getServicesManager();
                sm.register(SnapshotHandler.class, EnderChestSnapshotHandler.instance, this, ServicePriority.Highest);
                sm.register(SnapshotHandler.class, InventorySnapshotHandler.instance, this, ServicePriority.Highest);
                sm.register(SnapshotHandler.class, ProfileSnapshotHandler.instance, this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, AdvancementSnapshotHandler.instance, this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, PDCSnapshotHandler.instance, this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, PotionEffectSnapshotHandler.instance, this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, VaultSnapshotHandler.instance, this, ServicePriority.Normal);
            }

            {
                var pm = super.getServer().getPluginManager();
                pm.registerEvents(SynchronizeListener.instance, this);
                pm.registerEvents(SnapshotListener.instance, this);
            }

            {
                var messenger = getServer().getMessenger();
                messenger.registerIncomingPluginChannel(this, LockingManager.CHANNEL, LockingManager.instance);
                messenger.registerOutgoingPluginChannel(this, LockingManager.CHANNEL);
            }

            {
                var mgr = ProtocolLibrary.getProtocolManager();
                mgr.addPacketListener(SynchronizeListener.instance);
            }

            LockingManager.instance.lockAll();   // 热重载
        } catch (Throwable e) {
            getLogger().severe("加载插件失败, 为了数据安全, 关闭当前服务器: %s".formatted(Throwables.getStackTraceAsString(e)));
            Bukkit.shutdown();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        super.onDisable();
        SynchronizeManager.instance.saveAndUnlockAll(SnapshotCause.PLUGIN_DISABLE);  // 关闭服务器不会调用 PlayerQuitEvent 事件, 因此需要全量保存一次

        {
            var messenger = getServer().getMessenger();
            messenger.unregisterIncomingPluginChannel(this);
            messenger.unregisterOutgoingPluginChannel(this);
        }

        {
            Bukkit.getServicesManager().unregisterAll(this);
        }
    }
}
