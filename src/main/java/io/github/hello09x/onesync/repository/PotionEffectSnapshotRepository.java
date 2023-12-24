package io.github.hello09x.onesync.repository;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.util.PotionEffectListTypeHandler;
import io.github.hello09x.bedrock.database.Repository;
import io.github.hello09x.onesync.repository.model.PotionEffectSnapshot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class PotionEffectSnapshotRepository extends Repository<PotionEffectSnapshot> {

    public final static PotionEffectSnapshotRepository instance = new PotionEffectSnapshotRepository(Main.getInstance());

    public PotionEffectSnapshotRepository(@NotNull Plugin plugin) {
        super(plugin);
    }

    public int insert(@NotNull PotionEffectSnapshot snapshot) {
        var sql = "insert into potion_effect_snapshot (snapshot_id, player_id, effects) values (?, ?, ?)";
        return execute(connection -> {
            try (PreparedStatement stm = connection.prepareStatement(sql)) {
                stm.setLong(1, snapshot.snapshotId());
                stm.setString(2, snapshot.playerId().toString());
                PotionEffectListTypeHandler.instance.setParameter(stm, 3, snapshot.effects());
                return stm.executeUpdate();
            }
        });
    }

    @Override
    protected void initTables() {
        execute(connection -> {
            Statement stm = connection.createStatement();
            var rs = stm.executeQuery("select * from information_schema.INNODB_TABLES where name = '%s'".formatted(connection.getCatalog() + "/" + "potion_effect_snapshot"));
            if (rs.next()) {
                return;
            }
            stm.executeUpdate("""
                    create table potion_effect_snapshot
                    (
                        snapshot_id bigint   not null comment '快照 ID'
                            primary key,
                        player_id   char(36) not null comment '玩家 ID',
                        effects     json     not null comment '效果'
                    );
                    """);
            stm.executeUpdate("""
                    create index potion_effect_snapshot_player_id_index
                        on potion_effect_snapshot (player_id);
                    """);
        });
    }
}
