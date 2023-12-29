package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.PotionEffectSnapshotRepository;
import io.github.hello09x.onesync.repository.model.PotionEffectSnapshot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectSnapshotHandler extends CacheableSnapshotHandler<PotionEffectSnapshot> {

    public final static PotionEffectSnapshotHandler instance = new PotionEffectSnapshotHandler();

    private final PotionEffectSnapshotRepository repository = PotionEffectSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "效果";
    }

    @Override
    public @Nullable PotionEffectSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable PotionEffectSnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isPotionEffects()) {
            return null;
        }

        var effects = player.getActivePotionEffects();
        var snapshot = new PotionEffectSnapshot(
                snapshotId,
                player.getUniqueId(),
                new ArrayList<>(effects)
        );
        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull PotionEffectSnapshot snapshot, boolean force) {
        if (!config.isPotionEffects()) {
            return;
        }

        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.addPotionEffects(snapshot.effects());
    }
}
