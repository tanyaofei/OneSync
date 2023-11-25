package io.github.hello09x.onesync.config;

import io.github.hello09x.bedrock.config.Config;
import io.github.hello09x.onesync.Main;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Getter
public class OnesyncConfig extends Config<OnesyncConfig> {

    public final static OnesyncConfig instance = new OnesyncConfig(Main.getInstance(), "1");
    private final Sync sync = new Sync();
    private final Snapshot snapshot = new Snapshot();

    private OnesyncConfig(@NotNull Plugin plugin, @Nullable String version) {
        super(plugin, version);
        super.reload(false);
    }

    @Override
    protected void reload(@NotNull FileConfiguration file) {
        this.sync.reload(file);
        this.snapshot.reload(file);
    }

    public enum PDCSyncMode {
        ALL,
        NONE,
        WHITELIST,
        BLACKLIST,
    }

    @Getter
    public static class Sync {
        private boolean inventory;
        private boolean pdc;
        private boolean health;
        private boolean food;
        private boolean exp;

        public void reload(@NotNull FileConfiguration file) {
            this.inventory = file.getBoolean("sync.inventory", false);
            this.pdc = file.getBoolean("sync.pdc", false);
            this.health = file.getBoolean("sync.health", false);
            this.food = file.getBoolean("sync.food", false);
            this.exp = file.getBoolean("sync.exp", false);
        }

    }

    @Getter
    public static class Snapshot {
        private boolean enabled;

        public void reload(@NotNull FileConfiguration file) {
            this.enabled = file.getBoolean("snapshot", true);
        }
    }

}
