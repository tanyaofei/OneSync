package io.github.hello09x.onesync.manager.synchronize.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.Enabled;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.manager.synchronize.entity.SnapshotType;
import io.github.hello09x.onesync.repository.VaultSnapshotRepository;
import io.github.hello09x.onesync.repository.model.VaultSnapshot;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Singleton
public class VaultSnapshotHandler extends CacheableSnapshotHandler<VaultSnapshot> {

    private final static SnapshotType TYPE = new SnapshotType(
            "onesync.snapshot.vault",
            "经济"
    );

    private final static Logger log = Main.getInstance().getLogger();

    private final VaultSnapshotRepository repository;

    private final OneSyncConfig.SynchronizeConfig config;

    private final Holder holder;

    @Inject
    public VaultSnapshotHandler(VaultSnapshotRepository repository, OneSyncConfig.SynchronizeConfig config) {
        this.repository = repository;
        this.config = config;
        this.holder = Optional.of(Bukkit.getPluginManager().isPluginEnabled("Vault"))
                .filter(isEnabled -> isEnabled)
                .map(x -> new Holder())
                .filter(holder -> holder.economy != null)
                .orElse(null);
    }

    @Override
    public @NotNull SnapshotType snapshotType() {
        return TYPE;
    }

    @Override
    protected @Nullable VaultSnapshot save0(@NotNull Long snapshotId, @NotNull Player player, @Nullable VaultSnapshot baton) {
        if (config.getVault() == Enabled.FALSE) {
            return null;
        }
        if (config.getVault() == Enabled.ISOLATED) {
            if (baton != null) {
                var snapshot = new VaultSnapshot(snapshotId, baton.playerId(), baton.balance());
                repository.insert(snapshot);
                return snapshot;
            }
            return null;
        }

        if (this.holder == null) {
            throw new IllegalStateException("服务器没有启用经济插件, 无法保存 %s 的经济快照".formatted(player.getName()));
        }

        var snapshot = new VaultSnapshot(
                snapshotId,
                player.getUniqueId(),
                holder.economy.getBalance(player)
        );

        repository.insert(snapshot);
        return snapshot;
    }

    @Override
    protected @Nullable VaultSnapshot getOne0(@NotNull Long snapshotId) {
        return repository.selectBySnapshotId(snapshotId);
    }

    @Override
    protected void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteBySnapshotIds(snapshotIds);
    }

    @Override
    public boolean apply(@NotNull Player player, @NotNull VaultSnapshot snapshot) {
        if (config.getVault() != Enabled.TRUE) {
            return false;
        }

        if (this.holder == null) {
            throw new IllegalStateException("服务器没有启用经济插件, 无法恢复 %s 的经济快照".formatted(player.getName()));
        }

        // fixme: Folia 并发不在一个事务里
        var diff = snapshot.balance() - this.holder.economy.getBalance(player);
        if (diff > 0) {
            this.holder.economy.depositPlayer(player, diff);
        } else if (diff < 0) {
            this.holder.economy.withdrawPlayer(player, -diff);
        }
        return true;
    }

    private final static class Holder {

        @UnknownNullability
        private final Economy economy = Optional
                .ofNullable(Bukkit.getServicesManager().getRegistration(Economy.class))
                .map(RegisteredServiceProvider::getProvider)
                .orElse(null);

    }


}
