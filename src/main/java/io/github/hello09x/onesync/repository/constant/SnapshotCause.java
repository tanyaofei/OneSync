package io.github.hello09x.onesync.repository.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.Component.text;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SnapshotCause {

    /**
     * 玩家退出
     */
    PLAYER_QUIT(text("退出游戏")),

    /**
     * 关闭插件
     */
    PLUGIN_DISABLE(text("插件关闭")),

    /**
     * 世界保存
     */
    WORLD_SAVE(text("保存地图")),

    /**
     * 玩家死亡
     */
    PLAYER_DEATH(text("玩家死亡"))

    ;

    final Component displayName;

}
