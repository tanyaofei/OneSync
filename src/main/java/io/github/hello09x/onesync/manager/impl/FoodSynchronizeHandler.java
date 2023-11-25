package io.github.hello09x.onesync.manager.impl;

import io.github.hello09x.onesync.manager.SynchronizeHandler;
import io.github.hello09x.onesync.manager.SynchronizedData;
import io.github.hello09x.onesync.repository.FoodRepository;
import io.github.hello09x.onesync.repository.model.Food;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FoodSynchronizeHandler implements SynchronizeHandler<SynchronizedData.Food> {

    public final static FoodSynchronizeHandler instance = new FoodSynchronizeHandler();
    private final FoodRepository repository = FoodRepository.instance;

    @Override
    public @Nullable SynchronizedData.Food load(@NotNull UUID uuid) {
        return Optional.ofNullable(repository.selectById(uuid))
                .map(food -> new SynchronizedData.Food(
                        food.level(),
                        food.saturation()
                        ))
                .orElse(null);
    }

    @Override
    public boolean save(@NotNull Player player, boolean clean) {
        return this.repository.insertOrUpdate(new Food(
                        player.getUniqueId(),
                        player.getFoodLevel(),
                        player.getSaturation()
                )
        ) > 0;
    }

    @Override
    public void apply(@NotNull Player player, @NotNull SynchronizedData.Food food) {
        player.setFoodLevel(food.level());
        player.setSaturation(food.saturation());
    }
}
