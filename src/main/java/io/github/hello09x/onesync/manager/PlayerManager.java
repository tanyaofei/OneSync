package io.github.hello09x.onesync.manager;

import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.Folia;
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

public class PlayerManager implements PluginMessageListener {

    public final static PlayerManager instance = new PlayerManager();

    /**
     * key: playerName
     * value: serverName
     */
    private Set<String> players = new HashSet<>();

    private PlayerManager() {
        Folia.runTaskTimer(Main.getInstance(), this::requestUpdatePlayers, 20, 20);
    }

    public @NotNull @Unmodifiable Set<String> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    private void requestUpdatePlayers() {
        var message = ByteStreams.newDataOutput();
        message.writeUTF("PlayerList");
        message.writeUTF("ALL");
        Bukkit.getServer().sendPluginMessage(Main.getInstance(), "BungeeCord", message.toByteArray());
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
    }
}
