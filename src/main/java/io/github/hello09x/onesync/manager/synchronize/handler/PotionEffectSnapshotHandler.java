package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.PotionEffectSnapshotRepository;
import io.github.hello09x.onesync.repository.model.PotionEffectSnapshot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectSnapshotHandler extends CacheableSnapshotHandler<PotionEffectSnapshot> {

    public final static PotionEffectSnapshotHandler instance = new PotionEffectSnapshotHandler();
    private final static SnapshotType TYPE = new SnapshotType(
            "onesync.snapshot.potion-effect",
            "效果"
    );

    private final PotionEffectSnapshotRepository repository = PotionEffectSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable PotionEffectSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable PotionEffectSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable PotionEffectSnapshot baton) {
        if (config.getPotionEffects() == Enabled.FALSE) {
            return null;
        }

        if (config.getPotionEffects() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new PotionEffectSnapshot(snapshotId, baton.playerId(), baton.effects());
                repository.insert(snapshot);
                return snapshot;
            }
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
    public boolean apply(@NotNull Player player, @NotNull PotionEffectSnapshot snapshot) {
        if (config.getPotionEffects() != Enabled.TRUE) {
            return false;
        }

        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.addPotionEffects(snapshot.effects());
        return true;
    }
}
