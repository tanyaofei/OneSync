package io.github.hello09x.onesync.util;

import io.github.hello09x.bedrock.database.typehandler.TypeHandler;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NamespacedKeyTypeHandler implements TypeHandler<NamespacedKey> {

    public final static NamespacedKeyTypeHandler instance = new NamespacedKeyTypeHandler();

    @Override
    public void setParameter(@NotNull PreparedStatement stm, int i, @NotNull NamespacedKey value) throws SQLException {
        stm.setString(i, value.asString());
    }

    @Override
    public @Nullable NamespacedKey getResult(@NotNull ResultSet rs, @NotNull String columnName) throws SQLException {
        var value = rs.getString(columnName);
        if (value == null) {
            return null;
        }

        var split = value.split(":", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid namespaced key: " + value);
        }
        return new NamespacedKey(split[0], split[1]);
    }
}
