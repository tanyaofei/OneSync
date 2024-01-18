package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.bedrock.util.InventoryUtils;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
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

public class EnderChestSnapshotHandler extends CacheableSnapshotHandler<EnderChestSnapshot> {

    public final static EnderChestSnapshotHandler instance = new EnderChestSnapshotHandler();
    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.ender-chest",
            "末影箱"
    );
    private final EnderChestSnapshotRepository repository = EnderChestSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable EnderChestSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable EnderChestSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable EnderChestSnapshot initial) {
        if (!config.isEnderChest()) {
            if (initial != null) {
                var snapshot = new EnderChestSnapshot(snapshotId, initial.playerId(), initial.items());
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
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull EnderChestSnapshot snapshot) {
        if (!config.isEnderChest()) {
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
