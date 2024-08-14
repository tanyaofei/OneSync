package io.github.hello09x.onesync.config;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.config.PluginConfig;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
@Singleton
public class OneSyncConfig extends PluginConfig {

    private final SynchronizeConfig synchronize = new SynchronizeConfig();
    private final SnapshotConfig snapshot = new SnapshotConfig();
    private boolean debug;

    @Setter
    private String serverId = UUID.randomUUID().toString();

    @Inject
    public OneSyncConfig(@NotNull Plugin plugin) {
        super(plugin, false);
        this.reload0();
    }

    @Provides
    public SynchronizeConfig synchronizeConfig() {
        return this.synchronize;
    }

    @Provides
    public SnapshotConfig snapshotConfig() {
        return this.snapshot;
    }

    private static @NotNull String getNonBlankString(@NotNull FileConfiguration file, @NotNull String path, @NotNull String def) {
        var value = file.getString(path, def);
        if (value.isBlank()) {
            return def;
        }
        return value;
    }

    @Override
    protected void reload(@NotNull FileConfiguration file) {
        this.debug = file.getBoolean("debug", true);
        this.synchronize.reload(file);
        this.snapshot.reload(file);
        Optional.ofNullable(file.getString("server-id")).filter(StringUtils::isNotBlank).ifPresent(this::setServerId);
    }

    @Getter
    @ToString
    public final static class SynchronizeConfig {

        private Enabled inventory;
        private Enabled enderChest;
        private Enabled pdc;
        private Enabled gameMode;
        private Enabled op;
        private Enabled health;
        private Enabled exp;
        private Enabled food;
        private Enabled air;
        private Enabled advancements;
        private Enabled potionEffects;
        private Enabled vault;

        public void reload(@NotNull FileConfiguration file) {
            this.inventory = Enabled.ofValue(file.getString("synchronize.inventory", "true"));
            this.enderChest = Enabled.ofValue(file.getString("synchronize.ender-chest", "true"));
            this.pdc = Enabled.ofValue(file.getString("synchronize.pdc", "false"));
            this.gameMode = Enabled.ofValue(file.getString("synchronize.profile.game-mode", "false"));
            this.op = Enabled.ofValue(file.getString("synchronize.profile.op", "false"));
            this.health = Enabled.ofValue(file.getString("synchronize.profile.health", "false"));
            this.exp = Enabled.ofValue(file.getString("synchronize.profile.exp", "false"));
            this.food = Enabled.ofValue(file.getString("synchronize.profile.food", "false"));
            this.air = Enabled.ofValue(file.getString("synchronize.profile.air", "false"));
            this.advancements = Enabled.ofValue(file.getString("synchronize.advancements", "false"));
            this.potionEffects = Enabled.ofValue(file.getString("synchronize.potion-effects", "false"));
            this.vault = Enabled.ofValue(file.getString("synchronize.vault", "false"));
        }
    }

    @Getter
    @ToString
    public final static class SnapshotConfig {

        /**
         * 每个玩家最大快照数
         */
        private int capacity;

        /**
         * 每个玩家多少天内每天至少保留一份最后的快照
         */
        private int keepDays;

        private Set<SnapshotCause> when = Collections.emptySet();

        public void reload(@NotNull FileConfiguration file) {
            this.capacity = file.getInt("snapshot.capacity", 45);
            this.keepDays = file.getInt("snapshot.keep-days", 7);
            this.when = file.getStringList("snapshot.when").stream().map(name -> {
                try {
                    return SnapshotCause.valueOf(name);
                } catch (Throwable e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }
    }

}
