package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.CachedSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.InventoryHelper;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class InventorySnapshotHandler extends CachedSnapshotHandler<InventorySnapshot> {

    public final static InventorySnapshotHandler instance = new InventorySnapshotHandler();
    private final static Logger log = Main.getInstance().getLogger();

    private final InventorySnapshotRepository repository = InventorySnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "背包";
    }

    @Override
    @SneakyThrows
    public @Nullable InventorySnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable InventorySnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isInventory()) {
            return null;
        }

        var snapshot = new InventorySnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryHelper.toMap(player.getInventory()),
                player.getInventory().getHeldItemSlot()
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull InventorySnapshot snapshot, boolean force) {
        if (!config.isInventory() && !force) {
            return;
        }

        InventoryHelper.replace(player.getInventory(), snapshot.items());
        player.getInventory().setHeldItemSlot(snapshot.heldItemSlot());
    }

}
