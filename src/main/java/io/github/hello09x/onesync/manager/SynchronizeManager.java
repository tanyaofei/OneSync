package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
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
    private final LockingManager lockingManager = LockingManager.instance;
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
     * @param playerId   玩家 ID
     * @param playerName 玩家名称
     * @param timeout    超时时间
     */
    public void prepare(@NotNull UUID playerId, @Nullable String playerName, long timeout) throws TimeoutException {
        this.prepared.remove(playerId);
        long startedAt = System.currentTimeMillis();
        while (lockingManager.isLocked(playerId)) {
            if (System.currentTimeMillis() - startedAt >= timeout) {
                throw new TimeoutException("Timeout after %dms".formatted(timeout));
            }
            try {
                Thread.sleep(100);
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
            try {
                var component = handler.getOne(snapshot.id());
                if (component == null) {
                    continue;
                }
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
                throw e;
            }
        }

        this.prepared.put(playerId, snapshots);
    }

    /**
     * 应用已经准备好的快照数据
     *
     * @param player 玩家
     */
    public void applyPrepared(@NotNull Player player) {
        var pairs = this.prepared.get(player.getUniqueId());
        if (pairs == null) {
            throw new IllegalStateException("No prepared snapshots for player: " + player.getName() + "(" + player.getUniqueId() + ")");
        }

        for (var pair : pairs) {
            var registration = pair.registration;
            var snapshot = pair.snapshot;
            var handler = registration.getProvider();

            if (snapshot == null) {
                continue;
            }
            if (!registration.getPlugin().isEnabled()) {
                log.warning("插件 [%s] 已卸载, 无法为 %s 恢复它提供的「%s」数据".formatted(player.getName(), registration.getPlugin().getName(), handler.snapshotType()));
                continue;
            }

            try {
                handler.applyUnsafe(player, snapshot);
            } catch (Throwable e) {
                log.severe("恢复 %s 由 [%s] 提供的「%s」数据失败: %s".formatted(
                        player.getName(),
                        pair.registration.getPlugin().getName(),
                        player.getUniqueId(),
                        e.getMessage()
                ));
                throw e;
            }
        }

        lockingManager.lock(player.getUniqueId());  // 锁定玩家, 当玩家退出游戏时才解锁
        this.prepared.remove(player.getUniqueId()); // 如果发生异常了, 则不移除玩家, 玩家下线时如果还存在准备好的数据, 则不会触发保存
    }

    public void save(@NotNull Player player, @NotNull SnapshotCause cause) {
        try {
            snapshotManager.create(player, cause);
        } finally {
            lockingManager.unlock(player);
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
            if (this.isPrepared(player.getUniqueId())) {
                continue;
            }
            try {
                this.save(player, cause);
            } catch (Throwable e) {
                // 因为玩家退出时调用此方法并不能取消操作, 因此只能继续执行
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

    }


}
