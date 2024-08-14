package io.github.hello09x.onesync.api.handler;

import io.github.hello09x.onesync.manager.entity.SnapshotType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface SnapshotHandler<T extends SnapshotComponent> {

    @NotNull
    @SuppressWarnings("rawtypes")
    static Collection<RegisteredServiceProvider<SnapshotHandler>> getRegistrations() {
        return Bukkit.getServicesManager().getRegistrations(SnapshotHandler.class);
    }

    @SuppressWarnings("unchecked")
    static <T extends SnapshotComponent> Collection<SnapshotHandler<T>> getImpl() {
        return Bukkit.getServicesManager().getRegistrations(SnapshotHandler.class)
                .stream()
                .map(RegisteredServiceProvider::getProvider)
                .map(handler -> (SnapshotHandler<T>) handler)
                .toList();
    }

    /**
     * @return 快照名称
     */
    @NotNull SnapshotType snapshotType();

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
    void save(@NotNull Long snapshotId, @NotNull Player player, @Nullable T initial);

    /**
     * 删除
     * <p>如果对应的快照不存在, 应当当作成功直接返回</p>
     *
     * @param snapshotIds 快照 ID
     */
    void remove(@NotNull List<Long> snapshotIds);

    /**
     * 应用快照
     *
     * @param player   玩家
     * @param snapshot 快照
     * @return 是否应当应用快照
     */
    boolean apply(@NotNull Player player, @NotNull T snapshot);

    /**
     * 应用快照
     *
     * @param player   玩家
     * @param snapshot 快照
     */
    @SuppressWarnings("unchecked")
    default boolean applyUnsafe(@NotNull Player player, SnapshotComponent snapshot) {
        return this.apply(player, (T) snapshot);
    }


}
