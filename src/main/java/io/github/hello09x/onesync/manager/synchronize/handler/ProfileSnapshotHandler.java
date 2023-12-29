package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.ProfileSnapshotRepository;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ProfileSnapshotHandler extends CacheableSnapshotHandler<ProfileSnapshot> {

    public final static ProfileSnapshotHandler instance = new ProfileSnapshotHandler();

    private final ProfileSnapshotRepository repository = ProfileSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "档案";
    }

    @Override
    public @Nullable ProfileSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable ProfileSnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        var gameMode = config.isGameMode() ? player.getGameMode() : null;
        var op = config.isOp() ? player.isOp() : null;

        Integer level = null;
        Float exp = null;
        if (config.isExp()) {
            level = player.getLevel();
            exp = player.getExp();
        }

        Double health = null, maxHealth = null;
        if (config.isHealth()) {
            health = player.getHealth();
            maxHealth = Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).map(AttributeInstance::getBaseValue).orElse(null);
        }

        Integer foodLevel = null;
        Float saturation = null, exhaustion = null;
        if (config.isFood()) {
            foodLevel = player.getFoodLevel();
            saturation = player.getSaturation();
            exhaustion = player.getExhaustion();
        }

        Integer remainingAir = null;
        if (config.isAir()) {
            remainingAir = player.getRemainingAir();
        }

        var snapshot = new ProfileSnapshot(
                snapshotId,
                player.getUniqueId(),
                gameMode,
                op,
                level,
                exp,
                health,
                maxHealth,
                foodLevel,
                saturation,
                exhaustion,
                remainingAir
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull ProfileSnapshot snapshot, boolean force) {
        if (config.isGameMode() || force) {
            Optional.ofNullable(snapshot.gameMode()).ifPresent(player::setGameMode);
        }
        if (config.isOp() || force) {
            Optional.ofNullable(snapshot.op()).ifPresent(player::setOp);
        }
        if (config.isExp() || force) {
            Optional.ofNullable(snapshot.level()).ifPresent(player::setLevel);
            Optional.ofNullable(snapshot.exp()).ifPresent(player::setExp);
        }
        if (config.isHealth() || force) {
            Optional.ofNullable(snapshot.health()).ifPresent(player::setHealth);
            Optional.ofNullable(snapshot.maxHealth()).ifPresent(maxHealth -> {
                Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(attr -> attr.setBaseValue(maxHealth));
            });
        }
        if (config.isFood() || force) {
            Optional.ofNullable(snapshot.foodLevel()).ifPresent(player::setFoodLevel);
            Optional.ofNullable(snapshot.saturation()).ifPresent(player::setSaturation);
            Optional.ofNullable(snapshot.exhaustion()).ifPresent(player::setExhaustion);
        }
        if (config.isAir() || force) {
            Optional.ofNullable(snapshot.remainingAir()).ifPresent(player::setRemainingAir);
        }
    }

}
