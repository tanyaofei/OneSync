package io.github.hello09x.onesync.listener;

import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.TeleportManager;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var pos = manager.getTeleportLocation(player);
        if (pos == null) {
            return;
        }

        player.teleportAsync(pos).thenAccept(success -> {
            if (!success) {
                player.sendMessage(text("传送失败: 可能被第三方插件取消", RED));
            }
        });
    }

}
