package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.event.ConfigReloadedEvent;
import io.github.hello09x.devtools.core.utils.BungeeCordUtils;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.constant.SubChannels;
import io.github.hello09x.onesync.repository.LockingRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
@SuppressWarnings("UnstableApiUsage")
public class LockingManager implements PluginMessageListener, Listener {

    public final static String COMMAND_ACQUIRE = "Acquire";
    private final static String ACQUIRE_ALL = "ALL";
    private final static String SUB_CHANNEL = SubChannels.Locking;
    private final static Logger log = OneSync.getInstance().getLogger();

    private final LockingRepository repository;

    private final OneSyncConfig config;
    private String serverId;

    @Inject
    public LockingManager(LockingRepository repository, OneSyncConfig config) {
        this.repository = repository;
        this.config = config;
        this.serverId = config.getServerId();
    }

    @EventHandler
    public void onConfigReloaded(@NotNull ConfigReloadedEvent event) {
        var oldServerId = this.serverId;
        if (!this.config.getServerId().equals(this.serverId)) {
            this.serverId = this.config.getServerId();
            log.info("服务器 ID 已发生变化, %s -> %s".formatted(oldServerId, this.serverId));
        }
    }

    /**
     * 判断玩家是否已上锁
     *
     * @param playerId 玩家 ID
     * @return 是否已上锁
     */
    public boolean isLocked(@NotNull UUID playerId) {
        return this.repository.selectByPlayerId(playerId) != null;
    }

    /**
     * 如果玩家存在于当前服务器, 则对他上锁
     *
     * @param playerId 玩家 ID
     */
    public void acquire(@NotNull UUID playerId) {
        Optional.ofNullable(Bukkit.getPlayer(playerId))
                .ifPresent(p -> repository.insertOrUpdate(p.getUniqueId(), this.serverId, true));
    }

    /**
     * 对玩家上锁
     *
     * @param player 玩家
     */
    public void acquire(@NotNull Player player) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Not in main thread");
        }
        repository.insertOrUpdate(player.getUniqueId(), this.serverId, true);
    }

    /**
     * 对玩家解锁
     *
     * @param player 玩家
     */
    public void release(@NotNull Player player) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Not in main thread");
        }
        repository.insertOrUpdate(player.getUniqueId(), this.serverId, false);
    }

    /**
     * 解锁当前服务器的
     */
    public void releaseAll() {
        if (repository.deleteByServerId(this.serverId) > 0) {
            this.broadcastRequireAll();
        }
    }

    /**
     * 对所有当前服务器在线的玩家上锁
     */
    public void acquireAll() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Not in main thread");
        }
        for (var p : Bukkit.getOnlinePlayers()) {
            try {
                repository.insertOrUpdate(p.getUniqueId(), this.serverId, true);
            } catch (Throwable e) {
                log.severe(Throwables.getStackTraceAsString(e));
            }
        }
    }

    /**
     * 移除锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器对该玩家上锁
     *
     * @param player 玩家
     */
    public void reacquire(@NotNull OfflinePlayer player) {
        if (repository.deleteByPlayerId(player.getUniqueId()) > 0) {
            this.acquire(player.getUniqueId());
            // 避免这个玩家是在别的服务器上玩的, 让其他服务器对该玩家重新上锁
            this.broadcastRequire(player);
        }
    }

    /**
     * 移除所有锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器对在线的玩家上锁
     */
    public void reacquireAll() {
        if (repository.deleteAll() > 0) {
            this.acquireAll();
            // 因为删掉了所有服务器的锁, 需要让其他服务器重新上锁
            this.broadcastRequireAll();
        }
    }

    /**
     * 发送插件消息, 对特定玩家重新上锁
     *
     * @param player 玩家
     */
    public void broadcastRequire(@NotNull OfflinePlayer player) {
        BungeeCordUtils.sendForwardPluginMessage(OneSync.getInstance(), SUB_CHANNEL, () -> {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_ACQUIRE);
            message.writeUTF(player.getUniqueId().toString());
            return message.toByteArray();
        });
    }

    /**
     * 发送插件消息, 让其他服务器重新上锁
     */
    private void broadcastRequireAll() {
        BungeeCordUtils.sendForwardPluginMessage(OneSync.getInstance(), SUB_CHANNEL, () -> {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_ACQUIRE);
            message.writeUTF(ACQUIRE_ALL);
            return message.toByteArray();
        });
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (!channel.equals(BungeeCordUtils.CHANNEL)) {
            return;
        }

        var in = BungeeCordUtils.parseReceivedForwardMessage(SUB_CHANNEL, message);
        if (in == null) {
            return;
        }

        if (!in.readUTF().equals(COMMAND_ACQUIRE)) {
            return;
        }

        log.config("接收到「relock」消息");
        var arg = in.readUTF();
        if (arg.equals(ACQUIRE_ALL)) {
            this.acquireAll();
        } else {
            this.acquire(UUID.fromString(arg));
        }
    }
}
