package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

        for (var handler : SnapshotHandler.HANDLERS) {
            try {
                handler.save(snapshotId, player);
            } catch (Throwable e) {
                log.severe("[%s] 保存 %s(%s) 「%s」数据失败: %s".formatted(
                        cause,
                        player.getName(),
                        player.getUniqueId(),
                        handler.snapshotType(),
                        Throwables.getStackTraceAsString(e)
                ));
            }
        }

        this.wipeAsync(player.getUniqueId());
    }

    /**
     * 根据配置文件配置的快照数据, 清除多余或者过期的快照
     *
     * @param playerId 玩家 ID
     * @return 任务
     */
    public CompletableFuture<Void> wipeAsync(@NotNull UUID playerId) {
        return CompletableFuture.runAsync(() -> {
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
            for (var handler : SnapshotHandler.HANDLERS) {
                if (!handler.plugin().isEnabled()) {
                    continue;
                }

                handler.remove(removing);
            }
        });
    }

    /**
     * 为所有在线玩家创建快照
     *
     * @param cause 创建原因
     */
    public void createAll(@NotNull SnapshotCause cause) {
        var players = Bukkit.getOnlinePlayers();
        var stopwatch = new StopWatch();
        stopwatch.start();
        for (var player : players) {
            try {
                this.create(player, cause);
            } catch (Throwable e) {
                log.severe("[%s] 保存 %s(%s) 数据失败: %s".formatted(
                        cause,
                        player.getName(),
                        player.getUniqueId(),
                        Throwables.getStackTraceAsString(e)));
            }
        }
        stopwatch.stop();
        log.info("[%s] 保存 %d 名玩家数据完毕, 耗时 %dms".formatted(cause, players.size(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }


}
