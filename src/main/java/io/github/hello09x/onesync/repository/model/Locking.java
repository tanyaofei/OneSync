package io.github.hello09x.onesync.repository.model;

import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record Locking(

        UUID playerId,

        @NotNull
        String serverId,

        @NotNull
        LocalDateTime createdAt

) {

        @Singleton
        public static class LockingRowMapper implements RowMapper<Locking> {

                @Override
                public @Nullable Locking mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
                        return new Locking(
                                UUID.fromString(rs.getString("player_id")),
                                rs.getString("server_id"),
                                LocalDateTime.ofInstant(rs.getDate("created_at").toInstant(), ZoneId.systemDefault())
                        );
                }
        }


}
