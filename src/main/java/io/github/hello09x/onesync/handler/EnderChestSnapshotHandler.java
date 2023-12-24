package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.api.handler.CachedSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.EnderChestSnapshotRepository;
import io.github.hello09x.onesync.util.InventoryHelper;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnderChestSnapshotHandler extends CachedSnapshotHandler<EnderChestSnapshot> {

    public final static EnderChestSnapshotHandler instance = new EnderChestSnapshotHandler();
    private final EnderChestSnapshotRepository repository = EnderChestSnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "末影箱";
    }

    @Override
    @SneakyThrows
    public @Nullable EnderChestSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable EnderChestSnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isEnderChest()) {
            return null;
        }

        var snapshot = new EnderChestSnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryHelper.toMap(player.getEnderChest())
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull EnderChestSnapshot snapshot, boolean force) {
        if (!config.isEnderChest()) {
            return;
        }

        InventoryHelper.replace(player.getEnderChest(), snapshot.items());
    }
}
