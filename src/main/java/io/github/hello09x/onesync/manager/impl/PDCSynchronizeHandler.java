package io.github.hello09x.onesync.manager.impl;

import io.github.hello09x.onesync.manager.SynchronizeHandler;
import io.github.hello09x.onesync.repository.PDCRepository;
import io.github.hello09x.onesync.repository.model.PDC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

public class PDCSynchronizeHandler implements SynchronizeHandler<byte[]> {

    public final static PDCSynchronizeHandler instance = new PDCSynchronizeHandler();
    private final PDCRepository repository = PDCRepository.instance;

    @Override
    public byte @Nullable [] load(@NotNull UUID uuid) {
        return Optional.ofNullable(repository.selectById(uuid)).map(PDC::data).orElse(null);
    }

    @Override
    public boolean save(@NotNull Player player, boolean clean) {
        byte[] data;
        try {
            data = player.getPersistentDataContainer().serializeToBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            return repository.insertOrUpdate(new PDC(
                    player.getUniqueId(),
                    data,
                    null,
                    null
            )) > 0;
        } finally {
            if (clean) {
                var c = player.getPersistentDataContainer();
                var keys = c.getKeys();
                for (var key : keys) {
                    c.remove(key);
                }
            }
        }

    }

    @Override
    public void apply(@NotNull Player player, byte @NotNull [] data) {
        try {
            player.getPersistentDataContainer().readFromBytes(data, false);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
