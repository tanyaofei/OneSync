package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class InventorySnapshotHandler implements SnapshotHandler<InventorySnapshot> {

    public final static InventorySnapshotHandler instance = new InventorySnapshotHandler();
    private final static Logger log = Main.getInstance().getLogger();

    private final InventorySnapshotRepository repository = InventorySnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public @NotNull String snapshotType() {
        return "背包/末影箱";
    }

    @Override
    public @Nullable InventorySnapshot getOne(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isInventory()) {
            return;
        }

        var snapshot = new InventorySnapshot(
                snapshotId,
                player.getUniqueId(),
                asMap(player.getInventory()),
                asMap(player.getEnderChest()),
                player.getInventory().getHeldItemSlot()
        );

        repository.insert(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull InventorySnapshot snapshot, boolean force) {
        this.applyInventory(player, snapshot, force);
        this.applyEnderChest(player, snapshot, force);
    }

    public void applyInventory(@NotNull Player player, @NotNull InventorySnapshot snapshot, boolean force) {
        if (!config.isInventory() && !force) {
            return;
        }
        this.apply(snapshot.items(), player.getInventory());
        player.getInventory().setHeldItemSlot(snapshot.heldItemSlot());
    }

    public void applyEnderChest(@NotNull Player player, @NotNull InventorySnapshot snapshot, boolean force) {
        if (!config.isInventory() && !force) {
            return;
        }
        this.apply(snapshot.enderItems(), player.getEnderChest());
    }

    public void apply(@NotNull Map<Integer, ItemStack> from, @NotNull Inventory to) {
        for (int i = to.getSize() - 1; i >= 0; i--) {
            to.setItem(i, from.get(i));
        }
    }

    private static @NotNull Map<Integer, ItemStack> asMap(@NotNull Inventory inventory) {
        var items = new HashMap<Integer, ItemStack>(inventory.getSize(), 1.0F);
        var itr = inventory.iterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var item = itr.next();
            if (item == null) {
                continue;
            }
            items.put(i, item);
        }
        return items;
    }

}
