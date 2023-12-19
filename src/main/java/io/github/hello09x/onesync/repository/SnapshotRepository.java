package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

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


    public SnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected void initTables() {

    }
}
