package io.github.hello09x.onesync.repository.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.database.typehandler.TypeHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Table("onesync.inventory")
public record Inventory(

        @TableId("uuid")
        UUID uuid,

        @TableField("name")
        String name,

        @TableField(value = "items", typeHandler = IntegerToBytesMapTypeHandler.class)
        Map<Integer, byte[]> items,

        @TableField(value = "ender_items", typeHandler = IntegerToBytesMapTypeHandler.class)
        Map<Integer, byte[]> enderItems,

        @TableField(value = "legacy", typeHandler = BytesListTypeHandler.class)
        List<byte[]> legacy,

        @TableField("created_at")
        LocalDateTime createdAt,

        @TableField("updated_at")
        LocalDateTime updatedAt

) {

        final static Gson gson = new Gson();

        public static class BytesListTypeHandler implements TypeHandler<List<byte[]>> {

                public final static BytesListTypeHandler instance = new BytesListTypeHandler();

                private final static TypeToken<List<byte[]>> TYPE = new TypeToken<>() {
                };

                @Override
                public void setParameter(@NotNull PreparedStatement stm, int i, @NotNull List<byte[]> value) throws SQLException {
                        stm.setString(i, gson.toJson(value));
                }

                @Override
                public @Nullable List<byte[]> getResult(@NotNull ResultSet rs, @NotNull String columnName) throws SQLException {
                        return Optional.ofNullable(rs.getString(columnName))
                                .map(json -> gson.fromJson(json, TYPE))
                                .orElse(null);
                }
        }


        public static class IntegerToBytesMapTypeHandler implements TypeHandler<Map<Integer, byte[]>> {

                public final static IntegerToBytesMapTypeHandler instance = new IntegerToBytesMapTypeHandler();

                private final static TypeToken<Map<Integer, byte[]>> TYPE = new TypeToken<>() {
                };

                @Override
                public void setParameter(@NotNull PreparedStatement stm, int i, @NotNull Map<Integer, byte[]> value) throws SQLException {
                        stm.setString(i, gson.toJson(value));
                }

                @Override
                public @Nullable Map<Integer, byte[]> getResult(@NotNull ResultSet rs, @NotNull String columnName) throws SQLException {
                        return Optional
                                .ofNullable(rs.getString(columnName))
                                .map(json -> gson.fromJson(json, TYPE))
                                .orElse(null);
                }
        }


}
