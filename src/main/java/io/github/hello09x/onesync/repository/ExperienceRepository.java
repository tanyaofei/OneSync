package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Experience;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.UUID;

public class ExperienceRepository extends Repository<Experience> {

    public final static ExperienceRepository instance = new ExperienceRepository(Main.getInstance());

    private ExperienceRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Experience selectById(@NotNull UUID uuid) {
        return super.selectById(uuid);
    }

    public int insertOrUpdate(@NotNull Experience experience) {
        var sql = """
                replace into onesync.experience (uuid, level, exp)
                values (?, ?, ?)
                """;
        return super.execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, experience.uuid().toString());
                stm.setInt(2, experience.level());
                stm.setFloat(3, experience.exp());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    public @Nullable Experience selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initTables() {
        super.execute(connection -> {
            var stm = connection.createStatement();
            stm.execute("create database if not exists onesync");
            stm.execute("""
                    create table if not exists onesync.experience
                    (
                        uuid      varchar(36) not null
                            primary key,
                        level     int         not null,
                        exp       float       not null
                    );
                    """);
        });
    }
}
