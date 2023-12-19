package io.github.hello09x.onesync.manager;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.LockingRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Logger;

public class LockingManager implements PluginMessageListener {

    public final static LockingManager instance = new LockingManager();
    private final static Logger log = Main.getInstance().getLogger();

    private final LockingRepository repository = LockingRepository.instance;

    public final static String CHANNEL = "onesync:locking";
    public final static String SUB_CHANNEL_RELOCK = "relock";

    public void lock(@NotNull UUID playerId) {
        repository.setLock(playerId, true);
    }

    public void unlock(@NotNull OfflinePlayer player) {
        repository.deleteByPlayerId(player.getUniqueId());
    }

    public void unlockAll() {
        repository.deleteAll();
        this.relock();

        var r = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (r != null) {
            var message = ByteStreams.newDataOutput();
            message.writeUTF(SUB_CHANNEL_RELOCK);
            r.sendPluginMessage(Main.getInstance(), CHANNEL, message.toByteArray());
        }

    }

    public void relock() {
        for (var player : Bukkit.getOnlinePlayers()) {
            this.lock(player.getUniqueId());
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

        @SuppressWarnings("UnstableApiUsage")
        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals(SUB_CHANNEL_RELOCK)) {
            return;
        }

        log.info("接收到「relock」消息");
        this.relock();
    }
}
