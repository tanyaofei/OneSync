package io.github.hello09x.onesync.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.MCUtils;
import io.github.hello09x.bedrock.util.PluginMessages;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OneSyncConfig;
import io.github.hello09x.onesync.constant.SubChannels;
import io.github.hello09x.onesync.repository.TeleportRepository;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import io.github.hello09x.onesync.repository.model.Teleport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@SuppressWarnings("UnstableApiUsage")
public class TeleportManager implements PluginMessageListener {

    public final static String SUB_CHANNEL = SubChannels.Teleport;

    public final static TeleportManager instance = new TeleportManager();

    private final static String CTL_ASK = "ask";
    private final static String CTL_ACCEPT = "accept";
    private final static String CTL_TELEPORT = "teleport";
    private final static String CTL_DENY = "deny";

    private final ServerManager serverList = ServerManager.instance;
    private final TeleportRepository repository = TeleportRepository.instance;
    private final OneSyncConfig.TeleportConfig config = OneSyncConfig.instance.getTeleport();

    private final Map<String, Pair<Location, MutableInt>> teleportLocations = MCUtils.isFolia()
            ? new ConcurrentHashMap<>()
            : new HashMap<>();

    public TeleportManager() {
        if (MCUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Main.getInstance(), task -> this.tick(), 20, 1);
            Bukkit.getAsyncScheduler().runAtFixedRate(Main.getInstance(), task -> this.removeExpiredTeleports(), 5, 5, TimeUnit.MINUTES);
        } else {
            Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::tick, 20, 1);
            Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), this::removeExpiredTeleports, 20 * 60 * 5, 20 * 60 * 5);
        }
    }

    private void tick() {
        this.teleportLocations.entrySet().removeIf(entry -> entry.getValue().getRight().decrementAndGet() <= 0);
    }

    private void removeExpiredTeleports() {
        repository.deleteByCreatedAtBefore(LocalDateTime.now().minus(config.getExpiresIn()));
    }

    /**
     * 发起一个传送请求
     *
     * @param requester 请求发送人
     * @param receiver  接收人
     * @param type      传送类型
     * @return 返回给发送人的提示消息
     */
    public @NotNull Component ask(@NotNull Player requester, @NotNull String receiver, @NotNull TeleportType type) {
        var after = LocalDateTime.now().plus(config.getExpiresIn());
        var existed = repository.selectLatestByRequesterAndReceiverAfter(requester.getName(), receiver, after);
        if (existed != null) {
            if (existed.type() == type) {
                repository.deleteByRequesterAndReceiver(requester.getName(), receiver);
            } else {
                return textOfChildren(text("你已经对 ", GRAY), text(receiver, WHITE), text(" 发起过传送请求了", GRAY));
            }
        }

        var teleport = new Teleport(requester.getName(), receiver, TeleportType.TP, null);
        repository.insert(teleport);

        var out = ByteStreams.newDataOutput();
        out.writeUTF(CTL_ASK);
        out.writeUTF(type.name());
        out.writeUTF(requester.getName());
        out.writeUTF(receiver);

        requester.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.box(SUB_CHANNEL, out));
        this.onPluginMessageReceived("BungeeCord", requester, PluginMessages.boxLocal(SUB_CHANNEL, out));
        return text("传送请求已发送, 等待对方接受", GRAY);
    }

    /**
     * 回应传送请求
     *
     * @param receiver  接收人
     * @param requester 发送人
     * @param accept    是否接收传送
     * @return 返回给接收人的提示消息
     */
    public @NotNull Component answer(@NotNull Player receiver, @Nullable String requester, boolean accept) {
        var after = LocalDateTime.now().plus(config.getExpiresIn());
        var teleport = requester == null
                ? repository.selectLatestByReceiverAfter(receiver.getName(), after)
                : repository.selectLatestByRequesterAndReceiverAfter(requester, receiver.getName(), after);

        if (teleport == null) {
            return text("你没有接收到任何传送请求", GRAY);
        }
        repository.deleteByRequesterAndReceiver(teleport.requester(), teleport.receiver());

        var out = ByteStreams.newDataOutput();
        out.writeUTF(accept ? CTL_ACCEPT : CTL_DENY);
        out.writeUTF(teleport.type().name());
        out.writeUTF(teleport.requester());
        out.writeUTF(teleport.receiver());
        receiver.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.box(SUB_CHANNEL, out));
        this.onPluginMessageReceived("BungeeCord", receiver, PluginMessages.boxLocal(SUB_CHANNEL, out));

        return textOfChildren(
                text("你"),
                accept ? text("接收了 ") : text("拒绝了 "),
                text(teleport.requester(), WHITE),
                text(" 的传送请求")
        ).color(GRAY);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        var in = PluginMessages.unbox(SUB_CHANNEL, message);
        if (in == null) {
            return;
        }

        switch (in.readUTF()) {
            case CTL_ASK -> this.onAsk(in);
            case CTL_ACCEPT -> this.onAccept(in);
            case CTL_DENY -> this.onDeny(in);
            case CTL_TELEPORT -> this.onTeleport(in);
        }
    }

    /**
     * 接收到 {@link #CTL_ASK}, 当接收人在此服务器时处理
     */
    public void onAsk(@NotNull ByteArrayDataInput in) {
        var type = TeleportType.valueOf(in.readUTF());
        var requester = in.readUTF();
        var receiver = Bukkit.getPlayerExact(in.readUTF());
        if (receiver == null) {
            return;
        }

        switch (type) {
            case TP -> receiver.sendMessage(textOfChildren(
                    text(requester, WHITE),
                    text(" 请求传送到你身边 ", GRAY),
                    text("[接受]", GREEN)
                            .clickEvent(ClickEvent.runCommand("/stpaccept " + requester))
                            .hoverEvent(HoverEvent.showText(text("/stpaccept"))),
                    space(),
                    text("[拒绝]", RED)
                            .clickEvent(ClickEvent.runCommand("/stpdeny" + requester))
                            .hoverEvent(HoverEvent.showText(text("/stpdeny")))
            ));
            case TPHERE -> receiver.sendMessage(textOfChildren(
                    text(requester, WHITE),
                    text(" 请求你传送过去 ", GRAY),
                    space(),
                    text("[接受]", GREEN)
                            .clickEvent(ClickEvent.runCommand("/stpaccept " + requester))
                            .hoverEvent(HoverEvent.showText(text("/stpaccept"))),
                    space(),
                    text("[拒绝]", RED)
                            .clickEvent(ClickEvent.runCommand("/stpdeny " + requester))
                            .hoverEvent(HoverEvent.showText(text("/stpdeny")))
            ));
        }
    }

    public @Nullable Location getTeleportLocation(@NotNull Player player) {
        return Optional.ofNullable(this.teleportLocations.remove(player.getName())).map(Pair::getKey).orElse(null);
    }

    /**
     * 接收到 {@link #CTL_ACCEPT}, 当被传送人在此服务器时处理
     * <ul>
     *     <li>发送一个 3 秒倒计时</li>
     *     <li>3 秒后如果玩家位置没有发生变化, 则发送 {@link #CTL_TELEPORT} 给目的服务器</li>
     * </ul>
     */
    public void onAccept(@NotNull ByteArrayDataInput in) {
        var type = TeleportType.valueOf(in.readUTF());
        var requester = in.readUTF();
        var receiver = in.readUTF();

        var teleportor = this.getAB(type, requester, receiver)[0];
        if (teleportor == null) {
            // 需要被传送的人不在此服务器
            return;
        }

        var pos = teleportor.getLocation();
        Runnable teleport0 = () -> {
            if (this.isMoved(pos, teleportor.getLocation())) {
                teleportor.sendMessage(text("你动了, 传送取消", GRAY));
            } else {
                var out = ByteStreams.newDataOutput();
                out.writeUTF(CTL_TELEPORT);
                out.writeUTF(type.name());
                out.writeUTF(requester);
                out.writeUTF(receiver);

                teleportor.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.box(SUB_CHANNEL, out));
                this.onPluginMessageReceived("BungeeCord", teleportor, PluginMessages.boxLocal(SUB_CHANNEL, out));
            }
        };

        var wait = config.getWait();
        if (wait <= 0) {
            teleport0.run();
        } else {
            if (MCUtils.isFolia()) {
                teleportor.getScheduler().runDelayed(Main.getInstance(), task -> teleport0.run(), null, wait * 20L);
            } else {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), teleport0, wait * 20L);
            }
            teleportor.showTitle(Title.title(
                    text("开始传送"),
                    text("请不要移动")
            ));
        }
    }

    /**
     * 接收到 {@link #CTL_DENY}, 当发送人在此服务器时处理
     * <p>告诉发送人他的传送请求被拒绝了</p>
     */
    public void onDeny(@NotNull ByteArrayDataInput in) {
        in.readUTF();   // type
        var requester = in.readUTF();
        var receiver = in.readUTF();

        var player = Bukkit.getPlayerExact(requester);
        if (player == null) {
            // 发起者不在此服务器
            return;
        }

        player.sendMessage(textOfChildren(text(receiver, WHITE), text(" 拒绝了你的传送请求", GRAY)));
    }

    /**
     * 接收到 {@link #CTL_TELEPORT}, 当接风人是此服务器时处理
     * <ul>
     *     <li>将坐标记录起来</li>
     *     <li>发送 BungeeCord 消息, 让传送人切换到此服务器</li>
     *     <li>当传送人加入游戏时, 将他传送到记录的坐标里</li>
     * </ul>
     */
    public void onTeleport(@NotNull ByteArrayDataInput in) {
        var type = TeleportType.valueOf(in.readUTF());
        var requester = in.readUTF();
        var receiver = in.readUTF();

        var player = this.getAB(type, requester, receiver)[1];
        if (player == null) {
            // 接风人不在此服务器
            return;
        }

        var teleportor = switch (type) {
            case TP -> requester;
            case TPHERE -> receiver;
        };
        this.teleportLocations.put(teleportor, Pair.of(player.getLocation(), new MutableInt(1200)));    // 玩家 Join 时会传送到这个地方

        var out = ByteStreams.newDataOutput();
        out.writeUTF("ConnectOther");
        out.writeUTF(teleportor);
        out.writeUTF(serverList.getCurrent());
        player.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.box(SUB_CHANNEL, out));
    }

    /**
     * 判断玩家是否移动了
     *
     * @param before 之前的位置
     * @param now    现在的位置
     * @return 是否移动了
     */
    private boolean isMoved(@NotNull Location before, @NotNull Location now) {
        if (before.getWorld() != now.getWorld()) {
            return true;
        }

        return before.getBlockX() == now.getBlockX()
                && before.getBlockY() == now.getBlockY()
                && before.getBlockZ() == now.getBlockZ();
    }

    /**
     * A teleport to -> B
     */
    private @Nullable Player @NotNull [] getAB(@NotNull TeleportType type, @NotNull String requester, @NotNull String receiver) {
        Player from, to;
        switch (type) {
            case TP -> {
                from = Bukkit.getPlayerExact(requester);
                to = Bukkit.getPlayerExact(receiver);
            }
            case TPHERE -> {
                from = Bukkit.getPlayerExact(receiver);
                to = Bukkit.getPlayerExact(requester);
            }
            default -> throw new UnsupportedOperationException();
        }
        return new Player[]{from, to};
    }

}
