package io.github.hello09x.onesync.listener;

import io.github.hello09x.onesync.Main;
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

public class SnapshotListener implements Listener {

    public final static SnapshotListener instance = new SnapshotListener();

    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotManager snapshotManager = SnapshotManager.instance;
    private final SynchronizeManager synchronizeManager = SynchronizeManager.instance;

    private final OneSyncConfig config = OneSyncConfig.instance;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSaveWorld(@NotNull WorldSaveEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.WORLD_SAVE)) {
            return;
        }

        if (!Bukkit.getWorlds().get(0).equals(event.getWorld())) {
            return;
        }

        snapshotManager.createForAll(SnapshotCause.WORLD_SAVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (!config.getSnapshot().getWhen().contains(SnapshotCause.PLAYER_DEATH)) {
            return;
        }

        var player = event.getPlayer();
        if (synchronizeManager.shouldNotSaveSnapshot(player.getUniqueId())) {
            // 如果该玩家正在恢复数据中, 则跳过
            log.warning("玩家 %s 正在恢复数据, 此时「死亡」不会创建快照".formatted(player.getName()));
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
        if (synchronizeManager.shouldNotSaveSnapshot(player.getUniqueId())) {
            // 如果该玩家正在恢复数据, 则跳过
            log.warning("玩家 %s 正在恢复数据, 此次「切换游戏模式」不会创建快照".formatted(player.getName()));
            return;
        }

        snapshotManager.create(player, SnapshotCause.PLAYER_GAME_MODE_CHANGE);
    }

}
