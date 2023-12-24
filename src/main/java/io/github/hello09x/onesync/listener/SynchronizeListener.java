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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;

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

    public void onPreJoin(@NotNull PacketEvent event) {
        var stopwatch = new StopWatch();
        var player = event.getPlayer();
        stopwatch.start();
        synchronizeManager.prepare(player.getUniqueId(), player.getName(), 2000);
        stopwatch.stop();
        log.info("加载 %s(%s) 数据完毕, 耗时 %d ms".formatted(player.getName(), player.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        try {
            synchronizeManager.applyPrepared(player);
        } catch (Throwable e) {
            player.kick(text("无法为你恢复玩家数据, 请联系管理员"), PlayerKickEvent.Cause.PLUGIN);
        }
    }

    /**
     * 玩家退出服务器时预加载数据
     * <p><b>玩家切换服务器是先调用另外一个服务器的 {@link AsyncPlayerPreLoginEvent} 后再调用此事件</b></p>
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (synchronizeManager.shouldNotSaveSnapshot(player.getUniqueId())) {
            log.info("[退出游戏] %s 存在未恢复数据, 本次操作不会保存数据。这可能是由于恢复数据时发生错误导致的".formatted(player.getName()));
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        synchronizeManager.save(player, SnapshotCause.PLAYER_QUIT);
        stopwatch.stop();
        log.info("[退出游戏] - 保存玩家 %s(%s) 数据完毕, 耗时: %dms".formatted(player.getName(), player.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

}
