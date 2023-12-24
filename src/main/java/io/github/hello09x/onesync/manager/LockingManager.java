package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import io.github.hello09x.onesync.Main;
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
    public final static String COMMAND_RELOCK = "Relock";
    private final static Logger log = Main.getInstance().getLogger();
    private final static String RELOCK_ALL = "ALL";
    private final LockingRepository repository = LockingRepository.instance;

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
    public void lock(@NotNull UUID playerId) {
        Optional.ofNullable(Bukkit.getPlayer(playerId))
                .ifPresent(p -> repository.setLock(p.getUniqueId(), true));
    }

    /**
     * 对玩家上锁
     *
     * @param player 玩家
     */
    public void lock(@NotNull Player player) {
        repository.setLock(player.getUniqueId(), true);
    }

    /**
     * 对玩家解锁
     *
     * @param player 玩家
     */
    public void unlock(@NotNull Player player) {
        repository.setLock(player.getUniqueId(), false);
    }

    /**
     * 对所有当前服务器在线的玩家上锁
     */
    public void lockAll() {
        for (var p : Bukkit.getOnlinePlayers()) {
            try {
                repository.setLock(p.getUniqueId(), true);
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
    public void relock(@NotNull OfflinePlayer player) {
        repository.deleteByPlayerId(player.getUniqueId());
        this.lock(player.getUniqueId());

        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_RELOCK);
            message.writeUTF(player.getUniqueId().toString());
            r.sendPluginMessage(Main.getInstance(), CHANNEL, message.toByteArray());
        }
    }

    /**
     * 移除所有锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器对在线的玩家上锁
     */
    public void relockAll() {
        repository.deleteAll();
        this.lockAll();

        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(COMMAND_RELOCK);
            message.writeUTF(RELOCK_ALL);
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
        if (!in.readUTF().equals(COMMAND_RELOCK)) {
            return;
        }

        if (Main.isDebugging()) {
            log.info("接收到「relock」消息");
        }
        var arg = in.readUTF();
        if (arg.equals(RELOCK_ALL)) {
            this.lockAll();
        } else {
            this.lock(UUID.fromString(arg));
        }
    }
}
