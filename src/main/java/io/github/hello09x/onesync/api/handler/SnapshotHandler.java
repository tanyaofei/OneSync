package io.github.hello09x.onesync.api.handler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

public interface SnapshotHandler<T> {

    @NotNull
    List<? extends SnapshotHandler<?>> HANDLERS = ServiceLoader
            .load(SnapshotHandler.class, SnapshotHandler.class.getClassLoader())
            .stream()
            .map(provider -> (SnapshotHandler<?>) provider.get())
            .toList();

    /**
     * @return 快照名称
     */
    @NotNull String snapshotType();

    /**
     * @return 插件
     */
    @NotNull Plugin plugin();

    /**
     * 获取玩家最新的快照
     *
     * @param playerId 玩家 ID
     * @return 快照
     */
    @Nullable T getLatest(@NotNull UUID playerId);

    /**
     * 保存玩家数据
     *
     * @param player 玩家
     */
    void save(@NotNull Long snapshotId, @NotNull Player player);

    /**
     * 删除
     *
     * @param snapshotIds 快照 ID
     */
    void remove(@NotNull List<Long> snapshotIds);

    /**
     * 删除
     *
     * @param snapshotId 快照 ID
     */
    void remove(@NotNull Long snapshotId);

    /**
     * 应用快照
     *
     * @param player   玩家
     * @param snapshot 快照
     */
    void apply(@NotNull Player player, @NotNull T snapshot);

}
