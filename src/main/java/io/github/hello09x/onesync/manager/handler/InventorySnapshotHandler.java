package io.github.hello09x.onesync.manager.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.InventoryUtils;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.entity.SnapshotType;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
public class InventorySnapshotHandler extends CacheableSnapshotHandler<InventorySnapshot> {

    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.inventory",
            "背包"
    );

    private final static Logger log = OneSync.getInstance().getLogger();

    private final InventorySnapshotRepository repository;
    private final OneSyncConfig.SynchronizeConfig config;

    @Inject
    public InventorySnapshotHandler(InventorySnapshotRepository repository, OneSyncConfig.SynchronizeConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable InventorySnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectBySnapshotId(snapshotId);
    }

    @Override
    public @Nullable InventorySnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable InventorySnapshot baton) {
        if (config.getInventory() == Enabled.FALSE) {
            return null;
        }
        if (config.getInventory() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new InventorySnapshot(snapshotId, baton.playerId(), baton.items(), baton.heldItemSlot());
                repository.insert(snapshot);
                return snapshot;
            }
            return null;
        }

        var snapshot = new InventorySnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryUtils.toMap(player.getInventory()),
                player.getInventory().getHeldItemSlot()
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteBySnapshotIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull InventorySnapshot snapshot) {
        if (config.getInventory() != Enabled.TRUE) {
            return false;
        }

        InventoryUtils.replace(player.getInventory(), snapshot.items(), true);
        player.getInventory().setHeldItemSlot(snapshot.heldItemSlot());
        return true;
    }

    public void updateItems(@NotNull Long snapshotId, @NotNull Map<Integer, ItemStack> items) {
        repository.updateItemsBySnapshotId(snapshotId, items);
        this.invalidate();
    }

}
