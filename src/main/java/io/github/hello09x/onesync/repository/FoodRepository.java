package io.github.hello09x.onesync.repository;

import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.repository.model.Food;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.UUID;

public class FoodRepository extends Repository<Food> {

    public final static FoodRepository instance = new FoodRepository(Main.getInstance());

    private FoodRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public @Nullable Food selectById(@NotNull UUID id) {
        return super.selectById(id.toString());
    }

    public int insertOrUpdate(@NotNull Food food) {
        var sql = """
                replace into onesync.food (uuid, level, saturation)
                values (?, ?, ?)
                """;

        return super.execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setString(1, food.uuid().toString());
                stm.setInt(2, food.level());
                stm.setFloat(3, food.saturation());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    public @Nullable Food selectById(@NotNull Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initTables() {
        super.execute(connection -> {
            var stm = connection.createStatement();
            stm.execute("create database if not exists onesync");
            stm.execute("""
                    create table if not exists onesync.food
                    (
                        uuid        varchar(36) not null
                            primary key,
                        level       int         not null,
                        saturation  float       not null
                    );
                    """);
        });
    }

}
