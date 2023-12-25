package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.VaultSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class VaultSnapshotRepository extends Repository<VaultSnapshot> {

    public final static VaultSnapshotRepository instance = new VaultSnapshotRepository(Main.getInstance());

    public VaultSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull VaultSnapshot snapshot) {
        var sql = "insert into vault_snapshot (snapshot_id, player_id, balance) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                stm.setDouble(3, snapshot.balance());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "vault_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table vault_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        balance     double   not null comment '余额'
                    );
                    """);

            stm.executeUpdate("""
                    create index vault_snapshot_player_id_index
                        on vault_snapshot (player_id);
                    """);
        });
    }
}
