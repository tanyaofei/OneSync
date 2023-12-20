package io.github.hello09x.onesync.listener;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.SnapshotManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class SnapshotListener implements Listener {

    public final static SnapshotListener instance = new SnapshotListener();

    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotManager snapshotManager = SnapshotManager.instance;
    private final OneSyncConfig config = OneSyncConfig.instance;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSaveWorld(@NotNull WorldSaveEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.WORLD_SAVE)) {
            return;
        }

        if (!Bukkit.getWorlds().get(0).equals(event.getWorld())) {
            return;
        }

        snapshotManager.createAll(SnapshotCause.WORLD_SAVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.PLAYER_DEATH)) {
            return;
        }

        snapshotManager.create(event.getPlayer(), SnapshotCause.PLAYER_DEATH);
    }

}
