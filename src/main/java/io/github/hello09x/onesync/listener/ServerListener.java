package io.github.hello09x.onesync.listener;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class ServerListener implements Listener {

    public final static ServerListener instance = new ServerListener();
    private final static Logger log = Main.getInstance().getLogger();
    private final SynchronizeManager manager = SynchronizeManager.instance;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        if (!event.getWorld().getName().equals("world")) {
            return;
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            try {
                manager.save(player, false);
            } catch (Throwable e) {
                log.severe(Throwables.getStackTraceAsString(e));
            }
        }
    }

}
