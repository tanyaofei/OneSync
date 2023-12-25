package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.PDCSnapshotRepository;
import io.github.hello09x.onesync.repository.model.PDCSnapshot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class PDCSnapshotHandler extends CacheableSnapshotHandler<PDCSnapshot> {

    public final static PDCSnapshotHandler instance = new PDCSnapshotHandler();
    private final PDCSnapshotRepository repository = PDCSnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "PDC";
    }

    @Override
    public @Nullable PDCSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable PDCSnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isPdc()) {
            return null;
        }

        byte[] data;
        try {
            data = player.getPersistentDataContainer().serializeToBytes();
        } catch (IOException e) {
            throw new Error(e);
        }

        var snapshot = new PDCSnapshot(
                snapshotId,
                player.getUniqueId(),
                data
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull PDCSnapshot snapshot, boolean force) {
        if (!config.isPdc() && !force) {
            return;
        }

        try {
            player.getPersistentDataContainer().readFromBytes(snapshot.data(), true);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
