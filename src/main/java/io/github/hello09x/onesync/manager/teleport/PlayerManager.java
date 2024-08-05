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
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Singleton
@SuppressWarnings("UnstableApiUsage")
public class PlayerManager implements PluginMessageListener {


    private Set<String> players = new HashSet<>();

    private final static int TICK_PERIOD = 20;

    public PlayerManager() {
        if (ServerUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Main.getInstance(), ignored -> this.tick(), TICK_PERIOD, TICK_PERIOD);
        } else {
            Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::tick, TICK_PERIOD, TICK_PERIOD);
        }
    }

    public @NotNull @Unmodifiable Set<String> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    private long updatedAt = 0;

    private void tick() {
        if ((System.currentTimeMillis() - updatedAt) / 1000 < TICK_PERIOD / 20) {
            return;
        }
        BungeeCordUtils.getPlayerList(Main.getInstance());
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

        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals(BungeeCordUtils.SubChannel.PLAYER_LIST)) {
            return;
        }

        if (!in.readUTF().equals("ALL")) {
            return;
        }

        this.players = new HashSet<>(Arrays.asList(in.readUTF().split(", ")));
        updatedAt = System.currentTimeMillis();
    }
}
