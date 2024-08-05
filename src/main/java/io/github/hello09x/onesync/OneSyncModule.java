package io.github.hello09x.onesync;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.bukkit.plugin.Plugin;

/**
 * @author tanyaofei
 * @since 2024/8/5
 **/
public class OneSyncModule extends AbstractModule {

    @Provides
    public Plugin plugin() {
        return Main.getInstance();
    }

}
