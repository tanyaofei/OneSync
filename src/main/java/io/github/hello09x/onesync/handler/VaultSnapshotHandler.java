package io.github.hello09x.onesync.handler;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.CacheableSnapshotHandler;
import io.github.hello09x.onesync.config.OneSyncConfig;
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

public class VaultSnapshotHandler extends CacheableSnapshotHandler<VaultSnapshot> {


    public final static VaultSnapshotHandler instance = new VaultSnapshotHandler();
    private final static Logger log = Main.getInstance().getLogger();

    private final VaultSnapshotRepository repository = VaultSnapshotRepository.instance;

    private final OneSyncConfig.Synchronize config = OneSyncConfig.instance.getSynchronize();

    private final Holder holder;

    public VaultSnapshotHandler() {
        this.holder = Optional.of(Bukkit.getPluginManager().isPluginEnabled("Vault"))
                .filter(isEnabled -> isEnabled)
                .map(x -> new Holder())
                .filter(holder -> holder.economy != null)
                .orElse(null);
    }

    @Override
    protected @Nullable VaultSnapshot save0(@NotNull Long snapshotId, @NotNull Player player) {
        if (!config.isVault()) {
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
        return repository.selectById(snapshotId);
    }

    @Override
    protected void remove0(@NotNull List<Long> snapshotIds) {
        repository.deleteByIds(snapshotIds);
    }

    @Override
    public @NotNull String snapshotType() {
        return "经济";
    }

    @Override
    public void apply(@NotNull Player player, @NotNull VaultSnapshot snapshot, boolean force) {
        if (!config.isVault() || force) {
            return;
        }

        if (this.holder == null) {
            throw new IllegalStateException("服务器没有启用经济插件, 无法恢复 %s 的经济快照".formatted(player.getName()));
        }

        var current = this.holder.economy.getBalance(player);
        this.holder.economy.withdrawPlayer(player, current);
        this.holder.economy.depositPlayer(player, snapshot.balance());
    }

    private final static class Holder {

        @UnknownNullability
        private final Economy economy = Optional
                .ofNullable(Bukkit.getServicesManager().getRegistration(Economy.class))
                .map(RegisteredServiceProvider::getProvider)
                .orElse(null);

    }


}
