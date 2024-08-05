package io.github.hello09x.onesync.manager.synchronize.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.InventoryUtils;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.EnderChestSnapshotRepository;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Singleton
public class EnderChestSnapshotHandler extends CacheableSnapshotHandler<EnderChestSnapshot> {

    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.ender-chest",
            "末影箱"
    );

    private final EnderChestSnapshotRepository repository;
    private final OneSyncConfig.SynchronizeConfig config;

    @Inject
    public EnderChestSnapshotHandler(EnderChestSnapshotRepository repository, OneSyncConfig.SynchronizeConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable EnderChestSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectBySnapshotId(snapshotId);
    }

    @Override
    public @Nullable EnderChestSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable EnderChestSnapshot baton) {
        if (config.getEnderChest() == Enabled.FALSE) {
            return null;
        }
        if (config.getEnderChest() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new EnderChestSnapshot(snapshotId, baton.playerId(), baton.items());
                repository.insert(snapshot);
                return snapshot;
            }
            return null;
        }

        var snapshot = new EnderChestSnapshot(
                snapshotId,
                player.getUniqueId(),
                InventoryUtils.toMap(player.getEnderChest())
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteBySnapshotIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull EnderChestSnapshot snapshot) {
        if (config.getEnderChest() != Enabled.TRUE) {
            return false;
        }

        InventoryUtils.replace(player.getEnderChest(), snapshot.items(), true);
        return true;
    }

    public void updateItems(@NotNull Long snapshotId, @NotNull Map<Integer, ItemStack> items) {
        repository.updateItemsBySnapshotId(snapshotId, items);
        this.invalidate();
    }

}
