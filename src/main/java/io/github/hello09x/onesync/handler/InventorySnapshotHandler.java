package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import io.github.hello09x.onesync.util.InventoryHelper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class InventorySnapshotHandler implements SnapshotHandler<InventorySnapshot> {

    public final static InventorySnapshotHandler instance = new InventorySnapshotHandler();
    private final static Logger log = Main.getInstance().getLogger();

    private final InventorySnapshotRepository repository = InventorySnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    private final Mutable<InventorySnapshot> theLast = new MutableObject<>();

    @Override
    public @NotNull String snapshotType() {
        return "背包";
    }

    @Override
    @SneakyThrows
    public @Nullable InventorySnapshot getOne(@NotNull Long snapshotId) {
        return Optional
                .ofNullable(theLast.getValue())
                .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
                .orElseGet(() -> repository.selectById(snapshotId));
    }

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isInventory()) {
            return;
        }

        var snapshot = new InventorySnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryHelper.toMap(player.getInventory()),
                player.getInventory().getHeldItemSlot()
        );

        repository.insert(snapshot);
        theLast.setValue(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
        theLast.setValue(null);
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
