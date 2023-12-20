package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.ProfileSnapshotRepository;
import io.github.hello09x.onesync.repository.model.ProfileSnapshot;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProfileSnapshotHandler implements SnapshotHandler<ProfileSnapshot> {

    private final ProfileSnapshotRepository repository = ProfileSnapshotRepository.instance;
    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    @Override
    public @NotNull String snapshotType() {
        return "Profile";
    }

    @Override
    public @NotNull Plugin plugin() {
        return Main.getInstance();
    }

    @Override
    public @Nullable ProfileSnapshot getLatest(@NotNull UUID playerId) {
        return repository.selectLatestByPlayerId(playerId);
    }

    @Override
    public @Nullable ProfileSnapshot getOne(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player) {
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

        var profile = new ProfileSnapshot(
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
                exhaustion
        );

        repository.insert(profile);
    }

    @Override
    public void remove(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public void remove(@NotNull Long snapshotId) {
        repository.deleteById(snapshotId);
    }

    @Override
    public void apply(@NotNull Player player, @NotNull ProfileSnapshot snapshot) {
        if (config.isGameMode()) {
            Optional.ofNullable(snapshot.gameMode()).ifPresent(player::setGameMode);
        }
        if (config.isOp()) {
            Optional.ofNullable(snapshot.op()).ifPresent(player::setOp);
        }
        if (config.isExp()) {
            Optional.ofNullable(snapshot.level()).ifPresent(player::setLevel);
            Optional.ofNullable(snapshot.exp()).ifPresent(player::setExp);
        }
        if (config.isHealth()) {
            Optional.ofNullable(snapshot.health()).ifPresent(player::setHealth);
            Optional.ofNullable(snapshot.maxHealth()).ifPresent(maxHealth -> {
                Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(attr -> attr.setBaseValue(maxHealth));
            });
        }
        if (config.isFood()) {
            Optional.ofNullable(snapshot.foodLevel()).ifPresent(player::setFoodLevel);
            Optional.ofNullable(snapshot.saturation()).ifPresent(player::setSaturation);
            Optional.ofNullable(snapshot.exhaustion()).ifPresent(player::setExhaustion);
        }
    }

}
