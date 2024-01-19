package io.github.hello09x.onesync.api.handler;

import org.apache.commons.lang3.mutable.MutableObject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class CacheableSnapshotHandler<T extends SnapshotComponent> implements SnapshotHandler<T> {

    private final MutableObject<T> latest = new MutableObject<>();

    @Override
    public final void save(@NotNull Long snapshotId, @NotNull Player player, @Nullable T initial) {
        var snapshot = this.save0(snapshotId, player, initial);
        this.latest.setValue(snapshot);
    }

    @Override
    public final @Nullable T getOne(@NotNull Long snapshotId) {
        return Optional.ofNullable(this.latest.getValue())
                .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
                .orElseGet(() -> this.getOne0(snapshotId));
    }

    @Override
    public final void remove(@NotNull List<Long> snapshotIds) {
        this.remove0(snapshotIds);
        this.invalidate();
    }

    protected abstract @Nullable T save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable T baton);

    protected abstract @Nullable T getOne0(@NotNull Long snapshotId);

    protected abstract void remove0(@NotNull List<Long> snapshotIds);

    protected void invalidate() {
        this.latest.setValue(null);
    }

}
