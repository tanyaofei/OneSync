package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Locking;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.ServiceUnavailableException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LockingRepository extends Repository<Locking> {

    private final static UUID SERVER_ID = UUID.randomUUID();

    public final static LockingRepository instance = new LockingRepository(Main.getInstance());

    public LockingRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Locking selectById(@NotNull UUID id) {
        return super.selectById(id);
    }

    public @Nullable Locking selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    public boolean setLock(@NotNull UUID playerId, boolean lock) {
        var modification = lock
                ? "replace into `locking` (player_id, server_id) values (?, ?)"
                : "delete from `locking` where player_id = ? and server_id = ?";

        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(modification)) {
                stm.setString(1, playerId.toString());
                stm.setString(2, SERVER_ID.toString());
                return stm.executeUpdate() > 0;
            }
        });
    }

    public int deleteByPlayerId(@NotNull UUID playerId) {
        var sql = "delete from locking where player_id = ?";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, playerId.toString());
                return stm.executeUpdate();
            }
        });
    }

    public int deleteAll() {
        return execute(connection -> {
            try (Statement stm = connection.createStatement()) {
                return stm.executeUpdate("delete from locking");
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            var stm = connection.createStatement();
            stm.executeUpdate("""
                    create table if not exists locking
                    (
                        player_id  char(36)                           not null comment '玩家 ID'
                            primary key,
                        server_id  char(36)                           not null comment '服务器 ID',
                        created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间'
                    );
                    """);
        });
    }


}
