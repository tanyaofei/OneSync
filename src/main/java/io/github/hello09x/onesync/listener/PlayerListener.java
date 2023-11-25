package io.github.hello09x.onesync.listener;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.LockingManager;
import io.github.hello09x.onesync.manager.SynchronizeManager;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class PlayerListener implements Listener {

    public final static PlayerListener instance = new PlayerListener();
    private final static Logger log = Main.getInstance().getLogger();
    private final SynchronizeManager manager = SynchronizeManager.instance;
    private final LockingManager locks = LockingManager.instance;

    private final Set<UUID> joinDisallowed = new HashSet<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var stopwatch = new StopWatch();
        stopwatch.start();
        var uuid = event.getUniqueId();
        if (this.locks.isLocked(uuid)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text("玩家数据同步中", WHITE));
            return;
        }
        this.manager.prepare(uuid);
        stopwatch.stop();
        log.info("%s 数据准备完毕(异步), 耗时: %dms".formatted(event.getPlayerProfile().getName(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
    }

    /**
     * 防止其他插件拦截了登陆后, 使用了过期数据登陆
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin2(@NotNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            this.manager.removePrepared(event.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        try {
            if (!this.manager.restore(event.getPlayer())) {
                player.kick(
                        text("玩家数据未就绪", RED),
                        PlayerKickEvent.Cause.PLUGIN
                );
                this.joinDisallowed.add(player.getUniqueId());
            } else {
                this.joinDisallowed.remove(player.getUniqueId());
            }
        } catch (Throwable e) {
            player.kick(
                    text("同步玩家数据异常, 请联系管理员", RED),
                    PlayerKickEvent.Cause.PLUGIN
            );
            this.joinDisallowed.add(player.getUniqueId());
            log.severe("玩家数据同步异常\n" + Throwables.getStackTraceAsString(e));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var stopwatch = new StopWatch();
        stopwatch.start();

        var player = event.getPlayer();
        if (this.joinDisallowed.contains(player.getUniqueId())) {
            // 由于数据未就绪而踢掉的玩家不保存数据
            return;
        }

        var success = this.manager.save(player, true);
        stopwatch.stop();

        if (success) {
            log.info("%s 的玩家数据已成功保存, 耗时 %dms".formatted(player.getName(), stopwatch.getTime(TimeUnit.MILLISECONDS)));
        } else {
            log.info("%s 部分玩家数据没有完全保存".formatted(player.getName()));
        }

        this.locks.setLock(player, false);
    }

}
