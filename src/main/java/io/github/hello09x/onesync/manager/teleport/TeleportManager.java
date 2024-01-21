package io.github.hello09x.onesync.manager.teleport;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.BungeeCord;
import io.github.hello09x.bedrock.util.Folia;
import io.github.hello09x.bedrock.util.Locations;
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
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@SuppressWarnings("UnstableApiUsage")
public class TeleportManager implements PluginMessageListener {

    public final static String SUB_CHANNEL = SubChannels.Teleport;

    public final static TeleportManager instance = new TeleportManager();
    private final static Logger log = Main.getInstance().getLogger();

    private final static String CTL_ASK = "ASK";
    private final static String CTL_ACCEPT = "ACCEPT";
    private final static String CTL_DENY = "DENY";
    private final static String CTL_IGNORE = "IGNORE";
    private final static String CTL_TELEPORT = "TELEPORT";

    private final ServerManager serverList = ServerManager.instance;
    private final WarmupManager warmupManager = WarmupManager.instance;
    private final TeleportRepository repository = TeleportRepository.instance;
    private final OneSyncConfig.TeleportConfig config = OneSyncConfig.instance.getTeleport();

    private final Map<String, Pair<Location, MutableInt>> teleportLocations = Folia.isFolia()
            ? new ConcurrentHashMap<>()
            : new HashMap<>();

