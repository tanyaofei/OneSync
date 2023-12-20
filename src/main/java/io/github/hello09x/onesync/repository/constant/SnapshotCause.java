package io.github.hello09x.onesync.repository.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SnapshotCause {

    /**
     * 玩家退出
     */
    PLAYER_QUIT(text("👻退出游戏", GRAY), Material.MUSIC_DISC_STAL),

    /**
     * 关闭插件
     */
    PLUGIN_DISABLE(text("🚫插件关闭", RED), Material.MUSIC_DISC_CHIRP),

    /**
     * 世界保存
     */
    WORLD_SAVE(text("🌍保存地图", AQUA), Material.MUSIC_DISC_WAIT),

    /**
     * 玩家死亡
     */
    PLAYER_DEATH(text("💀玩家死亡", WHITE), Material.MUSIC_DISC_STRAD)

    ;

    final Component displayName;

    final Material icon;

}
