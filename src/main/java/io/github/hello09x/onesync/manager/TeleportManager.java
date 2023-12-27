package io.github.hello09x.onesync.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.pluginmessage.PluginMessages;
import io.github.hello09x.bedrock.util.MCUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.constant.SubChannels;
import io.github.hello09x.onesync.repository.TeleportRepository;
import io.github.hello09x.onesync.repository.constant.TeleportType;
import io.github.hello09x.onesync.repository.model.Teleport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@SuppressWarnings("UnstableApiUsage")
public class TeleportManager implements PluginMessageListener {

    public final static String SUB_CHANNEL = SubChannels.Teleport;

    public final static TeleportManager instance = new TeleportManager();
    private final static String COMMAND_ASK = "ask";
    private final static String COMMAND_ACCEPT = "accept";
    private final static String COMMAND_DENY = "deny";
    private final ServerManager serverList = ServerManager.instance;
    private final TeleportRepository repository = TeleportRepository.instance;

    private final Map<String, Pair<Location, MutableInt>> teleportLocations = MCUtils.isFolia()
            ? new ConcurrentHashMap<>()
            : new HashMap<>();

    public TeleportManager() {
        if (MCUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Main.getInstance(), task -> this.cleanTeleportLocations(), 20, 1);
        } else {
            Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::cleanTeleportLocations, 20, 1);
        }
    }

    private void cleanTeleportLocations() {
        this.teleportLocations.entrySet().removeIf(entry -> entry.getValue().getRight().decrementAndGet() <= 0);
    }

    public @NotNull Component ask(@NotNull Player requester, @NotNull String receiver, @NotNull TeleportType type) {
        var existed = repository.selectLatestByRequesterAndReceiver(requester.getName(), receiver);
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
        out.writeUTF(COMMAND_ASK);
        out.writeUTF(type.name());
        out.writeUTF(requester.getName());
        out.writeUTF(receiver);

        requester.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.asForwardMessage(SUB_CHANNEL, out));
        this.onPluginMessageReceived("BungeeCord", requester, PluginMessages.asLocalForwardMessage(SUB_CHANNEL, out));
        return text("传送请求已发送, 等待对方接受", GRAY);
    }

    public @Nullable Component accept(@NotNull Player receiver, @Nullable String requester) {
        var teleport = requester == null
                ? repository.selectLatestByReceiver(receiver.getName())
                : repository.selectLatestByRequesterAndReceiver(requester, receiver.getName());

        if (teleport == null) {
            return text("你没有接收到任何传送请求", GRAY);
        }

        var out = ByteStreams.newDataOutput();
        out.writeUTF(COMMAND_ACCEPT);
        out.writeUTF(teleport.type().name());
        out.writeUTF(teleport.requester());
        out.writeUTF(receiver.getName());

        receiver.sendPluginMessage(Main.getInstance(), "BungeeCord", PluginMessages.asForwardMessage(SUB_CHANNEL, out));
        this.onPluginMessageReceived("BungeeCord", receiver, PluginMessages.asLocalForwardMessage(SUB_CHANNEL, out));

        repository.deleteByRequesterAndReceiver(teleport.requester(), teleport.receiver());
        return null;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        var in = PluginMessages.parseForwardMessage(SUB_CHANNEL, message);
        if (in == null) {
            return;
        }

        switch (in.readUTF()) {
            case COMMAND_ASK -> this.onReceiveAsk(in);
            case COMMAND_ACCEPT -> this.onReceiveAccept(in);
            case COMMAND_DENY -> this.onReceiveAccept(in);  // todo
        }
    }

    public void onReceiveAsk(@NotNull ByteArrayDataInput in) {
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
                    text("[接受]", GREEN).clickEvent(ClickEvent.runCommand("/stpaccept " + requester)),
                    space(),
                    text("[拒绝]", RED).clickEvent(ClickEvent.runCommand("/stpadeny" + requester))
            ));
            case TPHERE -> {
                receiver.sendMessage(textOfChildren(
                        text(requester, WHITE),
                        text(" 请求你传送过去 ", GRAY),
                        space(),
                        text("[接受]", GREEN).clickEvent(ClickEvent.runCommand("/stpaccept " + requester)),
                        space(),
                        text("[拒绝]", RED).clickEvent(ClickEvent.runCommand("/stpadeny " + requester))
                ));
            }
        }
    }

    public @Nullable Location getTeleportLocation(@NotNull Player player) {
        return Optional.ofNullable(this.teleportLocations.remove(player.getName())).map(Pair::getKey).orElse(null);
    }

    public void onReceiveAccept(@NotNull ByteArrayDataInput in) {
        var type = TeleportType.valueOf(in.readUTF());
        var receiver = in.readUTF();
        var requester = in.readUTF();

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

        if (from != null && to != null) {
            // 同服传送
            from.teleportAsync(to.getLocation());
        } else if (to != null) {
            this.teleportLocations.put(switch (type) {
                case TP -> requester;
                case TPHERE -> receiver;
            }, Pair.of(to.getLocation(), new MutableInt(1200)));

            var out = ByteStreams.newDataOutput();
            out.writeUTF("ConnectOther");
            out.writeUTF(receiver);
            out.writeUTF(serverList.getCurrent());
            to.sendPluginMessage(Main.getInstance(), "BungeeCord", out.toByteArray());
        }

    }


}
