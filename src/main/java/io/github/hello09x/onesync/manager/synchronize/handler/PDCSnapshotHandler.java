package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.PDCSnapshotRepository;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import io.github.hello09x.onesync.repository.model.PDCSnapshot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class PDCSnapshotHandler extends CacheableSnapshotHandler<PDCSnapshot> {

    public final static PDCSnapshotHandler instance = new PDCSnapshotHandler();
    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.pdc",
            "PDC"
    );
    private final PDCSnapshotRepository repository = PDCSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable PDCSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable PDCSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable PDCSnapshot baton) {
        if (config.getPdc() == Enabled.FALSE) {
            return null;
        }

        if (config.getPdc() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new PDCSnapshot(snapshotId, baton.playerId(), baton.data());
                repository.insert(snapshot);
                return snapshot;
            }
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
    public boolean apply(@NotNull Player player, @NotNull PDCSnapshot snapshot) {
        if (config.getPdc() != Enabled.TRUE) {
            return false;
        }

        try {
            player.getPersistentDataContainer().readFromBytes(snapshot.data(), true);
        } catch (IOException e) {
            throw new Error(e);
        }
        return true;
    }
}
