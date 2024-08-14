package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ServerUtils;
import io.github.hello09x.devtools.core.utils.SingletonSupplier;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import io.github.hello09x.onesync.repository.model.Snapshot;
import net.kyori.adventure.util.Ticks;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
public class SnapshotManager {

    private final static Logger log = OneSync.getInstance().getLogger();
    private final SnapshotRepository repository;
    private final BatonManager batonManager;
    private final SingletonSupplier<SynchronizeManager> synchronizeManager = new SingletonSupplier<>(() -> OneSync.getInjector().getInstance(SynchronizeManager.class));
    private final OneSyncConfig.SnapshotConfig config;

    private int periodicals = 0;

    @Inject
    public SnapshotManager(SnapshotRepository repository, BatonManager batonManager, OneSyncConfig config) {
        this.repository = repository;
        this.batonManager = batonManager;
        this.config = config.getSnapshot();

        // 定时保存策略
        Runnable doSavePeriodical = () -> {
            if (!this.config.getWhen().contains(SnapshotCause.PERIODICAL)) {
                return;
            }

            // 0x3 意味最多分 4 批 0 ~ 3
            int flag = (periodicals++) & 0x3;
            var players = Bukkit.getOnlinePlayers().stream().filter(p -> (p.getUniqueId().getLeastSignificantBits() & 0x3) == flag).toList();
            if (players.isEmpty()) {
                return;
            }

            this.create(players, SnapshotCause.PERIODICAL);
        };

        if (ServerUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(OneSync.getInstance(), ignored -> doSavePeriodical.run(), Ticks.TICKS_PER_SECOND * 5 * 60, Ticks.TICKS_PER_SECOND * 5 * 60);
        } else {
            Bukkit.getScheduler().runTaskTimer(OneSync.getInstance(), doSavePeriodical, Ticks.TICKS_PER_SECOND * 5 * 60, Ticks.TICKS_PER_SECOND * 5 * 60);
        }
    }

    /**
     * 创建快照, 如果该玩家正在登陆, 并未恢复完数据, 则不会为他创建快照
     *
     * @param player 玩家
     * @param cause  创建原因
     * @return 快照 ID
     */
    public @NotNull Long create(@NotNull Player player, @NotNull SnapshotCause cause) {
        if (synchronizeManager.get().isRestoring(player)) {
            throw new IllegalStateException("%s has a prepared snapshot that hasn't be used to restore, cannot create snapshot for him".formatted(player.getName()));
        }

        var snapshotId = repository.insert(new Snapshot(
                null,
                player.getUniqueId(),
                cause,
                LocalDateTime.now()
        ));

        for (var registration : SnapshotHandler.getRegistrations()) {
            var handler = registration.getProvider();
            try {
                handler.save(
                        snapshotId,
                        player,
                        batonManager.get(player, handler.snapshotType().key())
                );
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

        this.wipeAsync(player.getUniqueId()).thenAcceptAsync(removed -> {
            if (removed > 0) {
                log.config("已清理 %s %d 份的多余快照".formatted(player.getName(), removed));
            }
        });
        return snapshotId;
    }

    /**
     * 为指定的玩家创建快照
     *
     * @param players 玩家
     * @param cause   创建原因
     */
    public int create(@NotNull Collection<? extends Player> players, @NotNull SnapshotCause cause) {
        var stopwatch = new StopWatch();
        stopwatch.start();
        int success = 0;
        for (var player : players) {
            if (synchronizeManager.get().isRestoring(player)) {
                continue;
            }

            try {
                this.create(player, cause);
                success++;
            } catch (Throwable e) {
                log.severe("[%s] - 保存 %s 快照失败: %s".formatted(
                        cause,
                        player.getName(),
                        Throwables.getStackTraceAsString(e)));
            }
        }
        stopwatch.stop();
        log.info("[%s] - 保存 %d 名玩家快照完毕, 耗时 %d ms".formatted(cause, players.size(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        return success;
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
     * @return 清理的份数
     */
    public @NotNull CompletableFuture<Integer> wipeAsync(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var snapshots = repository.selectByPlayerId(playerId);
                var exceeded = snapshots.size() - config.getCapacity();
                if (exceeded <= 0 || snapshots.size() <= 1) {   // 至少保留一份
                    return 0;
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

                if (!removing.isEmpty()) {
                    this.remove(removing.toArray(Long[]::new));
                }
                return removing.size();
            } catch (Throwable e) {
                log.severe("清理快照失败: " + Throwables.getStackTraceAsString(e));
                throw e;
            }
        });
    }

    public void remove(Long @NotNull ... ids) {
        if (ids.length == 0) {
            return;
        }

        if (ids.length == 1) {
            var id = ids[0];
            repository.deleteById(id);
            for (var handler : SnapshotHandler.getImpl()) {
                try {
                    handler.remove(Collections.singletonList(id));
                } catch (Throwable e) {
                    log.warning(Throwables.getStackTraceAsString(e));
                }
            }
        } else {
            var idList = Arrays.asList(ids);
            repository.deleteByIds(idList);
            for (var handler : SnapshotHandler.getImpl()) {
                try {
                    handler.remove(idList);
                } catch (Throwable e) {
                    log.warning(Throwables.getStackTraceAsString(e));
                }
            }
        }
    }
}
