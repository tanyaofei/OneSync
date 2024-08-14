package io.github.hello09x.onesync.command.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.MenuManager;
import io.github.hello09x.onesync.manager.SnapshotManager;
import io.github.hello09x.onesync.repository.constant.SnapshotCause;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

@Singleton
public class SnapshotCommand {

    private final static MiniMessage miniMessage = MiniMessage.miniMessage();
    private final MenuManager menus;
    private final SnapshotManager manager;

    @Inject
    public SnapshotCommand(MenuManager menus, SnapshotManager manager) {
        this.menus = menus;
        this.manager = manager;
    }

    /**
     * 打开快照菜单
     */
    public void snapshot(@NotNull Player sender, @NotNull CommandArguments args) {
        var player = (OfflinePlayer) Objects.requireNonNull(args.get("player"));
        var page = (int) args.getOptional("page").orElse(1);
        menus.openSnapshotPage(sender, page, player);
    }

    /**
     * 创建指定玩家的快照
     */
    public void save(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        @SuppressWarnings("unchecked")
        var players = (List<? extends Player>) args.getOptional("players").orElse(Collections.emptyList());
        int total = players.size();
        if (total > 10) {
            sender.sendMessage(text("开始保存 %d 名玩家数据, 这可能需要一点时间".formatted(total), GRAY));
        }

        var stopwatch = new StopWatch();
        stopwatch.start();
        int success = manager.create(players, SnapshotCause.COMMAND);
        stopwatch.stop();
        sender.sendMessage(miniMessage.deserialize(
                "<gray>保存 <success> 名玩家数据完毕, 耗时 <time> ms</gray>",
                Placeholder.component("success", text(success, WHITE)),
                Placeholder.component("time", text(stopwatch.getTime(TimeUnit.MILLISECONDS), WHITE))
        ));

        if (success != total) {
            sender.sendMessage(text("部分玩家正在恢复数据中, 不会进行保存", GRAY));
        }
    }

}
