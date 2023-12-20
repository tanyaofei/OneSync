package io.github.hello09x.onesync.api.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

public interface SnapshotHandler<T extends SnapshotComponent> {

    @NotNull
    List<? extends SnapshotHandler<? extends SnapshotComponent>> HANDLERS = ServiceLoader
            .load(SnapshotHandler.class, SnapshotHandler.class.getClassLoader())
            .stream()
            .map(provider -> (SnapshotHandler<? extends SnapshotComponent>) provider.get())
            .toList();

    @NotNull
    static Collection<? extends SnapshotHandler<? extends SnapshotComponent>> getImpl() {
        return Bukkit
                .getServicesManager()
                .getRegistrations(SnapshotHandler.class)
                .stream()
                .map(RegisteredServiceProvider::getProvider)
                .map(impl -> (SnapshotHandler<? extends SnapshotComponent>) impl)
                .toList();
    }

    /**
     * @return 快照名称
     */
    @NotNull String snapshotType();

    /**
     * 获取玩家最新的快照
     *
     * @param playerId 玩家 ID
     * @return 快照
     */
    @Nullable T getLatest(@NotNull UUID playerId);

    /**
     * 根据快照 ID 获取快照
     *
     * @param snapshotId 快照 ID
     * @return 快照
     */
    @Nullable T getOne(@NotNull Long snapshotId);

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
    boolean apply(@NotNull Player player, @NotNull T snapshot);

    default boolean applyUnsafe(@NotNull Player player, SnapshotComponent snapshot) {
        return this.apply(player, (T) snapshot);
    }

}
