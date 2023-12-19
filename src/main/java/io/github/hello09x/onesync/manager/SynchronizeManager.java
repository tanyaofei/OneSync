package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.LockingRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class SynchronizeManager {

    public final static SynchronizeManager instance = new SynchronizeManager();
    private final LockingRepository synchronizer = LockingRepository.instance;
    private final SnapshotManager snapshotManager = SnapshotManager.instance;

    private final Map<UUID, List<PreparedSnapshot>> prepared = new ConcurrentHashMap<>();

    private final static Logger log = Main.getInstance().getLogger();

    /**
     * 玩家登陆游戏时提前准备好数据
     *
     * @param playerId 玩家 ID
     */
    public void prepare(@NotNull UUID playerId, long timeout) throws TimeoutException {
        long time = System.currentTimeMillis();
        while (synchronizer.isLocked(playerId)) {
            if (System.currentTimeMillis() - time >= timeout) {
                throw new TimeoutException("timeout");
            }
        }

        var snapshots = new ArrayList<PreparedSnapshot>(SnapshotHandler.HANDLERS.size());
        for (var handler : SnapshotHandler.HANDLERS) {
            if (Bukkit.getPluginManager().isPluginEnabled(handler.plugin())) {
                continue;
            }

            var snapshot = handler.getLatest(playerId);
            if (snapshot == null) {
                continue;
            }

            try {
                snapshots.add(new PreparedSnapshot(
                        (SnapshotHandler<Object>) handler,
                        handler.getLatest(playerId)
                ));
            } catch (Throwable e) {
                log.severe("准备 %s 的「%s」数据失败: %s".formatted(playerId, handler.snapshotType(), e.getMessage()));
                throw e;
            }
        }

        this.prepared.put(playerId, snapshots);
    }

    public void applyPrepared(@NotNull Player player) {
        var snapshots = this.prepared.get(player.getUniqueId());
        if (snapshots == null) {
            throw new IllegalStateException("No prepared snapshots for player: " + player.getName() + "(" + player.getUniqueId() + ")");
        }

        try {
            for (var s : snapshots) {
                try {
                    s.apply(player);
                } catch (Throwable e) {
                    log.severe("恢复 %s 的「%s」数据失败: %s".formatted(player.getName(), player.getUniqueId(), e.getMessage()));
                    throw e;
                }
            }

            this.synchronizer.setLock(player.getUniqueId(), true);  // 锁定玩家, 当玩家退出游戏时才解锁
        } finally {
            this.prepared.remove(player.getUniqueId());
        }
    }

    public void save(@NotNull Player player, @NotNull SnapshotCause cause) {
        try {
            snapshotManager.create(player, cause);
        } finally {
            synchronizer.setLock(player.getUniqueId(), false);
        }
    }

    public void saveAll(@NotNull SnapshotCause cause) {
        var players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        for (var player : players) {
            try {
                this.save(player, cause);
            } catch (Throwable e) {
                log.severe("保存玩家 %s(%s) 数据失败: %s".formatted(player.getName(), player.getUniqueId(), Throwables.getStackTraceAsString(e)));
            }
        }
        stopwatch.stop();
        log.info("[%s] 保存 %d 名玩家数据完毕, 耗时 %dms".formatted(cause, players.size(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

    public record PreparedSnapshot(

            @NotNull
            SnapshotHandler<Object> handler,

            @Nullable
            Object snapshot

    ) {

        public boolean apply(@NotNull Player player) {
            var snapshot = this.snapshot;
            if (snapshot == null) {
                return false;
            }

            if (!Bukkit.getPluginManager().isPluginEnabled(handler().plugin())) {
                return false;
            }

            this.handler.apply(player, snapshot);
            return true;
        }

    }


}
