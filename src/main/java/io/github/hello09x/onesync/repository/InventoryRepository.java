package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InventoryRepository extends Repository<Inventory> {

    public final static InventoryRepository instance = new InventoryRepository(Main.getInstance());

    public InventoryRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Inventory selectById(@NotNull UUID id) {
        return super.selectById(id.toString());
    }

    @Override
    public @Nullable Inventory selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    public @Nullable List<byte[]> selectLegacy(@NotNull UUID id) {
        var sql = """
                select legacy from onesync.inventory
                where `uuid` = ?
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, id.toString());
                var rs = stm.executeQuery();
                if (rs.next()) {
                    return Inventory.BytesListTypeHandler.instance.getResult(rs, "legacy");
                } else {
                    return null;
                }
            }
        });
    }

    public int updateLegacy(@NotNull UUID id, @NotNull List<byte[]> legacy) {
        var sql = """
                update onesync.inventory
                set legacy = ?
                where `uuid` = ?
                """;
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                Inventory.BytesListTypeHandler.instance.setParameter(stm, 1, legacy);
                stm.setString(2, id.toString());
                return stm.executeUpdate();
            }
        });
    }

    public int insertOrUpdate(@NotNull Inventory entity) {
        var uuid = entity.uuid();
        if (uuid == null) {
            throw new IllegalArgumentException("uuid must not be null");
        }

        var sql = """
                replace into onesync.inventory (`uuid`, `name`, items, ender_items, legacy)
                values (?, ?, ?, ?, ?)
                """;

        return super.execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql);) {
                stm.setString(1, entity.uuid().toString());
                stm.setString(2, entity.name());
                Inventory.IntegerToBytesMapTypeHandler.instance.setParameter(stm, 3, entity.items());
                Inventory.IntegerToBytesMapTypeHandler.instance.setParameter(stm, 4, entity.enderItems());
                Inventory.BytesListTypeHandler.instance.setParameter(stm, 5, entity.legacy());
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
                    create table if not exists onesync.inventory
                    (
                        uuid        varchar(36)                        not null primary key,
                        name        varchar(32)                        not null,
                        items       json                               not null,
                        ender_items json                               not null ,
                        legacy      json                               not null,
                        created_at  datetime default CURRENT_TIMESTAMP not null,
                        updated_at  datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
                        constraint  uuid
                            unique (uuid)
                    );
                    """);
        });
    }

}
