package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.repository.model.Locking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class LockingRepository {


    private final JdbcTemplate jdbc;
    private final Locking.LockingRowMapper rowMapper;

    @Inject
    public LockingRepository(JdbcTemplate jdbc, Locking.LockingRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
    }

    public @Nullable Locking selectByPlayerId(@NotNull UUID playerId) {
        return jdbc.queryForObject("select * from locking where player_id = ?", rowMapper, playerId.toString());
    }

    @CanIgnoreReturnValue
    public int insertOrUpdate(@NotNull UUID playerId, @NotNull String serverId, boolean lock) {
        var sql = lock
                ? "replace into `locking` (player_id, server_id) values (?, ?)"
                : "delete from `locking` where player_id = ? and server_id = ?";

        return jdbc.update(sql, playerId.toString(), serverId);
    }

    @CanIgnoreReturnValue
    public int updateServerId(@NotNull String from, @NotNull String to) {
        var sql = "update `locking` set server_id = ? where server_id = ?";

        return jdbc.update(sql, to, from);
    }

    @CanIgnoreReturnValue
    public int deleteByPlayerId(@NotNull UUID playerId) {
        var sql = "delete from locking where player_id = ?";
        return jdbc.update(sql, playerId.toString());
    }

    @CanIgnoreReturnValue
    public int deleteByServerId(@NotNull String serverId) {
        var sql = "delete from locking where server_id = ?";
        return jdbc.update(sql, serverId);
    }

    @CanIgnoreReturnValue
    public int deleteAll() {
        return jdbc.update("delete from locking");
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists locking
                    (
                        player_id  char(36)                           not null comment '玩家 ID'
                            primary key,
                        server_id  char(36)                           not null comment '服务器 ID',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);
    }


}
