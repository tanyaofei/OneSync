package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.PDC;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.UUID;

public class PDCRepository extends Repository<PDC> {

    public final static PDCRepository instance = new PDCRepository(Main.getInstance());

    public PDCRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable PDC selectById(@NotNull UUID id) {
        return super.selectById(id.toString());
    }

    @Override
    @Nullable
    public PDC selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    public int insertOrUpdate(@NotNull PDC pdc) {
        var uuid = pdc.uuid();
        if (uuid == null) {
            throw new IllegalArgumentException("uuid must not be null");
        }

        var sql = """
                replace into onesync.pdc (uuid, data)
                values (?, ?)
                """;

        return super.execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, pdc.uuid().toString());
                stm.setBytes(2, pdc.data());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        super.execute(connection -> {
            var stm = connection.createStatement();
            stm.execute("create database if not exists onesync");
            stm.execute("""
                    create table if not exists onesync.pdc
                    (
                        uuid       varchar(36)                        not null
                            primary key,
                        data       mediumblob                         not null,
                        created_at datetime default CURRENT_TIMESTAMP not null,
                        updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
                    );
                    """);
        });
    }
}
