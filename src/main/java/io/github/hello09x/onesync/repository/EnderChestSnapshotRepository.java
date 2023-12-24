package io.github.hello09x.onesync.repository;

import io.github.hello09x.onesync.util.ItemStackMapTypeHandler;
import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.EnderChestSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class EnderChestSnapshotRepository extends Repository<EnderChestSnapshot> {

    public final static EnderChestSnapshotRepository instance = new EnderChestSnapshotRepository(Main.getInstance());

    public EnderChestSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull EnderChestSnapshot snapshot) {
        var sql = """
                insert into ender_chest_snapshot (snapshot_id, player_id, items)
                values (?, ?, ?)
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                ItemStackMapTypeHandler.instance.setParameter(stm, 3, snapshot.items());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "ender_chest_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table ender_chest_snapshot
                    (
                        snapshot_id        bigint        not null comment '快照 ID'
                            primary key,
                        player_id          char(36)      not null comment '玩家 ID',
                        items              json          not null comment '背包物品'
                    );
                    """);
            stm.executeUpdate("""
                    create index ender_chest_snapshot_player_id_index
                        on ender_chest_snapshot (player_id);
                    """);
        });
    }

}
