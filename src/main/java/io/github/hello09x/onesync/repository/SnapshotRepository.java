package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.command.Page;
import io.github.hello09x.devtools.database.jdbc.GeneratedKeyHolder;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.devtools.database.jdbc.rowmapper.IntegerRowMapper;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static io.github.hello09x.devtools.core.utils.Exceptions.suppress;

@Singleton
public class SnapshotRepository {

    private final JdbcTemplate jdbc;

    private final Snapshot.SnapshotRowMapper rowMapper;

    @Inject
    public SnapshotRepository(JdbcTemplate jdbc, Snapshot.SnapshotRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
    }

    public @NotNull Long insert(@NotNull Snapshot snapshot) {
        var sql = """
                insert into `snapshot` (player_id, cause) values (?, ?)
                """;

        var generatedKeyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, generatedKeyHolder, snapshot.playerId().toString(), snapshot.cause().name());
        return Objects.requireNonNull(generatedKeyHolder.getKeyAs(BigInteger.class)).longValue();
    }

    public @Nullable Snapshot selectById(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from snapshot where id = ?", rowMapper, snapshotId);
    }

    public @NotNull List<Snapshot> selectByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from `snapshot` where player_id = ?";
        return jdbc.query(sql, rowMapper, playerId.toString());
    }

    public @Nullable Snapshot selectLatestByPlayerId(@NotNull UUID playerId) {
        var sql = "select * from `snapshot` where player_id = ? order by created_at desc, id desc limit 1";
        return jdbc.queryForObject(sql, rowMapper, playerId.toString());
    }

    public @NotNull Page<Snapshot> selectPageByPlayerId(int page, int size, @NotNull UUID playerId) {
        var countSql = "select count(*) from `snapshot` where player_id = ?";
        var total = Objects.requireNonNull(jdbc.queryForObject(countSql, IntegerRowMapper.instance, playerId.toString()));
        int offset = (page - 1) * size;

        var records = jdbc.query(
                "select * from `snapshot` where player_id = ? order by created_at desc, id desc limit ?, ?",
                rowMapper,
                offset,
                size,
                playerId.toString()
        );

        return new Page<>(
                page,
                size,
                total,
                records
        );

    }

    @CanIgnoreReturnValue
    public int deleteById(@NotNull Long id) {
        return jdbc.update("delete from `snapshot` where id = ?", id);
    }

    @CanIgnoreReturnValue
    public int deleteByIds(@NotNull List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }

        return jdbc.update("delete from `snapshot` where id in (?)", StringUtils.join(ids, ","));
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists `snapshot`
                    (
                        id         bigint auto_increment
                            primary key,
                        player_id  char(36)                           not null comment '玩家 ID',
                        cause      varchar(32)                        not null comment '节点',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);

        suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                        create index snapshot_player_id_index
                            on `snapshot` (player_id);
                        """);
        }, false);

    }
}
