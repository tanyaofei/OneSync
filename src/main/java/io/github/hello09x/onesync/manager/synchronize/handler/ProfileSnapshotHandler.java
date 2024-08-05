package io.github.hello09x.onesync.manager.synchronize.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.ProfileSnapshotRepository;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class ProfileSnapshotHandler extends CacheableSnapshotHandler<ProfileSnapshot> {

    private final static SnapshotType TYPE = new SnapshotType(
            "onesync.snapshot.profile",
            "档案"
    );

    private final ProfileSnapshotRepository repository;
    private final OneSyncConfig.SynchronizeConfig config;

    @Inject
    public ProfileSnapshotHandler(ProfileSnapshotRepository repository, OneSyncConfig.SynchronizeConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable ProfileSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectBySnapshotId(snapshotId);
    }

    private static <V> @Nullable V getOrHandover(
            @Nullable V fromPlayer,
            @Nullable ProfileSnapshot baton,
            @NotNull Function<ProfileSnapshot, V> fromBaton,
            @NotNull Enabled enabled
    ) {
        if (enabled == Enabled.FALSE) {
            return null;
        }
        if (enabled == Enabled.ISOLATED) {
            return Optional.ofNullable(baton).map(fromBaton).orElse(null);
        }
        return fromPlayer;
    }

    private static @Nullable Double getMaxHealth(@NotNull Player player) {
        return Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).map(AttributeInstance::getBaseValue).orElse(null);
    }

    @Override
    public @Nullable ProfileSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable ProfileSnapshot baton) {
        var gameMode = getOrHandover(player.getGameMode(), baton, ProfileSnapshot::gameMode, config.getGameMode());
        var op = getOrHandover(player.isOp(), baton, ProfileSnapshot::op, config.getOp());

        var level = getOrHandover(player.getLevel(), baton, ProfileSnapshot::level, config.getExp());
        var exp = getOrHandover(player.getExp(), baton, ProfileSnapshot::exp, config.getExp());

        var health = getOrHandover(player.getHealth(), baton, ProfileSnapshot::health, config.getHealth());
        var maxHealth = getOrHandover(getMaxHealth(player), baton, ProfileSnapshot::maxHealth, config.getHealth());

        var foodLevel = getOrHandover(player.getFoodLevel(), baton, ProfileSnapshot::foodLevel, config.getFood());
        var saturation = getOrHandover(player.getSaturation(), baton, ProfileSnapshot::saturation, config.getFood());
        var exhaustion = getOrHandover(player.getExhaustion(), baton, ProfileSnapshot::exhaustion, config.getFood());

        var remainingAir = getOrHandover(player.getRemainingAir(), baton, ProfileSnapshot::remainingAir, config.getAir());

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
        repository.deleteBySnapshotIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull ProfileSnapshot snapshot) {
        if (config.getGameMode() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.gameMode()).ifPresent(player::setGameMode);
        }
        if (config.getOp() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.op()).ifPresent(player::setOp);
        }
        if (config.getExp() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.level()).ifPresent(player::setLevel);
            Optional.ofNullable(snapshot.exp()).ifPresent(player::setExp);
        }
        if (config.getHealth() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.health()).ifPresent(player::setHealth);
            Optional.ofNullable(snapshot.maxHealth()).ifPresent(maxHealth -> {
                Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(attr -> attr.setBaseValue(maxHealth));
            });
        }
        if (config.getFood() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.foodLevel()).ifPresent(player::setFoodLevel);
            Optional.ofNullable(snapshot.saturation()).ifPresent(player::setSaturation);
            Optional.ofNullable(snapshot.exhaustion()).ifPresent(player::setExhaustion);
        }
        if (config.getAir() == Enabled.TRUE) {
            Optional.ofNullable(snapshot.remainingAir()).ifPresent(player::setRemainingAir);
        }
        return true;
    }

}
