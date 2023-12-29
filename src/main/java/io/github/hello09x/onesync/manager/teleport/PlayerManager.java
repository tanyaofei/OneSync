package io.github.hello09x.onesync.manager.teleport;

import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.BungeeCord;
import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.onesync.Main;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class PlayerManager implements PluginMessageListener {

    public final static PlayerManager instance = new PlayerManager();

    private Set<String> players = new HashSet<>();

    private final static int TICK_PERIOD = 20;

    private PlayerManager() {
        Folia.runTaskTimer(Main.getInstance(), this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    public @NotNull @Unmodifiable Set<String> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    private long updatedAt = 0;

    private void tick() {
        if ((System.currentTimeMillis() - updatedAt) / 1000 < TICK_PERIOD / 20) {
            return;
        }
        BungeeCord.getPlayerList(Main.getInstance());
    }


    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals("PlayerList")) {
            return;
        }

        if (!in.readUTF().equals("ALL")) {
            return;
        }

        this.players = new HashSet<>(Arrays.asList(in.readUTF().split(", ")));
        updatedAt = System.currentTimeMillis();
    }
}
