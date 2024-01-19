package io.github.hello09x.onesync.api.handler;

import io.github.hello09x.onesync.repository.ExtensionSnapshotRepository;
import io.github.hello09x.onesync.repository.model.ExtensionSnapshot;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ExtensionSnapshotHandler<T extends SnapshotComponent> implements SnapshotHandler<T> {


    protected final NamespacedKey type;
    private final ExtensionSnapshotRepository repository = ExtensionSnapshotRepository.instance;

    public ExtensionSnapshotHandler(@NotNull NamespacedKey type) {
        this.type = type;
    }

    /**
     * 从玩家中获取数据, 序列化成字节数组
     *
     * @param player 玩家
     * @return 序列化后的字节数组
     */
    public abstract byte @NotNull [] serialize(@NotNull Player player);

    /**
     * 反序列化快照数据
     *
     * @param snapshot 序列化后快照数据
     * @return 反序列化后的快照数据
     */
    public abstract @NotNull T deserialize(@NotNull ExtensionSnapshot snapshot);

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player, @Nullable T baton) {
        var snapshot = new ExtensionSnapshot(
                null,
                snapshotId,
                player.getUniqueId(),
                this.type,
                this.serialize(player)
        );
        repository.insert(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteBySnapshotIdsAndType(snapshotIds, this.type);
    }


    @Override
    public @Nullable T getOne(@NotNull Long snapshotId) {
        var snapshot = this.repository.selectBySnapshotIdAndType(snapshotId, this.type);
        if (snapshot == null) {
            return null;
        }
        return this.deserialize(snapshot);
    }
}
