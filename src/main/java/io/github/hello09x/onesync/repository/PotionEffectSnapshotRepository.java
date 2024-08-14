package io.github.hello09x.onesync.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.database.jdbc.JdbcTemplate;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.repository.model.PotionEffectSnapshot;
import io.github.hello09x.onesync.util.PotionEffectListTypeCodec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class PotionEffectSnapshotRepository {

    private final JdbcTemplate jdbc;
    private final PotionEffectSnapshot.PotionEffectSnapshotRowMapper rowMapper;
    private final PotionEffectListTypeCodec potionEffectListTypeCodec;

    @Inject
    public PotionEffectSnapshotRepository(JdbcTemplate jdbc, PotionEffectSnapshot.PotionEffectSnapshotRowMapper rowMapper, PotionEffectListTypeCodec potionEffectListTypeCodec) {
        this.jdbc = jdbc;
        this.rowMapper = rowMapper;
        this.potionEffectListTypeCodec = potionEffectListTypeCodec;
        this.initTables();
    }

    @CanIgnoreReturnValue
    public int insert(@NotNull PotionEffectSnapshot snapshot) {
        var sql = "insert into potion_effect_snapshot (snapshot_id, player_id, effects) values (?, ?, ?)";
        return jdbc.update(sql, snapshot.snapshotId(), snapshot.playerId().toString(), potionEffectListTypeCodec.serialize(snapshot.effects()));
    }

    public @Nullable PotionEffectSnapshot selectBySnapshotId(@NotNull Long snapshotId) {
        return jdbc.queryForObject("select * from potion_effect_snapshot where snapshot_id = ?", rowMapper, snapshotId);
    }

    @CanIgnoreReturnValue
    public int deleteBySnapshotIds(@NotNull List<Long> snapshotIdList) {
        return jdbc.update("delete from potion_effect_snapshot where snapshot_id in (?)", StringUtils.join(snapshotIdList, ","));
    }

    protected void initTables() {
        jdbc.execute("""
                    create table if not exists potion_effect_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        effects     json     not null comment '效果'
                    );
                    """);

        io.github.hello09x.devtools.core.utils.Exceptions.suppress(OneSync.getInstance(), () -> {
            jdbc.execute("""
                    create index potion_effect_snapshot_player_id_index
                        on potion_effect_snapshot (player_id);
                    """);
        }, false);
    }
}
