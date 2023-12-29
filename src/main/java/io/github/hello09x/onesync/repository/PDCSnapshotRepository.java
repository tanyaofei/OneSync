package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.bedrock.util.Exceptions;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.PDCSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class PDCSnapshotRepository extends Repository<PDCSnapshot> {

    public final static PDCSnapshotRepository instance = new PDCSnapshotRepository(Main.getInstance());

    public PDCSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull PDCSnapshot snapshot) {
        var sql = "insert into pdc_snapshot (snapshot_id, player_id, `data`) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                stm.setBytes(3, snapshot.data());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            stm.executeUpdate("""
                    create table if not exists pdc_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        `data`      blob     not null comment 'PDC 数据'
                    );
                    """);
            Exceptions.noException(() -> {
                stm.executeUpdate("""
                        create index pdc_snapshot_player_id_index
                            on pdc_snapshot (player_id);
                        """);
            });
        });
    }
}
