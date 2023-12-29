package io.github.hello09x.onesync.listener;

import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.teleport.TeleportManager;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;


public class TeleportListener implements Listener {

    public final static TeleportListener instance = new TeleportListener();

    private final TeleportManager manager = TeleportManager.instance;
    private final OneSyncConfig.TeleportConfig config = OneSyncConfig.instance.getTeleport();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var pos = manager.getTeleportLocation(player);
        if (pos == null) {
            return;
        }

        player.teleportAsync(pos).thenAccept(success -> {
            if (!success) {
                player.sendMessage(text("传送失败: 可能被第三方插件取消或者你被骑了", RED));
            } else {
                if (config.isSound()) {
                    player.getLocation().getWorld().playSound(
                            player.getLocation(),
                            Sound.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.PLAYERS,
                            0.8F,
                            1.0F
                    );
                }
            }
        });
    }

}
