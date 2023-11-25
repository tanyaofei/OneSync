package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Health;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class HealthRepository extends Repository<Health> {

    public final static HealthRepository instance = new HealthRepository(Main.getInstance());

    private HealthRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Health selectById(@NotNull UUID id) {
        return super.selectById(id.toString());
    }

    public int saveOrUpdate(@NotNull Health health) {
        var sql = """
                replace into onesync.health (uuid, health, max_health)
                values (?, ?, ?)
                """;

        return super.execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, health.uuid().toString());
                stm.setDouble(2, health.health());
                stm.setDouble(3, health.maxHealth());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    public @Nullable Health selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initTables() {
        super.execute(connection -> {
            var stm = connection.createStatement();
            stm.execute("create database if not exists onesync");
            stm.execute("""
                    create table if not exists onesync.health
                    (
                        uuid       varchar(36) not null
                            primary key,
                        health     double      not null,
                        max_health double      not null
                    );
                    """);
        });
    }
}
