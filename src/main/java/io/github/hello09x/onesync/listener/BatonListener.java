package io.github.hello09x.onesync.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ServerUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.synchronize.BatonManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class BatonListener implements Listener {

    private final BatonManager batonManager;

    @Inject
    public BatonListener(BatonManager batonManager) {
        this.batonManager = batonManager;
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Runnable doRemoveBaton = () -> {
            if (event.getPlayer().equals(event.getPlayer().getPlayer())) {
                return;
            }
            batonManager.remove(event.getPlayer());
        };
        if (ServerUtils.isFolia()) {
            event.getPlayer().getScheduler().runDelayed(Main.getInstance(), ignored -> doRemoveBaton.run(), null, 1);
        } else {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), doRemoveBaton, 1);
        }
    }

}
