package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SnapshotManager {

    public final static SnapshotManager instance = new SnapshotManager();
    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotRepository repository = SnapshotRepository.instance;
    private final OneSyncConfig.Snapshot config = OneSyncConfig.instance.getSnapshot();

    /**
     * 创建快照
     *
     * @param player 玩家
     * @param cause  创建原因
     */
    public void create(@NotNull Player player, @NotNull SnapshotCause cause) {
        var snapshotId = repository.insert(new Snapshot(
                null,
                player.getUniqueId(),
                cause,
                null
        ));

        for (var registration : SnapshotHandler.getRegistrations()) {
            try {
                registration.getProvider().save(snapshotId, player);
            } catch (Throwable e) {
                log.severe("[%s] - 保存 %s 由 [%s] 提供的「%s」快照失败: %s".formatted(
                        cause,
                        player.getName(),
                        registration.getPlugin().getName(),
                        registration.getProvider().snapshotType(),
                        Throwables.getStackTraceAsString(e)
                ));
            }
        }

        this.wipeAsync(player.getUniqueId());
    }

    /**
     * 为所有在线玩家创建快照
     *
     * @param cause 创建原因
     */
    public void createForAll(@NotNull SnapshotCause cause) {
        var players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        for (var player : players) {
            try {
                this.create(player, cause);
            } catch (Throwable e) {
                log.severe("[%s] - 保存 %s 快照失败: %s".formatted(
                        cause,
                        player.getName(),
                        Throwables.getStackTraceAsString(e)));
            }
        }
        stopwatch.stop();
        log.info("[%s] - 保存 %d 名玩家快照完毕, 耗时 %dms".formatted(cause, players.size(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

    /**
     * 获取玩家最新的快照
     *
     * @param playerId 玩家 ID
     * @return 最新的快照
     */
    public @Nullable Snapshot getLatest(@NotNull UUID playerId) {
        return repository.selectLatestByPlayerId(playerId);
    }

    /**
     * 根据配置文件配置的快照数据, 清除多余或者过期的快照
     *
     * @param playerId 玩家 ID
     * @return 任务
     */
    public CompletableFuture<Void> wipeAsync(@NotNull UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                var snapshots = repository.selectByPlayerId(playerId);
                var exceeded = snapshots.size() - config.getCapacity();
                if (exceeded <= 0) {
                    return;
                }

                var ordered = new LinkedHashMap<LocalDate, List<Snapshot>>(snapshots.size(), 1.0F);
                snapshots.sort(Comparator.comparing(Snapshot::createdAt));
                for (var snapshot : snapshots) {
                    ordered.computeIfAbsent(snapshot.createdAt().toLocalDate(), x -> new ArrayList<>()).add(snapshot);
                }

                var removing = new ArrayList<Long>(exceeded);
                var keepEarliest = LocalDate.now().minusDays(config.getKeepDays());
                for (var entry : ordered.entrySet()) {
                    if (exceeded <= 0) {
                        break;
                    }

                    var itr = entry.getValue().listIterator();
                    if (entry.getKey().isBefore(keepEarliest)) {
                        // 不需要至少保存一份
                        while (itr.hasNext() && exceeded > 0) {
                            removing.add(itr.next().id());
                            exceeded--;
                        }
                    } else {
                        // 至少保存一份
                        while (itr.hasNext() && exceeded > 0) {
                            var snapshot = itr.next();
                            if (!itr.hasNext()) {
                                // 当前是最后一份了, 保留下来
                                break;
                            }
                            removing.add(snapshot.id());
                            exceeded--;
                        }
                    }
                }

                repository.deleteByIds(removing);
                for (var registration : SnapshotHandler.getRegistrations()) {
                    try {
                        registration.getProvider().remove(removing);
                    } catch (Throwable e) {
                        var player = Bukkit.getOfflinePlayer(playerId);
                        log.severe("删除 %s 由 [%s] 提供的「%s」快照失败: %s".formatted(
                                player.getName(),
                                registration.getPlugin().getName(),
                                registration.getProvider().snapshotType(),
                                Throwables.getStackTraceAsString(e)
                        ));
                    }
                }
            } catch (Throwable e) {
                log.severe("删除快照失败: " + Throwables.getStackTraceAsString(e));
            }
        });
    }


}
