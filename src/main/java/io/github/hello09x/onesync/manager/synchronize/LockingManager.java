package io.github.hello09x.onesync.manager.synchronize;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.BungeeCord;
import io.github.hello09x.bedrock.util.MCUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.constant.SubChannels;
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
    public final static String COMMAND_ACQUIRE = "Acquire";
    private final static String ACQUIRE_ALL = "ALL";
    private final static String SUB_CHANNEL = SubChannels.Locking;
    private final static Logger log = Main.getInstance().getLogger();

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
        MCUtils.ensureMainThread();
        repository.setLock(player.getUniqueId(), this.serverId, true);
    }

    /**
     * 对玩家解锁
     *
     * @param player 玩家
     */
    public void release(@NotNull Player player) {
        MCUtils.ensureMainThread();
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
        MCUtils.ensureMainThread();
        for (var p : Bukkit.getOnlinePlayers()) {
            try {
                repository.setLock(p.getUniqueId(), this.serverId, true);
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
    public void requireAll() {
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
        BungeeCord.sendForwardPluginMessage(Main.getInstance(), SUB_CHANNEL, () -> {
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
        BungeeCord.sendForwardPluginMessage(Main.getInstance(), SUB_CHANNEL, () -> {
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
        if (!channel.equals(BungeeCord.CHANNEL)) {
            return;
        }

        var in = BungeeCord.parseReceivedForward(SUB_CHANNEL, message);
        if (in == null) {
            return;
        }

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
