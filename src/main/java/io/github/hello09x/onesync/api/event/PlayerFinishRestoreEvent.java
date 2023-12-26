package io.github.hello09x.onesync.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 当玩家恢复数据结束时发布此事件
 */
public class PlayerFinishRestoreEvent extends PlayerEvent {

    public final static HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Result result;

    public PlayerFinishRestoreEvent(@NotNull Player who, @NotNull Result result) {
        super(who);
        this.result = result;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }


    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    public boolean isSuccess() {
        return result == Result.SUCCESS;
    }

    public enum Result {

        SUCCESS,

        FAILED

    }


}
