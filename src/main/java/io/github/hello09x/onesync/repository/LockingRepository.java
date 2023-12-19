package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Locking;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
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

    public boolean isLocked(@NotNull UUID playerId) {
        return Optional.ofNullable(this.selectById(playerId.toString())).isPresent();
    }

    public boolean setLock(@NotNull UUID playerId, boolean lock) {
        var modification = lock
                ? "insert into `locking` (player_id, server_id) values (?, ?)"
                : "delete from `locking` where player_id = ? and server_id = ?";

        return execute(connection -> {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement stm = connection.prepareStatement(modification)) {
                    stm.setString(1, playerId.toString());
                    stm.setString(2, SERVER_ID.toString());
                    return stm.executeUpdate() > 0;
                }
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    /**
     * 解锁
     * <p>
     * <b>使用该方法前需要确保该玩家在所有服务器都下线了</b>
     * </p>
     *
     * @param playerIds 玩家 ID 列表
     * @return 是否解锁成功
     */
    public boolean unlock(@NotNull List<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return false;
        }
        var sql = "delete from `locking` where player_id in ("
                + IntStream.range(0, playerIds.size()).mapToObj(x -> "?").collect(Collectors.joining(", "))
                + ")";

        execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                int i = 1;
                for (var playerId : playerIds) {
                    stm.setString(i++, playerId.toString());
                }
                stm.executeUpdate();
            }
        });

        return true;
    }

    @Override
    protected void initTables() {
    }


}
