package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.bedrock.util.InventoryUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class InventorySnapshotHandler extends CacheableSnapshotHandler<InventorySnapshot> {

    public final static InventorySnapshotHandler instance = new InventorySnapshotHandler();
    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.inventory",
            "背包"
    );
    private final static Logger log = Main.getInstance().getLogger();

    private final InventorySnapshotRepository repository = InventorySnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable InventorySnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
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
        repository.deleteByIds(snapshotIds);
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
