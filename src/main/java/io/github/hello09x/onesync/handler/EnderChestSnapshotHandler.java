package io.github.hello09x.onesync.handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.EnderChestSnapshotRepository;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import io.github.hello09x.onesync.util.InventoryHelper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class EnderChestSnapshotHandler implements SnapshotHandler<EnderChestSnapshot> {

    public final static EnderChestSnapshotHandler instance = new EnderChestSnapshotHandler();
    private final EnderChestSnapshotRepository repository = EnderChestSnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    private final Mutable<EnderChestSnapshot> theLast = new MutableObject<>();

    @Override
    public @NotNull String snapshotType() {
        return "末影箱";
    }

    @Override
    @SneakyThrows
    public @Nullable EnderChestSnapshot getOne(@NotNull Long snapshotId) {
        return Optional
                .ofNullable(theLast.getValue())
                .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
                .orElseGet(() -> repository.selectById(snapshotId));
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
        theLast.setValue(snapshot);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
        theLast.setValue(null);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull EnderChestSnapshot snapshot, boolean force) {
        if (!config.isEnderChest()) {
            return;
        }

        InventoryHelper.replace(player.getEnderChest(), snapshot.items());
    }
}