    public TeleportManager() {
        Folia.runTaskTimer(Main.getInstance(), this::tickExpiredTeleportLocation, 20, 1);
        if (Folia.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(Main.getInstance(), task -> this.tickExpiredTeleports(), 5, 5, TimeUnit.MINUTES);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), this::tickExpiredTeleports, 20 * 60 * 5, 20 * 60 * 5);
        }
    }

    private void tickExpiredTeleportLocation() {
        this.teleportLocations.entrySet().removeIf(entry -> entry.getValue().getRight().decrementAndGet() <= 0);
    }

    private void tickExpiredTeleports() {
        repository.deleteByCreatedAtBefore(LocalDateTime.now().minus(config.getExpiresIn()));
    }

    /**
     * 发起一个传送请求
     *
     * @param requester 发送方
     * @param receiver  接收方
     * @param type      传送类型
     * @return 返回给发送方的提示消息
     */
    public @NotNull Component ask(@NotNull Player requester, @NotNull String receiver, @NotNull TeleportType type, boolean force) {
        var expirations = LocalDateTime.now().plus(config.getExpiresIn());
        var existed = repository.selectLatestByRequesterAndReceiverAfter(requester.getName(), receiver, expirations);
        if (existed != null && !force) {
            if (existed.type() != type) {
                repository.deleteByRequesterAndReceiver(requester.getName(), receiver);
            } else {
                return textOfChildren(text("你已经对 ", GRAY), text(receiver, WHITE), text(" 发起过传送请求了", GRAY));
            }
        }

        var teleport = new Teleport(requester.getName(), receiver, type, null);
        repository.insert(teleport);

        var out = ByteStreams.newDataOutput();
        out.writeUTF(CTL_ASK);
        out.writeUTF(type.name());
        out.writeUTF(requester.getName());
        out.writeUTF(receiver);
        out.writeBoolean(force);
        var message = out.toByteArray();

        BungeeCord.sendPluginMessage(Main.getInstance(), requester, BungeeCord.asForward(SUB_CHANNEL, message));
        this.onPluginMessageReceived(BungeeCord.CHANNEL, requester, BungeeCord.asReceivedForward(SUB_CHANNEL, message));

        return textOfChildren(
                text("传送请求已发送给 "), text(receiver, WHITE), text(", 等待对方接受"),
                text(" [取消]", RED)
                        .clickEvent(ClickEvent.runCommand("/stpcancel " + receiver))
                        .hoverEvent(HoverEvent.showText(text("/stpacancel " + receiver)))
        ).color(GRAY);
    }

    /**
     * 回应传送请求
     *
     * @param receiver  接收方
     * @param requester 发送方
     * @param accept    是否接受传送
     * @return 返回给接收方的提示消息
     */
    public @NotNull Component answer(@NotNull Player receiver, @Nullable String requester, boolean accept) {
        return this.answer(receiver, requester, accept, false);
    }

    /**
     * 回应传送请求
     *
     * @param receiver  接收方
     * @param requester 发送方
     * @param accept    是否接受传送
     * @param force     是否是强制接受
     * @return 返回给接受人的消息
     */
    public @NotNull Component answer(@NotNull Player receiver, @Nullable String requester, boolean accept, boolean force) {
        var expirations = LocalDateTime.now().plus(config.getExpiresIn());
        var teleport = requester == null
                ? repository.selectLatestByReceiverBefore(receiver.getName(), expirations)
                : repository.selectLatestByRequesterAndReceiverAfter(requester, receiver.getName(), expirations);

        if (teleport == null) {
            return text("你没有接收到任何传送请求", GRAY);
        }
        repository.deleteByRequesterAndReceiver(teleport.requester(), teleport.receiver());

        var out = ByteStreams.newDataOutput();
        out.writeUTF(accept ? CTL_ACCEPT : CTL_DENY);
        out.writeUTF(teleport.type().name());
        out.writeUTF(teleport.requester());
        out.writeUTF(teleport.receiver());
        out.writeBoolean(force);
        var message = out.toByteArray();

        BungeeCord.sendPluginMessage(Main.getInstance(), receiver, BungeeCord.asForward(SUB_CHANNEL, message));
        this.onPluginMessageReceived(BungeeCord.CHANNEL, receiver, BungeeCord.asReceivedForward(SUB_CHANNEL, message));

        return textOfChildren(
                text("你"),
                accept ? text("接受了 ") : text("拒绝了 "),
                text(teleport.requester(), WHITE),
                text(" 的传送请求")
        ).color(GRAY);
    }

    /**
     * 回应没有启用跨服传送
     *
     * @param receiver  接收方
     * @param requester 发送方
     */
    public void ignore(@NotNull Player receiver, @NotNull String requester) {
        repository.deleteByRequesterAndReceiver(requester, receiver.getName());

        var out = ByteStreams.newDataOutput();
        out.writeUTF(CTL_IGNORE);
        out.writeUTF(requester);
        out.writeUTF(receiver.getName());
        var message = out.toByteArray();

        BungeeCord.sendPluginMessage(Main.getInstance(), receiver, BungeeCord.asForward(SUB_CHANNEL, message));
        this.onPluginMessageReceived(BungeeCord.CHANNEL, receiver, BungeeCord.asReceivedForward(SUB_CHANNEL, message));
    }

    /**
     * 取消传送请求
     *
     * @param requester 发送方
     * @param receiver  接收方
     * @return 返回给发送方的提示消息
     */
    public @NotNull Component cancel(@NotNull Player requester, @Nullable String receiver) {
        var expirations = LocalDateTime.now().plus(config.getExpiresIn());
        var teleport = receiver == null
                ? repository.selectLatestByRequesterBefore(requester.getName(), expirations)
                : repository.selectLatestByRequesterAndReceiverAfter(requester.getName(), receiver, expirations);

        if (teleport == null) {
            return text("你没有发起任何传送请求", GRAY);
        }

        repository.deleteByRequesterAndReceiver(teleport.requester(), teleport.receiver());
        return text("取消传送请求成功", GRAY);
    }

    /**
     * 获取玩家在此服务器传送的坐标点
     * <p>当玩家从 A 服务器传送到 B 服务器时, B 服务器会记录传送坐标点, 然后发送消息给 BungeeCord 让玩家切换服务器, 等加入游戏时传送他</p>
     * <p><b>玩家有可能切换服务器后传送失败</b></p>
     *
     * @param player 玩家
     * @return 传送坐标点
     */
    public @Nullable Location getTeleportLocation(@NotNull Player player) {
        return Optional.ofNullable(this.teleportLocations.remove(player.getName())).map(Pair::getLeft).orElse(null);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(BungeeCord.CHANNEL)) {
            return;
        }

        var in = BungeeCord.parseReceivedForward(SUB_CHANNEL, message);
        if (in == null) {
            return;
        }

        switch (in.readUTF()) {
            case CTL_ASK -> this.onAsk(in);
            case CTL_IGNORE -> this.onIgnored(in);
            case CTL_ACCEPT -> this.onAccept(in);
            case CTL_DENY -> this.onDeny(in);
            case CTL_TELEPORT -> this.onTeleport(in);
        }
    }

    /**
     * 接收到 {@link #CTL_ASK}, 当接收方在此服务器时处理
     */
    public void onAsk(@NotNull ByteArrayDataInput in) {
        var type = TeleportType.valueOf(in.readUTF());
        var requester = in.readUTF();
        var receiver = Bukkit.getPlayerExact(in.readUTF());
        if (receiver == null) {
            return;
        }

        if (!config.isEnabled()) {
            this.ignore(receiver, requester);
            return;
        }

        var force = in.readBoolean();
        if (force) {
            this.answer(receiver, requester, true, true);
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

        receiver.playSound(sound(Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.8F, 1.0F));
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
        var force = in.readBoolean();

        // 发送方在此服务器
        Optional.ofNullable(Bukkit.getPlayerExact(requester)).ifPresent(p -> p.sendMessage(textOfChildren(
                text(receiver, WHITE),
                text(" 已接受你的传送请求", GRAY)
        )));

        var teleported = this.getAB(type, requester, receiver)[0];
        if (teleported == null) {
            // 需要被传送的人不在此服务器
            return;
        }

        Runnable doTeleport = () -> {
            var out = ByteStreams.newDataOutput();
            out.writeUTF(CTL_TELEPORT);
            out.writeUTF(type.name());
            out.writeUTF(requester);
            out.writeUTF(receiver);
            var message = out.toByteArray();

            BungeeCord.sendPluginMessage(Main.getInstance(), teleported, BungeeCord.asForward(SUB_CHANNEL, message));
            this.onPluginMessageReceived(BungeeCord.CHANNEL, teleported, BungeeCord.asReceivedForward(SUB_CHANNEL, message));
        };

        var warmup = config.getWarmup();
        if (warmup <= 0 || force) {
            doTeleport.run();
        } else {
            teleported.showTitle(Title.title(
                    text("正在准备传送", GOLD),
                    text("请不要移动", GOLD),
                    Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(warmup), Duration.ofMillis(250))
            ));

            // 延迟 10 tick 再开始预热, 让玩家接收到 title 提示后有一点反应时间
            Folia.runTaskLater(Main.getInstance(), teleported, () -> {
                if (!teleported.isOnline()) {
                    return;
                }
                var pos = teleported.getLocation();
                warmupManager.add(teleported, new WarmupManager.Warmup(
                                x -> {
                                    if (this.isMoved(pos, teleported.getLocation())) {
                                        teleported.resetTitle();
                                        teleported.sendMessage(text("你动了, 行动取消", GRAY));
                                        return false;
                                    }
                                    if (config.isParticle()) {
                                        var center = teleported.getEyeLocation();
                                        var points = Locations.getCirclePoints(center, 0.5, 10);
                                        for (var point : points) {
                                            point.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, point, 1, 0.125D, 0, 0.125D);
                                        }
                                    }
                                    return true;
                                },
                                x -> doTeleport.run(),
                                new MutableInt(warmup * 20)
                        )
                );
            }, 10);
        }
    }

    /**
     * 接收到 {@link #CTL_DENY}, 当发送方在此服务器时处理
     * <p>告诉发送方他的传送请求被拒绝了</p>
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
     * 接收到 {@link #CTL_IGNORE}, 当发送方在此服务器时处理
     * <p>告诉发送方对方所在服务器没有启用跨服传送</p>
     */
    public void onIgnored(@NotNull ByteArrayDataInput in) {
        var requester = in.readUTF();
        var receiver = in.readUTF();

        var player = Bukkit.getPlayerExact(requester);
        if (player == null) {
            // 发起者不在此服务器
            return;
        }

        player.sendMessage(textOfChildren(text(receiver, WHITE), text(" 所在的服务器没有开启跨服传送", GRAY)));
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

        var teleported = switch (type) {
            case TP -> requester;
            case TPHERE -> receiver;
        };

        var t = Bukkit.getPlayerExact(teleported);
        if (t != null) {
            // 两个人都在同一个服务器
            t.teleportAsync(player.getLocation()).thenAccept(success -> {
                if (!success) {
                    t.sendMessage(text("传送失败: 可能被第三方插件取消或者你被骑了", RED));
                } else {
                    if (config.isSound()) {
                        t.getLocation().getWorld().playSound(
                                t.getLocation(),
                                Sound.ENTITY_ENDERMAN_TELEPORT,
                                SoundCategory.PLAYERS,
                                0.8F,
                                1.0F
                        );
                    }
                }
            });
        } else {
            // 传送放不在服务器, 让他切换服务器
            this.teleportLocations.put(teleported, Pair.of(player.getLocation(), new MutableInt(1200)));
            if (!BungeeCord.connectOther(Main.getInstance(), teleported, serverList.getCurrent())) {
                this.teleportLocations.remove(teleported);
            }
        }
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

        return before.getBlockX() != now.getBlockX()
               || before.getBlockY() != now.getBlockY()
               || before.getBlockZ() != now.getBlockZ();
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
