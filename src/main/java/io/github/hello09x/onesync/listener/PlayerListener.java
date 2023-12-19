package io.github.hello09x.onesync.listener;

import com.google.common.base.Throwables;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;

public class PlayerListener implements Listener {

    public final static PlayerListener instance = new PlayerListener();

    private final SynchronizeManager synchronizeManager = SynchronizeManager.instance;

    private final static Logger log = Main.getInstance().getLogger();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var stopwatch = new StopWatch();
        try {
            stopwatch.start();
            synchronizeManager.prepare(event.getUniqueId(), 1000);
            stopwatch.stop();
        } catch (TimeoutException e) {
            log.info("为玩家 %s(%s) 准备数据超时".formatted(event.getPlayerProfile().getName(), event.getUniqueId()));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text("准备数据超时, 请联系管理员"));
        } catch (Throwable e) {
            log.severe(Throwables.getStackTraceAsString(e));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text("无法为你准备玩家数据, 请联系管理员"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        try {
            synchronizeManager.applyPrepared(event.getPlayer());
        } catch (Throwable e) {
            event.getPlayer().kick(text("无法为你恢复玩家数据, 请联系管理员"), PlayerKickEvent.Cause.PLUGIN);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        var stopwatch = new StopWatch();
        stopwatch.start();
        synchronizeManager.save(player, SnapshotCause.PLAYER_QUIT);
        stopwatch.stop();
        log.info("保存玩家 %s(%s) 数据完毕, 耗时: %dms".formatted(player.getName(), player.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

}
