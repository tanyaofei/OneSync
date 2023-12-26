package io.github.hello09x.onesync.api.event;

import io.github.hello09x.onesync.manager.entity.PreparedSnapshotComponent;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当玩家即将恢复数据时发布此事件
 */
public class PlayerAttemptRestoreEvent extends PlayerEvent {

    public final static HandlerList HANDLERS = new HandlerList();

    /**
     * 即将用来恢复的快照数据, 这是一个不可修改的列表
     */
    @Getter
    private final List<PreparedSnapshotComponent> components;

    public PlayerAttemptRestoreEvent(@NotNull Player who, @NotNull List<PreparedSnapshotComponent> components) {
        super(who);
        this.components = components;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

}
