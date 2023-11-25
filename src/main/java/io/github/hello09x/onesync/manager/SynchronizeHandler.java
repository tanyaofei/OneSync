package io.github.hello09x.onesync.manager;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface SynchronizeHandler<T> {

    /**
     * 从数据库里读取数据
     *
     * @param uuid 玩家 UUID
     * @return 数据
     */
    @Nullable T load(@NotNull UUID uuid);

    /**
     * 将数据写入数据库
     *
     * @param player 玩家
     * @param clean  写入数据库后是否删除服务器数据
     */
    boolean save(@NotNull Player player, boolean clean);

    /**
     * 将准备好的数据恢复到玩家
     *
     * @param player 玩家
     * @param data   数据
     */
    void apply(@NotNull Player player, @NotNull T data);

}
