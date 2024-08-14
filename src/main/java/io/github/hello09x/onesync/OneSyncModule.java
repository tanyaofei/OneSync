package io.github.hello09x.onesync;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.menu.ChestMenuRegistry;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author tanyaofei
 * @since 2024/8/5
 **/
public class OneSyncModule extends AbstractModule {

    @Provides
    @Singleton
    public Plugin plugin() {
        return OneSync.getInstance();
    }

    @Provides
    @Singleton
    public ChestMenuRegistry chestMenuRegistry(@NotNull Plugin plugin) {
        return new ChestMenuRegistry(plugin);
    }

}
