package io.github.hello09x.onesync.config;

import io.github.hello09x.bedrock.config.Config;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public class OneSyncConfig extends Config<OneSyncConfig> {

    public final static OneSyncConfig instance = new OneSyncConfig(Main.getInstance(), "3");

    private final SynchronizeConfig synchronize = new SynchronizeConfig();
    private final SnapshotConfig snapshot = new SnapshotConfig();
    private final TeleportConfig teleport = new TeleportConfig();
    private boolean debug;

    @Setter
    private String serverId = UUID.randomUUID().toString();

    public OneSyncConfig(@NotNull Plugin plugin, @Nullable String version) {
        super(plugin, version);
        this.reload(false);
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
        this.teleport.reload(file);
        Optional.ofNullable(file.getString("server-id")).filter(StringUtils::isNotBlank).ifPresent(this::setServerId);
    }

    @Getter
    @ToString
    public final static class SynchronizeConfig {
        private boolean inventory;
        private boolean enderChest;
        private boolean pdc;
        private boolean gameMode;
        private boolean op;
        private boolean health;
        private boolean exp;
        private boolean food;
        private boolean air;
        private boolean advancements;
        private boolean potionEffects;
        private boolean vault;

        public void reload(@NotNull FileConfiguration file) {
            this.inventory = file.getBoolean("synchronize.inventory", true);
            this.enderChest = file.getBoolean("synchronize.ender-chest", true);
            this.pdc = file.getBoolean("synchronize.pdc", false);
            this.gameMode = file.getBoolean("synchronize.profile.game-mode", false);
            this.op = file.getBoolean("synchronize.profile.op", false);
            this.health = file.getBoolean("synchronize.profile.health", false);
            this.exp = file.getBoolean("synchronize.profile.exp", false);
            this.food = file.getBoolean("synchronize.profile.food", false);
            this.air = file.getBoolean("synchronize.profile.air", false);
            this.advancements = file.getBoolean("synchronize.advancements", false);
            this.potionEffects = file.getBoolean("synchronize.potion-effects", false);
            this.vault = file.getBoolean("synchronize.vault", false);
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

    @Getter
    @ToString
    public final static class TeleportConfig {

        private final Map<String, String> commands = new HashMap<>();
        private boolean enabled;
        private int wait;
        private Duration expiresIn;

        public void reload(@NotNull FileConfiguration file) {
            this.enabled = file.getBoolean("teleport.enabled", false);
            this.expiresIn = Duration.ofSeconds(file.getInt("teleport.expires-in", 60));
            this.wait = 20 * file.getInt("teleport.wait", 3);

            this.commands.clear();
            this.commands.put("stpa", getNonBlankString(file, "teleport.commands.stpa", "tpa"));
            this.commands.put("stpahere", getNonBlankString(file, "teleport.commands.stpahere", "tpahere"));
            this.commands.put("stpaccept", getNonBlankString(file, "teleport.commands.stpaccept", "tpaccept"));
            this.commands.put("stpdeny", getNonBlankString(file, "teleport.commands.stpdeny", "tpdeny"));
            this.commands.put("stpcacel", getNonBlankString(file, "teleport.commands.stpcacel", "tpcacel"));

            this.commands.put("stp", getNonBlankString(file, "teleport.commands.stp", "tp"));
            this.commands.put("stphere", getNonBlankString(file, "teleport.commands.stphere", "tphere"));
            this.commands.put("stphereall", getNonBlankString(file, "teleport.commands.stphereall", "tphereall"));
            this.commands.put("stpahereall", getNonBlankString(file, "teleport.commands.stpahereall", "tpahereall"));
        }
    }

}
