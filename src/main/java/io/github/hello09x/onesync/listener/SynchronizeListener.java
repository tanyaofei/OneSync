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

public class SynchronizeListener implements Listener {

    public final static SynchronizeListener instance = new SynchronizeListener();

    private final SynchronizeManager synchronizeManager = SynchronizeManager.instance;

    private final static Logger log = Main.getInstance().getLogger();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var stopwatch = new StopWatch();
        try {
            stopwatch.start();
            synchronizeManager.prepare(event.getUniqueId(), event.getPlayerProfile().getName(), 2000);
            stopwatch.stop();
            log.info("为玩家 %s(%s) 准备数据完毕, 耗时 %dms".formatted(event.getPlayerProfile().getName(), event.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        } catch (TimeoutException e) {
            log.info("准备玩家 %s(%s) 数据超时".formatted(event.getPlayerProfile().getName(), event.getUniqueId()));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text("准备数据超时, 请联系管理员"));
        } catch (Throwable e) {
            log.severe(Throwables.getStackTraceAsString(e));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text("准备数据失败, 请联系管理员"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (!synchronizeManager.isPrepared(player.getUniqueId())) {
            player.kick(text("服务器尚未为你准备数据, 请尝试重新登陆"), PlayerKickEvent.Cause.PLUGIN);
            return;
        }

        try {
            synchronizeManager.applyPrepared(player);
        } catch (Throwable e) {
            player.kick(text("无法为你恢复玩家数据, 请联系管理员"), PlayerKickEvent.Cause.PLUGIN);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (synchronizeManager.isPrepared(player.getUniqueId())) {
            log.info("%s 存在未恢复数据, 本次「退出」游戏不会保存玩家数据。这可能是由于恢复数据时失败导致".formatted(player.getName()));
            return;
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        synchronizeManager.save(player, SnapshotCause.PLAYER_QUIT);
        stopwatch.stop();
        log.info("保存玩家 %s(%s) 数据完毕, 耗时: %dms".formatted(player.getName(), player.getUniqueId(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

}
