package io.github.hello09x.onesync;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.hello09x.devtools.core.CoreModule;
import io.github.hello09x.devtools.core.utils.BungeeCordUtils;
import io.github.hello09x.devtools.database.DatabaseModule;
import io.github.hello09x.devtools.menu.ChestMenuRegistry;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.command.SynchronizeCommandRegistry;
import io.github.hello09x.onesync.command.TeleportCommandRegistry;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.listener.BatonListener;
import io.github.hello09x.onesync.listener.SnapshotListener;
import io.github.hello09x.onesync.listener.SynchronizeListener;
import io.github.hello09x.onesync.listener.TeleportListener;
import io.github.hello09x.onesync.manager.synchronize.LockingManager;
import io.github.hello09x.onesync.manager.synchronize.SynchronizeManager;
import io.github.hello09x.onesync.manager.synchronize.handler.*;
import io.github.hello09x.onesync.manager.teleport.PlayerManager;
import io.github.hello09x.onesync.manager.teleport.ServerManager;
import io.github.hello09x.onesync.manager.teleport.TeleportManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static ChestMenuRegistry chestMenuRegistry;
    @Getter
    private static Injector injector;

    @Override
    public void onLoad() {
        instance = this;
        injector = Guice.createInjector(
                new OneSyncModule(),
                new CoreModule(),
                new DatabaseModule()
        );
    }

    @Override
    public void onEnable() {
        try {
            chestMenuRegistry = new ChestMenuRegistry(this);
            injector.getInstance(SynchronizeCommandRegistry.class).register();

            {
                var sm = Bukkit.getServicesManager();
                sm.register(SnapshotHandler.class, injector.getInstance(EnderChestSnapshotHandler.class), this, ServicePriority.Highest);
                sm.register(SnapshotHandler.class, injector.getInstance(InventorySnapshotHandler.class), this, ServicePriority.Highest);
                sm.register(SnapshotHandler.class, injector.getInstance(ProfileSnapshotHandler.class), this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, injector.getInstance(AdvancementSnapshotHandler.class), this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, injector.getInstance(PDCSnapshotHandler.class), this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, injector.getInstance(PotionEffectSnapshotHandler.class), this, ServicePriority.Normal);
                sm.register(SnapshotHandler.class, injector.getInstance(VaultSnapshotHandler.class), this, ServicePriority.Normal);
            }

            {
                var pm = super.getServer().getPluginManager();
                pm.registerEvents(injector.getInstance(SynchronizeListener.class), this);
                pm.registerEvents(injector.getInstance(SnapshotListener.class), this);
                pm.registerEvents(injector.getInstance(TeleportListener.class), this);
                pm.registerEvents(injector.getInstance(BatonListener.class), this);
                pm.registerEvents(injector.getInstance(LockingManager.class), this);
            }

            {
                var messenger = getServer().getMessenger();
                messenger.registerIncomingPluginChannel(this, BungeeCordUtils.CHANNEL, injector.getInstance(LockingManager.class));
                messenger.registerOutgoingPluginChannel(this, BungeeCordUtils.CHANNEL);

                messenger.registerIncomingPluginChannel(this, BungeeCordUtils.CHANNEL, injector.getInstance(ServerManager.class));
                messenger.registerOutgoingPluginChannel(this, BungeeCordUtils.CHANNEL);

                messenger.registerIncomingPluginChannel(this, BungeeCordUtils.CHANNEL, injector.getInstance(PlayerManager.class));
                messenger.registerOutgoingPluginChannel(this, BungeeCordUtils.CHANNEL);

                messenger.registerIncomingPluginChannel(this, BungeeCordUtils.CHANNEL, injector.getInstance(TeleportManager.class));
                messenger.registerOutgoingPluginChannel(this, BungeeCordUtils.CHANNEL);
            }

            {
                var mgr = ProtocolLibrary.getProtocolManager();
                mgr.addPacketListener(injector.getInstance(SynchronizeListener.class));
            }

            {
                var lockingManager = injector.getInstance(LockingManager.class);
                lockingManager.releaseAll();     // 崩服重启, 释放上一次会话的锁
                lockingManager.acquireAll();     // 热重载
            }

            if (injector.getInstance(OneSyncConfig.TeleportConfig.class).isEnabled()) {
                injector.getInstance(TeleportCommandRegistry.class).register();
            }
        } catch (Throwable e) {
            getLogger().severe("加载插件失败, 为了数据安全, 关闭当前服务器!\n" + Throwables.getStackTraceAsString(e));
            Bukkit.shutdown();
            throw e;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        injector.getInstance(SynchronizeManager.class).saveAndUnlockAll(SnapshotCause.PLUGIN_DISABLE); // 关闭服务器不会调用 PlayerQuitEvent 事件, 因此需要全量保存一次

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
