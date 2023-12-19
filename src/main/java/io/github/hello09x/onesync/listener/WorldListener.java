package io.github.hello09x.onesync.listener;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.SnapshotManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class WorldListener implements Listener {

    public final static WorldListener instance = new WorldListener();

    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotManager manager = SnapshotManager.instance;
    private final OneSyncConfig config = OneSyncConfig.instance;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSaveWorld(@NotNull WorldSaveEvent event) {
        if (config.getSnapshot().getWhen().contains(SnapshotCause.WORLD_SAVE)) {
            manager.createAll(SnapshotCause.WORLD_SAVE);
        }
    }

}
