package io.github.hello09x.onesync.util;

import io.github.hello09x.onesync.OneSync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.logging.Logger;

public class SnapshotHelper {

    private final static Logger log = OneSync.getInstance().getLogger();

    public static <ID, T> @Nullable T getRefsToOrItself(
            @NotNull ID id,
            @NotNull Function<ID, T> getById,
            @NotNull Function<T, ID> getRefsTo,
            @NotNull String snapshotType
    ) {
        var snapshot = getById.apply(id);
        if (snapshot == null) {
            return null;
        }

        var refsTo = getRefsTo.apply(snapshot);
        if (refsTo == null) {
            return snapshot;
        }

        var ref = getById.apply(refsTo);
        if (ref == null) {
            log.warning("编号为 %s 的「%s」快照压缩指向的编号为 %s 的快照不存在".formatted(
                    id.toString(),
                    snapshotType,
                    refsTo.toString()
            ));
            return null;
        }

        var refsTo2 = getRefsTo.apply(ref);
        if (refsTo2 != null) {
            throw new IllegalStateException("Invalid refsTo snapshot: %s -> %s -> %s".formatted(
                    id.toString(),
                    refsTo.toString(),
                    refsTo2.toString()
            ));
        }

        return ref;
    }


}
