package io.github.hello09x.onesync.listener;

import io.github.hello09x.onesync.manager.TeleportManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;


public class TeleportListener implements Listener {


    private final TeleportManager manager = TeleportManager.instance;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var pos = manager.getTeleportLocation(player);
        if (pos != null) {
            player.teleportAsync(pos);  // support folia
        }
    }

}
