package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.EnderChestSnapshotRepository;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import io.github.hello09x.onesync.util.InventoryHelper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnderChestSnapshotHandler implements SnapshotHandler<EnderChestSnapshot> {

    public final static EnderChestSnapshotHandler instance = new EnderChestSnapshotHandler();
    private final EnderChestSnapshotRepository repository = EnderChestSnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public @NotNull String snapshotType() {
        return "末影箱";
    }

    @Override
    public @Nullable EnderChestSnapshot getOne(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isEnderChest()) {
            return;
        }

        var snapshot = new EnderChestSnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryHelper.toMap(player.getEnderChest())
        );

        repository.insert(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
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
