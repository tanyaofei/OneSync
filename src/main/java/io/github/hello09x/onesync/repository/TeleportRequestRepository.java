package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.TeleportRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Singleton
public class TeleportRequestRepository {

    private final JdbcTemplate jdbc;
    private final TeleportRequest.TeleportRequestRowMapper rowMapper;

    @Inject
    public TeleportRequestRepository(JdbcTemplate jdbc, TeleportRequest.TeleportRequestRowMapper rowMapper) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.initTables();
    }

    public int insert(@NotNull TeleportRequest teleportRequest) {
        var sql = "insert into teleport_request (requester, receiver, type) values (?, ?, ?)";
        return jdbc.update(sql, teleportRequest.requester(), teleportRequest.receiver(), teleportRequest.type().name());
    }

    public @Nullable TeleportRequest selectLatestByReceiverBefore(@NotNull String receiver, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleportRequest where receiver = ? and created_at < ? order by created_at desc limit 1";
        return jdbc.queryForObject(
                sql,
                rowMapper,
                receiver,
                Timestamp.valueOf(createdAtBefore)
        );
    }

    public @Nullable TeleportRequest selectLatestByRequesterBefore(@NotNull String requester, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleportRequest where requester = ? and created_at < ? order by created_at desc limit 1";
        return jdbc.queryForObject(sql, rowMapper, requester, Timestamp.valueOf(createdAtBefore));
    }

    public @Nullable TeleportRequest selectLatestByRequesterAndReceiverAfter(@NotNull String requester, @NotNull String receiver, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleportRequest where requester = ? and receiver = ? and created_at < ? limit 1";
        return jdbc.queryForObject(sql, rowMapper, requester, receiver, Timestamp.valueOf(createdAtBefore));
    }

    @CanIgnoreReturnValue
    public int deleteByRequesterAndReceiver(@NotNull String requester, @NotNull String receiver) {
        var sql = "delete from teleportRequest where requester = ? and receiver = ?";
        return jdbc.update(sql, requester, receiver);
    }

    public int deleteByCreatedAtBefore(@NotNull LocalDateTime before) {
        var sql = "delete from teleportRequest where created_at < ?";
        return jdbc.update(sql, Timestamp.valueOf(before));
    }

    protected void initTables() {
        jdbc.execute("""
                                      create table if not exists teleportRequest
                    (
                        requester  varchar(32)                        not null comment '请求人',
                        receiver   varchar(32)                        not null comment '接受人',
                        type       varchar(32)                        not null comment '类型',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);

        Exceptions.suppress(Main.getInstance(), () -> {
            jdbc.execute("""
                        create index teleport_request_receiver_id_index
                                              on teleportRequest (receiver);
                        """);
        }, false);

        Exceptions.suppress(Main.getInstance(), () -> {
            jdbc.execute("""
                        create index teleport_request_requester_id_index
                                              on teleportRequest (requester);
                        """);
        }, false);
    }
}
