package io.github.hello09x.onesync.listener;

import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.synchronize.BatonManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class BatonListener implements Listener {

    public final static BatonListener instance = new BatonListener();
    private final BatonManager batonManager = BatonManager.instance;

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Folia.runTaskLater(Main.getInstance(), event.getPlayer(), () -> {
            if (event.getPlayer().equals(event.getPlayer().getPlayer())) {
                return;
            }
            batonManager.remove(event.getPlayer());
        }, 1);
    }

}
