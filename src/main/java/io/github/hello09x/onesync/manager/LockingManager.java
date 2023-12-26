package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.MCUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.LockingRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class LockingManager implements PluginMessageListener {

    public final static LockingManager instance = new LockingManager();
    public final static String CHANNEL = "onesync:locking";
    public final static String COMMAND_ACQUIRE = "Acquire";
    private final static Logger log = Main.getInstance().getLogger();
    private final static String ACQUIRE_ALL = "ALL";
    private final LockingRepository repository = LockingRepository.instance;

    private String serverId = OneSyncConfig.instance.getServerId();

    public LockingManager() {
        OneSyncConfig.instance.addListener(config -> {
            if (!config.getServerId().equals(this.serverId)) {
                var old = this.serverId;
                try {
                    repository.updateServerId(old, config.getServerId());
                } catch (Throwable e) {
                    config.setServerId(old); // rollback
                    throw e;
                }
                this.serverId = config.getServerId();
                if (Main.isDebugging()) {
                    log.info("服务器 ID 已发生变化, %s -> %s".formatted(old, config.getServerId()));
                }
            }
        });
    }

    /**
     * 判断玩家是否已上锁
     *
     * @param playerId 玩家 ID
     * @return 是否已上锁
     */
    public boolean isLocked(@NotNull UUID playerId) {
        return this.repository.selectById(playerId) != null;
    }

    /**
     * 如果玩家存在于当前服务器, 则对他上锁
     *
     * @param playerId 玩家 ID
     */
    public void acquire(@NotNull UUID playerId) {
        Optional.ofNullable(Bukkit.getPlayer(playerId))
                .ifPresent(p -> repository.setLock(p.getUniqueId(), this.serverId, true));
    }

    /**
     * 对玩家上锁
     *
     * @param player 玩家
     */
    public void acquire(@NotNull Player player) {
        MCUtils.ensureMain();
        repository.setLock(player.getUniqueId(), this.serverId, true);
    }

    /**
     * 对玩家解锁
     *
     * @param player 玩家
     */
    public void release(@NotNull Player player) {
        MCUtils.ensureMain();
        repository.setLock(player.getUniqueId(), this.serverId, false);
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
        MCUtils.ensureMain();
        for (var p : Bukkit.getOnlinePlayers()) {
            try {
                repository.setLock(p.getUniqueId(), this.serverId, true);
            } catch (Throwable e) {
                log.severe(Throwables.getStackTraceAsString(e));
            }
        }
    }

    /**
     * 移除锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器该玩家上锁
     *
     * @param player 玩家
     */
    public void reacquire(@NotNull OfflinePlayer player) {
        if (repository.deleteByPlayerId(player.getUniqueId()) > 0) {
            this.acquire(player.getUniqueId());
            this.broadcastRequire(player);
        }
    }

    /**
     * 移除所有锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器对在线的玩家上锁
     */
    public void requireAll() {
        if (repository.deleteAll() > 0) {
            this.acquireAll();
            this.broadcastRequireAll();
        }
    }

    /**
     * 发送插件消息, 对特定玩家重新上锁
     *
     * @param player 玩家
     */
    public void broadcastRequire(@NotNull OfflinePlayer player) {
        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_ACQUIRE);
            message.writeUTF(player.getUniqueId().toString());
            r.sendPluginMessage(Main.getInstance(), CHANNEL, message.toByteArray());
        }
    }

    /**
     * 发送插件消息, 让其他服务器重新上锁
     */
    private void broadcastRequireAll() {
        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_ACQUIRE);
            message.writeUTF(ACQUIRE_ALL);
            r.sendPluginMessage(Main.getInstance(), CHANNEL, message.toByteArray());
        }
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals(COMMAND_ACQUIRE)) {
            return;
        }

        if (Main.isDebugging()) {
            log.info("接收到「relock」消息");
        }
        var arg = in.readUTF();
        if (arg.equals(ACQUIRE_ALL)) {
            this.acquireAll();
        } else {
            this.acquire(UUID.fromString(arg));
        }
    }
}
