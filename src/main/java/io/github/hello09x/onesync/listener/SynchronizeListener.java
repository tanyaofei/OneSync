package io.github.hello09x.onesync.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SynchronizeListener extends PacketAdapter implements Listener {

    public final static SynchronizeListener instance = new SynchronizeListener();
    private final static Logger log = Main.getInstance().getLogger();
    private final SynchronizeManager synchronizeManager = SynchronizeManager.instance;

    public SynchronizeListener() {
        super(
                Main.getInstance(),
                ListenerPriority.LOWEST,
                List.of(PacketType.Configuration.Server.FINISH_CONFIGURATION),
                ListenerOptions.ASYNC
        );
    }

    @Override
    public void onPacketSending(@NotNull PacketEvent event) {
        this.onPreJoin(event);
    }

    /**
     * 玩家在 FINISH_CONFIGURATION 之后预加载数据
     *
     * @see #onQuit(PlayerQuitEvent) 执行时, 会先阻塞等待另外一个服务器保存数据
     */
    public void onPreJoin(@NotNull PacketEvent event) {
        var stopwatch = new StopWatch();
        var player = event.getPlayer();
        stopwatch.start();
        synchronizeManager.prepare(player.getUniqueId(), player.getName(), 1000);
        stopwatch.stop();
        if (Main.isDebugging()) {
            log.info("加载 %s(%s) 数据完毕, 耗时 %d ms".formatted(player.getName(), player.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        }
    }

    /**
     * 玩家加入游戏的时候恢复预加载的数据
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var stopwatch = new StopWatch();
        stopwatch.start();
        synchronizeManager.applyPrepared(player);
        stopwatch.stop();
        if (Main.isDebugging()) {
            log.info("恢复 %s 数据完毕, 耗时 %d ms".formatted(player.getName(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        }
    }

    /**
     * 玩家退出游戏时
     *
     * @see #onPreJoin(PacketEvent) 玩家切换服务器时, 另外一个服务器加载数据前, 会等待此方法执行完毕
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (synchronizeManager.shouldNotSaveSnapshot(player.getUniqueId())) {
            if (Main.isDebugging()) {
                log.warning("[退出游戏] %s 存在未恢复数据, 本次操作不会保存数据。这可能是由于恢复数据时发生错误导致的".formatted(player.getName()));
            }
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        synchronizeManager.save(player, SnapshotCause.PLAYER_QUIT);
        stopwatch.stop();
        if (Main.isDebugging()) {
            log.info("玩家 %s 正在恢复数据, 此此「死亡」不会创建快照".formatted(player.getName()));
        }
    }

}
