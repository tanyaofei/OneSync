package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.bedrock.util.Exceptions;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.ExtensionSnapshot;
import io.github.hello09x.onesync.util.NamespacedKeyTypeHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExtensionSnapshotRepository extends Repository<ExtensionSnapshot> {

    public final static ExtensionSnapshotRepository instance = new ExtensionSnapshotRepository(Main.getInstance());

    public ExtensionSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull ExtensionSnapshot snapshot) {
        var sql = "insert into extension_snapshot (snapshot_id, player_id, type, `data`) values (?, ?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                NamespacedKeyTypeHandler.instance.setParameter(stm, 3, snapshot.type());
                stm.setBytes(4, snapshot.data());
                return stm.executeUpdate();
            }
        });
    }

    public @Nullable ExtensionSnapshot selectBySnapshotIdAndType(@NotNull Long snapshotId, @NotNull NamespacedKey type) {
        var sql = "select * from extension_snapshot where snapshot_id = ? and type = ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshotId);
                NamespacedKeyTypeHandler.instance.setParameter(stm, 2, type);
                return mapOne(stm.executeQuery());
            }
        });
    }

    public int deleteBySnapshotIdsAndType(@NotNull List<Long> snapshotIds, @NotNull NamespacedKey type) {
        if (snapshotIds.isEmpty()) {
            return 0;
        }

        var sql = "delete from extension_snapshot where and type = ? and snapshot_id in ("
                + IntStream.range(0, snapshotIds.size()).mapToObj(x -> "?").collect(Collectors.joining(", "))
                + ")";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                NamespacedKeyTypeHandler.instance.setParameter(stm, 1, type);
                int i = 2;
                for (var snapshotId : snapshotIds) {
                    stm.setLong(i++, snapshotId);
                }
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            stm.executeUpdate("""
                    create table if not exists extension_snapshot
                    (
                        id          int auto_increment comment 'ID'
                            primary key,
                        snapshot_id bigint       not null comment '快照 ID',
                        player_id   char(36)     not null comment '玩家 ID',
                        type        varchar(256) not null comment '类型',
                        `data`      blob         not null comment '数据',
                        constraint extension_snapshot_snapshot_id_type_uindex
                            unique (snapshot_id, type)
                    );
                    """);
            Exceptions.noException(() -> {
                stm.executeUpdate("""
                        create index extension_snapshot_player_id_index
                            on extension_snapshot (player_id);
                        """);
            });
        });
    }
}
