package io.github.hello09x.onesync.manager.teleport;

import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.BungeeCord;
import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.onesync.Main;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ServerManager implements PluginMessageListener {

    public final static ServerManager instance = new ServerManager();

    private final static int TICK_PERIOD = 100;

    @NotNull
    private String current = "";

    private long updatedAt = 0;

    private ServerManager() {
        Folia.runTaskTimer(Main.getInstance(), this::tick, 20, 100);
    }

    private void tick() {
        if ((System.currentTimeMillis() - updatedAt) / 1000 < TICK_PERIOD / 20) {
            return;
        }

        BungeeCord.getServer(Main.getInstance());
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
        updatedAt = System.currentTimeMillis();
    }
}
