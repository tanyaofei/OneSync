package io.github.hello09x.onesync.manager.synchronize.entity;

import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

public record PreparedSnapshotComponent(

        @SuppressWarnings("rawtypes")
        RegisteredServiceProvider<SnapshotHandler> registration,

        @Nullable
        SnapshotComponent component
) {
}
