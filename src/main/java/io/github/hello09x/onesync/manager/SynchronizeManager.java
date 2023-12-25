package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;

public class SynchronizeManager {

    public final static SynchronizeManager instance = new SynchronizeManager();
    private final static Logger log = Main.getInstance().getLogger();
    private final LockingManager lockingManager = LockingManager.instance;
    private final SnapshotManager snapshotManager = SnapshotManager.instance;
    private final Map<UUID, List<PreparedSnapshot>> prepared = new ConcurrentHashMap<>();

    /**
     * 判断玩家是否已经恢复了数据
     *
     * @param player 玩家
     * @return 是否已经恢复了数据
     */
    public boolean isRestored(@NotNull Player player) {
        return player.hasMetadata("onesync:restored");
    }

    public void setRestored(@NotNull Player player, boolean restored) {
        if (restored) {
            player.setMetadata("onesync:restored", new FixedMetadataValue(Main.getInstance(), true));
        } else {
            player.removeMetadata("onesync:restored", Main.getInstance());
        }
    }

    /**
     * 玩家登陆游戏时提前加载好数据
     *
     * @param playerId   玩家 ID
     * @param playerName 玩家名称
     * @param timeout    超时时间
     * @return 加载数据成功则返回 {@code true}, 否则在玩家 {@link #applyPreparedOrKick(Player)} 时踢掉玩家并返回 {@code false}
     */
    public @Nullable String tryPrepare(@NotNull UUID playerId, @Nullable String playerName, long timeout) {
        try {
            this.prepared.remove(playerId);
            long startedAt = System.currentTimeMillis();
            while (lockingManager.isLocked(playerId)) {
                if (System.currentTimeMillis() - startedAt >= timeout) {
                    return "[OneSync] 加载数据超时, 请联系管理员";
                }
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                    return "[OneSync] 加载数据中断, 请联系管理员";
                }
            }

            var snapshot = snapshotManager.getLatest(playerId);
            if (snapshot == null) {
                this.prepared.put(playerId, Collections.emptyList());
                return null;
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
                    log.severe("加载 %s(%s) 由 [%s] 提供的「%s」数据失败: %s".formatted(
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
            return null;
        } catch (Throwable e) {
            return "[OneSync] 加载数据失败, 请联系管理员";
        }
    }

    /**
     * 应用已经加载好的快照数据
     *
     * @param player 玩家
     * @return 玩家数据加载成功则为 {@code true}, 否则踢掉玩家并返回 {@code false}
     */
    public boolean applyPreparedOrKick(@NotNull Player player) {
        try {
            this.setRestored(player, false);
            var prepared = this.prepared.remove(player.getUniqueId());
            if (prepared == null) {
                // 兼容 fakeplayer, 同步加载数据
                var reason = this.tryPrepare(player.getUniqueId(), player.getName(), 0);
                if (reason != null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.kick(text(reason)));
                    return false;
                }
                prepared = this.prepared.remove(player.getUniqueId());
            }

            if (prepared == null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.kick(text("[OneSync] 服务器尚未为你加载数据, 请重新登录")));
                return false;
            }

            for (var pair : prepared) {
                var registration = pair.registration;
                var snapshot = pair.snapshot;
                var handler = registration.getProvider();

                if (snapshot == null) {
                    continue;
                }
                if (!registration.getPlugin().isEnabled()) {
                    log.severe("插件 [%s] 已卸载, 无法为 %s 恢复「%s」数据".formatted(player.getName(), registration.getPlugin().getName(), handler.snapshotType()));
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.kick(text("[OneSync] 玩家数据发生变化, 请重新登陆")));
                    return false;
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

            this.setRestored(player, true);     // 设置玩家已经恢复完毕, 其他创建快照事件才会处理他
            lockingManager.lock(player.getUniqueId());  // 锁定玩家, 当玩家退出游戏时才解锁
            return true;
        } catch (Throwable e) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.kick(text("[OneSync] 无法为你恢复玩家数据, 请联系管理员")));
            log.severe(Throwables.getStackTraceAsString(e));
            return false;
        }
    }


    /**
     * 保存玩家并解锁
     *
     * @param player 玩家
     * @param cause  保存原因
     */
    public void saveAndUnlock(@NotNull Player player, @NotNull SnapshotCause cause) {
        try {
            snapshotManager.create(player, cause);
        } finally {
            lockingManager.unlock(player);
        }
    }

    /**
     * 保存所有玩家并解锁
     *
     * @param cause 保存原因
     */
    public void saveAndUnlockAll(@NotNull SnapshotCause cause) {
        var players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        for (var player : players) {
            if (!this.isRestored(player)) {
                continue;
            }
            try {
                this.saveAndUnlock(player, cause);
            } catch (Throwable e) {
                // 因为玩家退出时调用此方法并不能取消操作, 因此只能继续执行
                log.severe("保存玩家 %s(%s) 数据失败: %s".formatted(player.getName(), player.getUniqueId(), Throwables.getStackTraceAsString(e)));
            }
        }
        stopwatch.stop();
        if (Main.isDebugging()) {
            log.info("[%s] 保存 %d 名玩家数据完毕, 耗时 %d ms".formatted(cause, players.size(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        }
    }

    public record PreparedSnapshot(

            @SuppressWarnings("rawtypes")
            RegisteredServiceProvider<SnapshotHandler> registration,

            @Nullable
            SnapshotComponent snapshot

    ) {

    }


}
