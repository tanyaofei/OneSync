package io.github.hello09x.onesync.manager;

import io.github.hello09x.onesync.repository.LockingRepository;
import io.github.hello09x.onesync.repository.model.Locking;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class LockingManager {

    public final static LockingManager instance = new LockingManager();

    private final LockingRepository repository = LockingRepository.instance;

    public boolean isLocked(@NotNull UUID uuid) {
        return Optional.ofNullable(repository.selectById(uuid)).map(Locking::locked).orElse(false);
    }

    public boolean setLock(@NotNull OfflinePlayer player, boolean locked) {
        return repository.insertOrUpdate(new Locking(
                player.getUniqueId(),
                locked
        )) > 0;
    }

    public boolean clear() {
        return repository.deleteAll() > 0;
    }


}
