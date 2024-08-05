package io.github.hello09x.onesync.manager.teleport;

import com.google.common.io.ByteStreams;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.BungeeCordUtils;
import io.github.hello09x.devtools.core.utils.ServerUtils;
import io.github.hello09x.onesync.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

@Singleton
@SuppressWarnings("UnstableApiUsage")
public class ServerManager implements PluginMessageListener {

    private final static int TICK_PERIOD = 100;

    @NotNull
    private String current = "";

    private long updatedAt = 0;

    public ServerManager() {
        if (ServerUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Main.getInstance(), ignored -> this.tick(), 20, 100);
        } else {
            Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::tick, 20, 100);
        }
    }

    private void tick() {
        if ((System.currentTimeMillis() - updatedAt) / 1000 < TICK_PERIOD / 20) {
            return;
        }

        BungeeCordUtils.getServer(Main.getInstance());
    }

    public @NotNull String getCurrent() {
        return this.current;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(BungeeCordUtils.CHANNEL)) {
            return;
        }

        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals(BungeeCordUtils.SubChannel.GET_SERVER)) {
            return;
        }

        this.current = in.readUTF();
        updatedAt = System.currentTimeMillis();
    }
}
