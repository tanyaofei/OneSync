package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.LockingRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
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
     * 判断玩家是否已经准备好了数据, 这同时意味着玩家只是登陆了还没加入数据, 当前的数据是脏的
     *
     * @param playerId 玩家 ID
     * @return 是否已经准备好了
     */
    public boolean isPrepared(@NotNull UUID playerId) {
        return this.prepared.containsKey(playerId);
    }

    /**
     * 玩家登陆游戏时提前准备好数据
     *
     * @param playerId 玩家 ID
     */
    public void prepare(@NotNull UUID playerId, @Nullable String playerName, long timeout) throws TimeoutException {
        this.prepared.remove(playerId);
        long startedAt = System.currentTimeMillis();
        while (synchronizer.isLocked(playerId)) {
            if (System.currentTimeMillis() - startedAt >= timeout) {
                throw new TimeoutException("Timeout after %dms".formatted(timeout));
            }
            try {
                Thread.sleep(startedAt / 10);
            } catch (InterruptedException ignored) {

            }
        }

        var snapshot = snapshotManager.getLatest(playerId);
        if (snapshot == null) {
            this.prepared.put(playerId, Collections.emptyList());
            return;
        }

        var snapshots = new ArrayList<PreparedSnapshot>();
        for (var registration : SnapshotHandler.getRegistrations()) {
            var handler = registration.getProvider();
            var component = handler.getOne(snapshot.id());
            if (component == null) {
                continue;
            }

            try {
                snapshots.add(new PreparedSnapshot(
                        registration,
                        component
                ));
            } catch (Throwable e) {
                log.severe("准备 %s(%s) 由 [%s] 提供的「%s」数据失败: %s".formatted(
                        playerName,
                        playerId,
                        registration.getPlugin().getName(),
                        handler.snapshotType(),
                        e.getMessage())
                );
                if (handler.important()) {
                    throw e;
                }
            }
        }

        this.prepared.put(playerId, snapshots);
    }

    public void applyPrepared(@NotNull Player player) {
        var pairs = this.prepared.get(player.getUniqueId());
        if (pairs == null) {
            throw new IllegalStateException("No prepared snapshots for player: " + player.getName() + "(" + player.getUniqueId() + ")");
        }

        try {
            for (var pair : pairs) {
                var snapshot = pair.snapshot;
                if (snapshot == null) {
                    continue;
                }
                try {
                    pair.registration.getProvider().applyUnsafe(player, snapshot);
                } catch (Throwable e) {
                    log.severe("恢复 %s 由 [%s] 提供的「%s」数据失败: %s".formatted(
                            player.getName(),
                            pair.registration.getPlugin().getName(),
                            player.getUniqueId(),
                            e.getMessage()
                    ));
                    if (pair.registration.getProvider().important()) {
                        throw e;
                    }
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

            @SuppressWarnings("rawtypes")
            RegisteredServiceProvider<SnapshotHandler> registration,

            @Nullable
            SnapshotComponent snapshot

    ) {

        public boolean apply(@NotNull Player player) {
            var snapshot = this.snapshot;
            if (snapshot == null) {
                return false;
            }

            this.registration.getProvider().applyUnsafe(player, snapshot);
            return true;
        }

    }


}
