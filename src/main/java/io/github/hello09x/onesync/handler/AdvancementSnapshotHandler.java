package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.repository.AdvancementSnapshotRepository;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AdvancementSnapshotHandler implements SnapshotHandler<AdvancementSnapshot> {

    private final static Logger log = Main.getInstance().getLogger();
    private final AdvancementSnapshotRepository repository = AdvancementSnapshotRepository.instance;

    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    private final static Map<String, Advancement> ADVANCEMENTS = StreamSupport
            .stream(((Iterable<Advancement>) Bukkit::advancementIterator).spliterator(), false)
            .collect(Collectors.toMap(adv -> adv.getKey().asString(), Function.identity()));

    @Override
    public @NotNull String snapshotType() {
        return "成就";
    }

    @Override
    public @NotNull Plugin plugin() {
        return Main.getInstance();
    }

    @Override
    public @Nullable AdvancementSnapshot getLatest(@NotNull UUID playerId) {
        return repository.selectLatestByPlayerId(playerId);
    }

    @Override
    public @Nullable AdvancementSnapshot getOne(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public void save(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isAdvancement()) {
            return;
        }
        var advancements = StreamSupport.
                stream(((Iterable<Advancement>) Bukkit::advancementIterator).spliterator(), false)
                .collect(Collectors.toMap(
                        advancement -> advancement.getKey().asString(),
                        advancement -> player.getAdvancementProgress(advancement).getAwardedCriteria()
                ));

        repository.insert(new AdvancementSnapshot(
                snapshotId,
                player.getUniqueId(),
                advancements
        ));
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
    public boolean apply(@NotNull Player player, @NotNull AdvancementSnapshot snapshot) {
        if (!config.isAdvancement()) {
            return false;
        }
        for (var entry : ADVANCEMENTS.entrySet()) {
            var key = entry.getKey();
            var advancement = entry.getValue();
            var criteria = snapshot.advancements().get(key);
            if (criteria != null && !criteria.isEmpty()) {
                try {
                    var progress = player.getAdvancementProgress(advancement);
                    criteria.forEach(progress::awardCriteria);
                } catch (Throwable e) {
                    log.warning("Failed to apply advancement: " + e.getMessage());
                }
            }
        }
        return true;
    }
}
