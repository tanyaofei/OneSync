package io.github.hello09x.onesync.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.onesync.manager.LockingManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class UnlockCommand {

    public final static UnlockCommand instance = new UnlockCommand();
    private final LockingManager manager = LockingManager.instance;

    private final static MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final static List<Component> MESSAGES = List.of(
            text("=========================================================================", GRAY),
            MINI_MESSAGE.deserialize("<b>「锁」</b>是用在玩家登陆时确保上一个服务器的数据写入完毕"),
            MINI_MESSAGE.deserialize("<gray> <green>+</green> 玩家<white>加入</white>服务器恢复数据后, 对玩家<white>上锁</white></gray>"),
            MINI_MESSAGE.deserialize("<gray> <green>+</green> 玩家<white>退出</white>服务器保存数据后, 对玩家<white>解锁</white></gray>"),
            empty(),
            MINI_MESSAGE.deserialize("<gray><i>当服务器异常关闭, 数据库异常, 没有完成解锁的过程时才需要执行此命令</i></gray>"),
            MINI_MESSAGE.deserialize("<gray>此命令作用后, 将会释放所有锁</gray>, <gold>并让所有服务器重新对当前在线的玩家上锁</gold>"),
            text("=========================================================================", GRAY),
            text(">> 解锁成功 <<", WHITE),
            text("=========================================================================", GRAY)
    );

    /**
     * 解锁指定玩家
     * @param sender
     * @param args
     */
    public void unlock(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        var player = (OfflinePlayer) Objects.requireNonNull(args.get("player"));
        if (player.getPlayer() != null) {
            sender.sendMessage(text("该玩家在线, 无须解锁", GRAY));
            return;
        }
        manager.relock(player);
        MESSAGES.forEach(sender::sendMessage);
    }

    public void unlockAll(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        manager.relockAll();
        MESSAGES.forEach(sender::sendMessage);
    }

}
