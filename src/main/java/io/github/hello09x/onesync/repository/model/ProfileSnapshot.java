package io.github.hello09x.onesync.repository.model;

import com.google.common.base.Throwables;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ComponentUtils;
import io.github.hello09x.devtools.database.jdbc.RowMapper;
import io.github.hello09x.onesync.OneSync;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.manager.handler.ProfileSnapshotHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * @param snapshotId 快照 ID
 * @param playerId   玩家 ID
 * @param gameMode   游戏模式
 * @param op         是否 OP
 * @param level      经验等级
 * @param exp        当前等级经验值
 * @param health     生命值
 * @param maxHealth  最大生命值
 * @param foodLevel  饥饿值
 * @param saturation 饱食度
 * @param exhaustion 饥饿度
 */
public record ProfileSnapshot(

        Long snapshotId,

        UUID playerId,

        @Nullable
        GameMode gameMode,

        @Nullable
        Boolean op,

        @Nullable
        Integer level,

        @Nullable
        Float exp,

        @Nullable
        Double health,

        @Nullable
        Double maxHealth,

        @Nullable
        Integer foodLevel,

        @Nullable
        Float saturation,

        @Nullable
        Float exhaustion,

        @Nullable
        Integer remainingAir

) implements SnapshotComponent {

    private final static Logger log = OneSync.getInstance().getLogger();

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(meta -> {
            meta.displayName(ComponentUtils.noItalic(text("档案", YELLOW)));
            meta.lore(Stream.of(
                    textOfChildren(text("游戏模式: ", GRAY), Optional.ofNullable(this.gameMode).map(v -> (Component) translatable(v, WHITE)).orElse(text("<无>", WHITE))),
                    textOfChildren(text("OP: ", GRAY), text(Optional.ofNullable(this.op).map(v -> v ? "是" : "否").orElse("<无>"), WHITE)),
                    textOfChildren(text("等级: ", GRAY), text(Optional.ofNullable(this.level).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("经验条: ", GRAY), text(Optional.ofNullable(this.exp).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("生命值: ", GRAY), text(Optional.ofNullable(this.health).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("最大生命值: ", GRAY), text(Optional.ofNullable(this.maxHealth).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("饥饿值: ", GRAY), text(Optional.ofNullable(this.foodLevel).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("饱食度: ", GRAY), text(Optional.ofNullable(this.saturation).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("饥饿度: ", GRAY), text(Optional.ofNullable(this.exhaustion).map(Object::toString).orElse("<无>"), WHITE)),
                    textOfChildren(text("氧气值: ", GRAY), text(Optional.ofNullable(this.remainingAir).map(air -> air / 20 + " 秒").orElse("<无>"), WHITE)),
                    empty()
            ).map(ComponentUtils::noItalic).toList());
        });
        return new MenuItem(item, event -> {
            if (event.getClick() == ClickType.RIGHT) {
                var player = Bukkit.getPlayer(this.playerId);
                if (player == null) {
                    viewer.sendMessage(text("该玩家不在线", RED));
                    viewer.closeInventory();
                    return;
                }

                try {
                    OneSync.getInjector().getInstance(ProfileSnapshotHandler.class).apply(player, this);
                    viewer.sendMessage(textOfChildren(text("为 ", GRAY), text(player.getName(), WHITE), text(" 恢复数据成功", GRAY)));
                } catch (Throwable e) {
                    log.severe(Throwables.getStackTraceAsString(e));
                    viewer.sendMessage(text("恢复数据失败", RED));
                }

                viewer.closeInventory();
            }
        });
    }

    @Singleton
    public static class ProfileSnapshotRowMapper implements RowMapper<ProfileSnapshot> {

        @Override
        public @Nullable ProfileSnapshot mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            return new ProfileSnapshot(
                    rs.getObject("snapshot_id", Long.class),
                    UUID.fromString(rs.getString("player_id")),
                    Optional.ofNullable(rs.getString("game_mode")).map(GameMode::valueOf).orElse(null),
                    rs.getObject("op", Boolean.class),
                    rs.getObject("level", Integer.class),
                    rs.getObject("exp", Float.class),
                    rs.getObject("health", Double.class),
                    rs.getObject("max_health", Double.class),
                    rs.getObject("food_level", Integer.class),
                    rs.getObject("saturation", Float.class),
                    rs.getObject("exhaustion", Float.class),
                    rs.getObject("remaining_air", Integer.class)
            );
        }
    }

}
