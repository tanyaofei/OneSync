package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.InventorySnapshotRepository;
import io.github.hello09x.onesync.repository.model.InventorySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventorySnapshotHandler implements SnapshotHandler<InventorySnapshot> {

    private final InventorySnapshotRepository repository = InventorySnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "Inventory";
    }

    @Override
    public @NotNull Plugin plugin() {
        return Main.getInstance();
    }

    @Override
    public @Nullable InventorySnapshot getLatest(@NotNull UUID playerId) {
        return repository.selectLatestByPlayerId(playerId);
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
                asMap(player.getEnderChest())
        );

        repository.insert(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void remove(@NotNull Long snapshotId) {
        repository.deleteById(snapshotId);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull InventorySnapshot snapshot) {
        if (!config.isInventory()) {
            return;
        }

        var inv = player.getInventory();
        for (var entry : snapshot.items().entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue());
        }

        var chest = player.getEnderChest();
        for (var entry : snapshot.enderItems().entrySet()) {
            chest.setItem(entry.getKey(), entry.getValue());
        }

    }

    private static @NotNull Map<Integer, ItemStack> asMap(@NotNull Inventory inventory) {
        var items = new HashMap<Integer, ItemStack>(inventory.getSize(), 1.0F);
        var itr = inventory.iterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var item = itr.next();
            items.put(i, item);
        }
        return items;
    }

}
