package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Teleport;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TeleportRepository extends Repository<Teleport> {

    public final static TeleportRepository instance = new TeleportRepository(Main.getInstance());

    public TeleportRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull Teleport teleport) {
        var sql = "insert into teleport (requester, receiver, type) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, teleport.requester());
                stm.setString(2, teleport.receiver());
                stm.setString(3, teleport.type().name());
                return stm.executeUpdate();
            }
        });
    }

    public boolean existsByRequesterAndReceiver(@NotNull String requester, @NotNull String receiver) {
        var sql = "select exists (select 1 from teleport where requester = ? and receiver = ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, requester);
                stm.setString(2, receiver);
                var rs = stm.executeQuery();
                rs.next();
                return rs.getInt(1) >= 1;
            }
        });
    }

    public @Nullable Teleport selectLatestByReceiverBefore(@NotNull String receiver, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleport where receiver = ? and created_at < ? order by created_at desc limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, receiver);
                stm.setTimestamp(2, Timestamp.valueOf(createdAtBefore));
                return mapOne(stm.executeQuery());
            }
        });
    }

    public @Nullable Teleport selectLatestByRequesterBefore(@NotNull String requester, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleport where requester = ? and created_at < ? order by created_at desc limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, requester);
                stm.setTimestamp(2, Timestamp.valueOf(createdAtBefore));
                return mapOne(stm.executeQuery());
            }
        });
    }

    public @Nullable Teleport selectLatestByRequesterAndReceiverAfter(@NotNull String requester, @NotNull String receiver, @NotNull LocalDateTime createdAtBefore) {
        var sql = "select * from teleport where requester = ? and receiver = ? and created_at < ? limit 1";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, requester);
                stm.setString(2, receiver);
                stm.setTimestamp(3, Timestamp.valueOf(createdAtBefore));
                return mapOne(stm.executeQuery());
            }
        });
    }

    public int deleteByRequesterAndReceiver(@NotNull String requester, @NotNull String receiver) {
        var sql = "delete from teleport where requester = ? and receiver = ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, requester);
                stm.setString(2, receiver);
                return stm.executeUpdate();
            }
        });
    }

    public int deleteByCreatedAtBefore(@NotNull LocalDateTime before) {
        var sql = "delete from teleport where created_at < ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setTimestamp(1, Timestamp.valueOf(before));
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "teleport"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table teleport
                    (
                        requester  varchar(32)                        not null comment '请求人',
                        receiver   varchar(32)                        not null comment '接受人',
                        type       varchar(32)                        not null comment '类型',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);
            stm.executeUpdate("""
                    create index teleport_request_receiver_id_index
                        on teleport (receiver);
                    """);
            stm.executeUpdate("""
                    create index teleport_request_requester_id_index
                        on teleport (requester);
                    """);
        });
    }
}
