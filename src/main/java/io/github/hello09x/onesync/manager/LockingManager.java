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
    private final static Logger log = Main.getInstance().getLogger();

    private final LockingRepository repository = LockingRepository.instance;

    public final static String CHANNEL = "onesync:locking";
    public final static String SUB_CHANNEL_RELOCK = "relock";


    /**
     * 如果玩家存在于当前服务器, 则对他上锁
     *
     * @param playerId 玩家 ID
     */
    public void relock(@NotNull UUID playerId) {
        Optional.ofNullable(Bukkit.getPlayer(playerId))
                .ifPresent(p -> repository.setLock(p.getUniqueId(), true));
    }

    /**
     * 对所有当前服务器在线的玩家上锁
     */
    public void relockAll() {
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
    public void removeLock(@NotNull OfflinePlayer player) {
        repository.deleteByPlayerId(player.getUniqueId());
        this.relock(player.getUniqueId());

        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(SUB_CHANNEL_RELOCK);
            message.writeUTF(player.getUniqueId().toString());
            r.sendPluginMessage(Main.getInstance(), CHANNEL, message.toByteArray());
        }
    }

    /**
     * 移除所有锁, 无论这个锁是哪台服务器上的, 之后再让所有服务器对在线的玩家上锁
     */
    public void removeAllLocks() {
        repository.deleteAll();
        this.relockAll();

        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(SUB_CHANNEL_RELOCK);
            message.writeUTF("ALL");
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
        if (!in.readUTF().equals(SUB_CHANNEL_RELOCK)) {
            return;
        }

        log.info("接收到「relock」消息");
        var arg = in.readUTF();
        if (arg.equals("ALL")) {
            this.relockAll();
        } else {
            this.relock(UUID.fromString(arg));
        }
    }
}
