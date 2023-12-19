package io.github.hello09x.onesync.config;

import io.github.hello09x.bedrock.config.Config;
import io.github.hello09x.onesync.Main;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
public class OneSyncConfig extends Config<OneSyncConfig> {

    public final static OneSyncConfig instance = new OneSyncConfig(Main.getInstance(), "1");

    private final Synchronize synchronize = new Synchronize();

    public OneSyncConfig(@NotNull Plugin plugin, @Nullable String version) {
        super(plugin, version);
        this.reload(false);
    }

    @Override
    protected void reload(@NotNull FileConfiguration file) {
        this.synchronize.reload(file);
    }

    @Getter
    @ToString
    public final static class Synchronize {
        private boolean inventory;
        private boolean pdc;
        private boolean gameMode;
        private boolean op;
        private boolean health;
        private boolean exp;
        private boolean food;

        public void reload(@NotNull FileConfiguration file) {
            this.inventory = file.getBoolean("synchronize.inventory", true);
            this.pdc = file.getBoolean("synchronize.pdc", false);
            this.gameMode = file.getBoolean("synchronize.game-mode", false);
            this.op = file.getBoolean("synchronize.op", false);
            this.health = file.getBoolean("synchronize.health", false);
            this.exp = file.getBoolean("synchronize.exp", false);
            this.food = file.getBoolean("synchronize.food", false);
        }
    }

}
