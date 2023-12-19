package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotHandler;
import io.github.hello09x.onesync.repository.SnapshotRepository;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import io.github.hello09x.onesync.repository.model.Snapshot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class SnapshotManager {

    public final static SnapshotManager instance = new SnapshotManager();
    private final static Logger log = Main.getInstance().getLogger();
    private final SnapshotRepository repository = SnapshotRepository.instance;

    /**
     * 创建快照
     *
     * @param player 玩家
     * @param cause  创建原因
     */
    public void create(@NotNull Player player, @NotNull SnapshotCause cause) {
        var snapshotId = repository.insert(new Snapshot(
                null,
                player.getUniqueId(),
                cause,
                null
        ));

        for (var handler : SnapshotHandler.HANDLERS) {
            try {
                handler.save(snapshotId, player);
            } catch (Throwable e) {
                log.severe("保存 %s(%s) 「%s」快照失败\n%s".formatted(
                        player.getName(),
                        player.getUniqueId(),
                        handler.snapshotType(),
                        Throwables.getStackTraceAsString(e)
                ));
            }
        }
    }


}
