package io.github.hello09x.onesync.manager.impl;

import io.github.hello09x.onesync.manager.SynchronizeHandler;
import io.github.hello09x.onesync.manager.SynchronizedData;
import io.github.hello09x.onesync.repository.HealthRepository;
import io.github.hello09x.onesync.repository.model.Health;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HealthSynchronizeHandler implements SynchronizeHandler<SynchronizedData.Health> {

    public final static HealthSynchronizeHandler instance = new HealthSynchronizeHandler();

    private final HealthRepository repository = HealthRepository.instance;


    @Override
    public @Nullable SynchronizedData.Health load(@NotNull UUID uuid) {
        return Optional.ofNullable(repository.selectById(uuid))
                .map(health -> new SynchronizedData.Health(health.health(), health.maxHealth()))
                .orElse(null);
    }

    @Override
    public boolean save(@NotNull Player player, boolean clean) {
        var health = new Health(
                player.getUniqueId(),
                player.getHealth(),
                Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                        .map(AttributeInstance::getValue)
                        .orElse(20D)
        );

        return repository.saveOrUpdate(health) > 0;
    }

    @Override
    public void apply(@NotNull Player player, SynchronizedData.@NotNull Health data) {

    }
}
