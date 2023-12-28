package io.github.hello09x.onesync.manager;

import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class ServerManager implements PluginMessageListener {

    public final static ServerManager instance = new ServerManager();
    private final OneSyncConfig.TeleportConfig config = OneSyncConfig.instance.getTeleport();

    @NotNull
    private String current = "";

    private ServerManager() {
        Folia.runTaskTimer(Main.getInstance(), this::getCurrentServer0, 20, 100);
    }

    private void getCurrentServer0() {
        if (!config.isEnabled()) {
            return;
        }

        var out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        Bukkit.getServer().sendPluginMessage(Main.getInstance(), "BungeeCord", out.toByteArray());
    }

    public @NotNull String getCurrent() {
        return this.current;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        var in = ByteStreams.newDataInput(message);
        if (!in.readUTF().equals("GetServer")) {
            return;
        }

        this.current = in.readUTF();
    }
}
