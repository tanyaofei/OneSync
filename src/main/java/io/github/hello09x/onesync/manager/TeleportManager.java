package io.github.hello09x.onesync.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.hello09x.bedrock.util.MCUtils;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.constant.Channels;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class TeleportManager implements PluginMessageListener {

    private final static String COMMAND_TP = "tp";
    private final static String COMMAND_TPHERE = "tphere";
    private final static String COMMAND_TPACCEPT = "tpaccept";

    public final static String CHANNEL = Channels.Teleport;

    private final PlayerManager playerList = PlayerManager.instance;
    private final ServerManager serverList = ServerManager.instance;
    public final static TeleportManager instance = new TeleportManager();
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

    public @NotNull Component tp(@NotNull Player requester, @NotNull String receiver) {
        if (repository.existsByRequesterAndReceiver(requester.getName(), receiver)) {
            return textOfChildren(text("你已经对 ", GRAY), text(receiver, WHITE), text(" 发起过传送请求了", GRAY));
        }

        var teleport = new Teleport(
                requester.getName(),
                receiver,
                TeleportType.TP,
                null
        );
        repository.insert(teleport);

        var out = ByteStreams.newDataOutput();
        out.writeUTF(COMMAND_TP);
        out.writeUTF(requester.getName());
        out.writeUTF(receiver);
        var message = out.toByteArray();
        requester.sendPluginMessage(Main.getInstance(), CHANNEL, message);
        this.onPluginMessageReceived(CHANNEL, requester, message);

        return text("传送请求已发送, 等待对方接受", GRAY);
    }

    public @NotNull Component tphere(@NotNull Player requester, @NotNull String receiver) {
        if (repository.existsByRequesterAndReceiver(requester.getName(), receiver)) {
            return textOfChildren(text("你已经对 ", GRAY), text(receiver, WHITE), text(" 发起过传送请求了", GRAY));
        }

        var out = ByteStreams.newDataOutput();
        out.writeUTF(COMMAND_TPHERE);
        out.writeUTF(requester.getName());
        out.writeUTF(receiver);
        var message = out.toByteArray();
        requester.sendPluginMessage(Main.getInstance(), CHANNEL, message);
        this.onPluginMessageReceived(CHANNEL, requester, message);

        return text("传送请求已发送, 等待对方接受", GRAY);
    }

    public @Nullable Component tpaccept(@NotNull Player receiver, @Nullable String requester) {
        var teleport = requester == null
                ? repository.selectLatestByReceiver(receiver.getName())
                : repository.selectLatestByRequesterAndReceiver(requester, receiver.getName());

        if (teleport == null) {
            return text("你没有接收到任何传送请求", GRAY);
        }

        var out = ByteStreams.newDataOutput();
        out.writeUTF(COMMAND_TPACCEPT);
        out.writeUTF(receiver.getName());
        out.writeUTF(teleport.receiver());
        var message = out.toByteArray();

        receiver.sendPluginMessage(Main.getInstance(), CHANNEL, message);
        this.onPluginMessageReceived(CHANNEL, receiver, message);
        return null;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(Channels.Teleport)) {
            return;
        }

        var in = ByteStreams.newDataInput(message);
        switch (in.readUTF()) {
            case COMMAND_TP -> this.onReceiveTp(in);
            case COMMAND_TPHERE -> this.onReceiveTphere(in);
            case COMMAND_TPACCEPT -> this.onReceiveTpaccept(in);
        }
    }

    public void onReceiveTp(@NotNull ByteArrayDataInput in) {
        var requester = in.readUTF();
        var receiver = Bukkit.getPlayerExact(in.readUTF());
        if (receiver == null) {
            return;
        }

        receiver.sendMessage(textOfChildren(
                text(requester, WHITE),
                text(" 请求传送到你身边 ", GRAY),
                text("[接受]", GREEN).clickEvent(ClickEvent.runCommand("/stpaccept " + requester)),
                space(),
                text("[拒绝]", RED).clickEvent(ClickEvent.runCommand("/stpadeny" + requester))
        ));
    }

    public void onReceiveTphere(@NotNull ByteArrayDataInput in) {
        var requester = in.readUTF();
        var receiver = Bukkit.getPlayerExact(in.readUTF());
        if (receiver == null) {
            return;
        }

        receiver.sendMessage(textOfChildren(
                text(requester, WHITE),
                text(" 请求你传送过去 ", GRAY),
                space(),
                text("[接受]", GREEN).clickEvent(ClickEvent.runCommand("/stpaccept " + requester)),
                space(),
                text("[拒绝]", RED).clickEvent(ClickEvent.runCommand("/stpadeny " + requester))
        ));
    }

    public @Nullable Location getTeleportLocation(@NotNull Player player) {
        return Optional.ofNullable(this.teleportLocations.remove(player.getName())).map(Pair::getKey).orElse(null);
    }

    public void onReceiveTpaccept(@NotNull ByteArrayDataInput in) {
        var receiver = in.readUTF();
        var requester = Bukkit.getPlayerExact(in.readUTF());
        if (requester == null) {
            return;
        }

        var r = Bukkit.getPlayer(receiver);
        if (r != null) {
            // 同服直接传送
            r.teleportAsync(requester.getLocation());
        } else {
            this.teleportLocations.put(receiver, Pair.of(requester.getLocation(), new MutableInt(1200)));
            var out = ByteStreams.newDataOutput();
            out.writeUTF("ConnectOther");
            out.writeUTF(receiver);
            out.writeUTF(serverList.getCurrent());
        }
    }

}
