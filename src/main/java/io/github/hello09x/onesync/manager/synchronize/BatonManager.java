package io.github.hello09x.onesync.manager.synchronize;

import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ServerUtils;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class BatonManager implements Listener {

    private final Map<Player, Map<String, SnapshotComponent>> PLAYER_BATONS = ServerUtils.isFolia() ? new ConcurrentHashMap<>() : new HashMap<>();

    public void set(@NotNull Player player, @NotNull String key, @NotNull SnapshotComponent component) {
        PLAYER_BATONS.computeIfAbsent(player, p -> new HashMap<>()).put(key, component);
    }

    public @Nullable SnapshotComponent get(@NotNull Player player, @NotNull String key) {
        return Optional
                .ofNullable(PLAYER_BATONS.get(player))
                .map(batons -> batons.get(key))
                .orElse(null);
    }

    public void remove(@NotNull Player player) {
        PLAYER_BATONS.remove(player);
    }

}
