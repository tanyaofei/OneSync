package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Locking;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.UUID;

public class LockingRepository extends Repository<Locking> {

    public final static LockingRepository instance = new LockingRepository(Main.getInstance());

    public LockingRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Locking selectById(@NotNull UUID id) {
        return super.selectById(id.toString());
    }

    public int deleteAll() {
        return execute(connection -> {
            var sql = """
                    delete from onesync.locking
                    """;
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                return stm.executeUpdate();
            }
        });
    }

    public int insertOrUpdate(@NotNull Locking locking) {
        return execute(connection -> {
            var sql = """
                    replace into onesync.locking (uuid, locked)
                    values (?, ?)
                    """;
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, locking.uuid().toString());
                stm.setBoolean(2, locking.locked());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    public @Nullable Locking selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initTables() {
        super.execute(connection -> {
            var stm = connection.createStatement();
            stm.execute("create database if not exists onesync");
            stm.execute("""
                    create table if not exists onesync.locking
                     (
                         uuid   varchar(36) not null
                             primary key,
                         locked tinyint(1)  not null
                     );
                    """);
        });
    }
}
