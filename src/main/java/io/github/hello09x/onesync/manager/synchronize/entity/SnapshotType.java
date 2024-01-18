package io.github.hello09x.onesync.manager.synchronize.entity;

import org.jetbrains.annotations.NotNull;

public record SnapshotType(

        @NotNull
        String key,

        @NotNull
        String name

) {

    @Override
    public String toString() {
        return this.name;
    }
}
