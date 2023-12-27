package io.github.hello09x.onesync.repository.constant;

public enum TeleportStatus {

    /**
     * 刚发起请求
     */
    REQUESTED,

    /**
     * 已接受, 但玩家可能还处于切换服务器中, 还没传送完毕
     */
    ACCEPTED,

}
