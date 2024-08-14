package io.github.hello09x.onesync.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.SnapshotManager;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

@Singleton
public class SnapshotListener implements Listener {

    private final static Logger log = OneSync.getInstance().getLogger();

    private final SnapshotManager snapshotManager;
    private final SynchronizeManager synchronizeManager;
    private final OneSyncConfig config;

    @Inject
    public SnapshotListener(SnapshotManager snapshotManager, SynchronizeManager synchronizeManager, OneSyncConfig config) {
        this.snapshotManager = snapshotManager;
        this.synchronizeManager = synchronizeManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSaveWorld(@NotNull WorldSaveEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.WORLD_SAVE)) {
            return;
        }

        if (!Bukkit.getWorlds().get(0).equals(event.getWorld())) {
            return;
        }

        var players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }

        snapshotManager.create(players, SnapshotCause.WORLD_SAVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.PLAYER_DEATH)) {
            return;
        }

        var player = event.getPlayer();
        if (synchronizeManager.isRestoring(player)) {
            // 如果该玩家正在恢复数据中, 则跳过
            log.config("玩家 %s 恢复数据中, 此此「死亡」不会创建快照".formatted(player.getName()));
            return;
        }

        snapshotManager.create(player, SnapshotCause.PLAYER_DEATH);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(@NotNull PlayerGameModeChangeEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.PLAYER_GAME_MODE_CHANGE)) {
            return;
        }

        var player = event.getPlayer();
        if (synchronizeManager.isRestoring(player)) {
            // 如果该玩家正在恢复数据, 则跳过
            log.config("玩家 %s 恢复数据中, 此次「切换游戏模式」不会创建快照".formatted(player.getName()));
            return;
        }

        snapshotManager.create(player, SnapshotCause.PLAYER_GAME_MODE_CHANGE);
    }

}
