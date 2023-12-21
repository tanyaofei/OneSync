package io.github.hello09x.onesync.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.hello09x.bedrock.database.typehandler.TypeHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ItemStackMapTypeHandler implements TypeHandler<Map<Integer, ItemStack>> {

    public final static ItemStackMapTypeHandler instance = new ItemStackMapTypeHandler();

    private static final Gson GSON = new Gson();
    private final static TypeToken<Map<Integer, byte[]>> BYTES_LIST_TYPE = new TypeToken<>() {
    };

    @Override
    public void setParameter(
            @NotNull PreparedStatement stm,
            int i,
            @NotNull Map<Integer, ItemStack> value
    ) throws SQLException {
        var bytes = new HashMap<Integer, byte[]>(value.size(), 1.0F);
        for (var entry : value.entrySet()) {
            var item = entry.getValue();
            bytes.put(entry.getKey(), item == null ? null : item.serializeAsBytes());
        }
        stm.setString(i, GSON.toJson(bytes));
    }

    @Override
    public @Nullable Map<Integer, ItemStack> getResult(@NotNull ResultSet rs, @NotNull String columnName) throws SQLException {
        var value = rs.getString(columnName);
        if (value == null) {
            return null;
        }

        var bytes = GSON.fromJson(value, BYTES_LIST_TYPE);
        var items = new HashMap<Integer, ItemStack>(bytes.size(), 1.0F);
        for (var entry : bytes.entrySet()) {
            items.put(entry.getKey(), ItemStack.deserializeBytes(entry.getValue()));
        }
        return items;
    }
}
