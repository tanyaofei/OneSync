package io.github.hello09x.onesync.repository;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.bedrock.page.Page;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SnapshotRepository extends Repository<Snapshot> {

    public final static SnapshotRepository instance = new SnapshotRepository(Main.getInstance());

    public @NotNull Long insert(@NotNull Snapshot snapshot) {
        var sql = """
                insert into `snapshot` (player_id, cause) values (?, ?)
                """;

        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stm.setString(1, snapshot.playerId().toString());
                stm.setString(2, snapshot.cause().name());
                stm.executeUpdate();
                var rs = stm.getGeneratedKeys();
                if (!rs.next()) {
                    throw new Error("Should never happen");
                }
                return rs.getLong(1);
            }
        });
    }

    public @NotNull List<Snapshot> selectByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from `snapshot` where player_id = ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return mapMany(stm.executeQuery());
            }
        });
    }

    public @Nullable Snapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from `snapshot` where player_id = ? order by created_at desc, id desc limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return mapOne(stm.executeQuery());
            }
        });
    }

    public @NotNull Page<Snapshot> selectPageByPlayerId(int page, int size, @NotNull UUID playerId) {
        @Language("SQL")
        var sql = "select * from `snapshot` where player_id = ? order by created_at desc, id desc";
        return super.selectPage(page, size, sql, playerId.toString());
    }

    public SnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "snapshot"));
            if (rs.next()) {
                return;
            }

            stm.executeUpdate("""
                    create table `snapshot`
                    (
                        id         bigint auto_increment
                            primary key,
                        player_id  char(36)                           not null comment '玩家 ID',
                        cause      varchar(32)                        not null comment '节点',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);
            stm.executeUpdate("""
                    create index snapshot_player_id_index
                        on `snapshot` (player_id);
                    """);
        });
    }
}
