package io.github.hello09x.onesync.repository.model;

import com.google.common.base.Throwables;
import io.github.hello09x.bedrock.database.Table;
import io.github.hello09x.bedrock.database.TableField;
import io.github.hello09x.bedrock.database.TableId;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.api.handler.SnapshotComponent;
import io.github.hello09x.onesync.manager.synchronize.handler.ProfileSnapshotHandler;
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
@Table("profile_snapshot")
public record ProfileSnapshot(

        @TableId("snapshot_id")
        Long snapshotId,

        @TableField("player_id")
        UUID playerId,

        @Nullable
        @TableField("game_mode")
        GameMode gameMode,

        @Nullable
        @TableField("op")
        Boolean op,

        @Nullable
        @TableField("level")
        Integer level,

        @Nullable
        @TableField("exp")
        Float exp,

        @Nullable
        @TableField("health")
        Double health,

        @Nullable
        @TableField("max_health")
        Double maxHealth,

        @Nullable
        @TableField("food_level")
        Integer foodLevel,

        @Nullable
        @TableField("saturation")
        Float saturation,

        @Nullable
        @TableField("exhaustion")
        Float exhaustion,

        @Nullable
        @TableField("remaining_air")
        Integer remainingAir

) implements SnapshotComponent {

    private final static Logger log = Main.getInstance().getLogger();

    @Override
    public @NotNull OfflinePlayer owner() {
        return Bukkit.getOfflinePlayer(this.playerId);
    }

    @Override
    public @NotNull MenuItem toMenuItem(@NotNull Player viewer, @NotNull Consumer<InventoryClickEvent> prevMenu) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(meta -> {
            meta.displayName(Components.noItalic("档案", YELLOW));
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
            ).map(Components::noItalic).toList());
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
                    ProfileSnapshotHandler.instance.apply(player, this);
                    viewer.sendMessage(textOfChildren(text("为 ", GRAY), text(player.getName(), WHITE), text(" 恢复数据成功", GRAY)));
                } catch (Throwable e) {
                    log.severe(Throwables.getStackTraceAsString(e));
                    viewer.sendMessage(text("恢复数据失败", RED));
                }

                viewer.closeInventory();
            }
        });
    }

}
