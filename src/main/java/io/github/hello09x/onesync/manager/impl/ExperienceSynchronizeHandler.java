package io.github.hello09x.onesync.manager.impl;

import io.github.hello09x.bedrock.io.Experiences;
import io.github.hello09x.onesync.manager.SynchronizeHandler;
import io.github.hello09x.onesync.manager.SynchronizedData;
import io.github.hello09x.onesync.repository.ExperienceRepository;
import io.github.hello09x.onesync.repository.model.Experience;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExperienceSynchronizeHandler implements SynchronizeHandler<SynchronizedData.Experience> {

    public final static ExperienceSynchronizeHandler instance = new ExperienceSynchronizeHandler();
    private final ExperienceRepository repository = ExperienceRepository.instance;

    @Override
    public @Nullable SynchronizedData.Experience load(@NotNull UUID uuid) {
        return Optional.ofNullable(repository.selectById(uuid)).map(exp -> new SynchronizedData.Experience(
                        exp.level(),
                        exp.exp()
                ))
                .orElse(null);
    }

    @Override
    public boolean save(@NotNull Player player, boolean clean) {
        try {
            return repository.insertOrUpdate(new Experience(
                    player.getUniqueId(),
                    player.getLevel(),
                    player.getExp()
            )) > 0;
        } finally {
            if (clean) {
                player.setLevel(0);
                player.setExp(0);
            }
        }
    }

    @Override
    public void apply(@NotNull Player player, SynchronizedData.@NotNull Experience data) {
        var originalExp = Experiences.getExp(player);
        player.setLevel(data.level());
        player.setExp(data.exp());
        player.giveExp(originalExp);
    }
}
