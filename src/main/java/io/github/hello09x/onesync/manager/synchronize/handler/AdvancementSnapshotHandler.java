package io.github.hello09x.onesync.manager.synchronize.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.AdvancementSnapshotRepository;
import io.github.hello09x.onesync.repository.model.AdvancementSnapshot;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AdvancementSnapshotHandler extends CacheableSnapshotHandler<AdvancementSnapshot> {

    public final static AdvancementSnapshotHandler instance = new AdvancementSnapshotHandler();

    private final static SnapshotType TYPE = new SnapshotType(
            "onesync:snapshot.advancement",
            "成就"
    );

    private final static Logger log = Main.getInstance().getLogger();
    private final static Map<String, Advancement> ADVANCEMENTS = StreamSupport
            .stream(((Iterable<Advancement>) Bukkit::advancementIterator).spliterator(), false)
            .collect(Collectors.toMap(adv -> adv.getKey().asString(), Function.identity()));

    private final AdvancementSnapshotRepository repository = AdvancementSnapshotRepository.instance;
    private final OneSyncConfig.SynchronizeConfig config = OneSyncConfig.instance.getSynchronize();


    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    public @Nullable AdvancementSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectById(snapshotId);
    }

    @Override
    public @Nullable AdvancementSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable AdvancementSnapshot baton) {
        if (config.getAdvancements() == Enabled.FALSE) {
            return null;
        }

        if (config.getAdvancements() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new AdvancementSnapshot(snapshotId, baton.playerId(), baton.advancements());
                repository.insert(snapshot);
                return snapshot;
            }
            return null;
        }

        var advancements = StreamSupport
                .stream(((Iterable<Advancement>) Bukkit::advancementIterator).spliterator(), false)
                .map(advancement -> Pair.of(advancement.getKey().asString(), player.getAdvancementProgress(advancement).getAwardedCriteria()))
                .filter(pair -> !pair.getValue().isEmpty())
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        var snapshot = new AdvancementSnapshot(snapshotId, player.getUniqueId(), advancements);

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    public void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull AdvancementSnapshot snapshot) {
        if (config.getAdvancements() != Enabled.TRUE) {
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
                    log.warning("无法为 %s 恢复「%s」的成就数据: %s".formatted(
                            player.getName(),
                            advancement.getKey().asString(),
                            e.getMessage())
                    );
                }
            }
        }

        return true;
    }
}
