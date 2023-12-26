package io.github.hello09x.onesync.api.event;

import io.github.hello09x.onesync.manager.entity.PreparedSnapshotComponent;
import io.github.hello09x.onesync.repository.model.Snapshot;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 当玩家异步准备好需要同步的数据时发起此事件, 此时玩家还没加入(join)游戏, 如果时管理员通过命令等方式强制恢复数据并不会发起此事件
 * <p><b>这个事件有可能是异步也可能是同步, 通常是当加入游戏的玩家是 假人(fakeplayer 插件) 或者别的插件提供的非真实玩家, 跳过了登陆过程才会进行同步调用</b></p>
 */
public class PlayerPrepareRestoreEvent extends Event {

    public final static HandlerList HANDLERS = new HandlerList();

    /**
     * @see Player#getUniqueId()
     */
    @NotNull
    @Getter
    private final UUID uniqueId;

    /**
     * @see Player#getName()
     */
    @Nullable
    @Getter
    private final String name;

    /**
     * 快照
     */
    @NotNull
    @Getter
    private final Snapshot snapshot;

    /**
     * 快照数据
     */
    @NotNull
    @Getter
    private final List<PreparedSnapshotComponent> components;

    public PlayerPrepareRestoreEvent(boolean isAsync, @NotNull UUID uniqueId, @Nullable String name, @NotNull Snapshot snapshot, @NotNull List<PreparedSnapshotComponent> components) {
        super(isAsync);
        this.uniqueId = uniqueId;
        this.name = name;
        this.snapshot = snapshot;
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
