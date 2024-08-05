package io.github.hello09x.onesync.repository.model;

import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record TeleportRequest(

        @NotNull
        String requester,

        @NotNull
        String receiver,

        @NotNull
        TeleportType type,

        LocalDateTime createdAt


) {
        @Singleton
        public static class TeleportRequestRowMapper implements RowMapper<TeleportRequest> {

                @Override
                public @Nullable TeleportRequest mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
                        return new TeleportRequest(
                                rs.getString("requester"),
                                rs.getString("receiver"),
                                TeleportType.valueOf(rs.getString("type")),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        );
                }
        }



}
