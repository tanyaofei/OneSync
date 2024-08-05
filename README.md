# OneSync

———— Minecraft 跨服同步一切数据

## 使用前提

1. 依赖于 `1.20.2` 的数据包特性，仅支持 `1.20.2` 以上的 `Paper` 及其下游核心，支持 `Folia`
2. 前置插件: `CommandAPI`

## 命令
+ /onesync reload：重新加载配置文件
+ /onesync unlock <玩家>：异常关闭服务器时解锁玩家
+ /onesync unlock-all: 异常关闭服务器时解锁所有玩家
+ /onesync snapshot <玩家>：查看玩家快照备份
+ /onesync save <多名玩家>：主动保存玩家数据

## 功能

### 数据同步

+ 背包
+ 末影箱
+ 饥饿值、经验值、OP、游戏模式、氧气值
+ 成就
+ 药水效果
+ 经济 (Vault)

### 快照 (备份)

起义解释: OneSync 的快照不仅仅是备份，同时也是玩家存档。最后一份快照会在玩家登陆时使用。

+ 友好的菜单界面，点击任意空白地方返回上一页
+ 可配置「多少天内至少保留一份快照备份」
+ 可配置快照备份的触发节点，如：玩家死亡、服务器存档、玩家游戏模式切换
+ 背包和末影响快照可直接查看与取出物品
+ 支持快照整体恢复或指定某项恢复

### 跨服传送

不需要 `BungeeCord` 端的跨服传送你心动了吗？深度还原 CMI 的传送命令

| 命令         | 权限                       |
|------------|--------------------------|
| tpa        | onesync.teleportRequest.tpa     |
| tpahere    | onesync.teleportRequest.tpahere |
| tp         | OP                       |
| tphere     | OP                       |
| tphereall  | OP                       |
| tpahereall | OP                       |

### 差异化配置
你可以在不同的服务器上差异化地开启或关闭某些数据的配置。

例如你有 A、B、C 三个服务器，你可以在 A、B 服务器上将 `inventory` 设置为 `true`，表示同步背包数据，而在 C 服务器设置为 `isolated` 表示不使用同步数据。

_注意：你必须确保所有服务器都关闭某一项同步，才能将这一项设置为 `false`，如果部分开启部分不开启，不开启的服务器应当设置为 `isolated`_


### 优点
+ Less is more：不需要 Redis，仅仅需要 MySQL；不需要在 BungeeCord 端安装任何插件。
+ 延迟低：安全的异步加载数据，玩家退出重登只占用主线程 1～5ms, 切换服务器占用 5 ～ 30ms
+ 由工作 3 年，30 年 JAVA 开发经验程序员（加班加出来的）开发

### 使用须知
+ 插件不会自动在 MySQL 上建立对应的数据库，请手动建立，默认数据库名为 onesync
+ 插件不会处理多个服已有数据的合并，启用插件后，玩家第一次进入的服务器的存档将会成为他的跨服存档
+ 插件不会清理原版存档，如果你不再需要此插件，请自行清理玩家存档文件
+ 为了你的数据安全，服务器启动时插件加载失败，将会停止服务器
+ 如果玩家登录时出现 [OneSync]加载数据超时，先让玩家重试登录一下，如果还不行则通常是因为上一个服务器没有正确关闭所以没有解锁玩家数据，你可以通过 unlock 命令将他解锁

### 安装方法：
+ 确保你电脑安装了 MySQL
+ 将此插件安装到你需要跨服同步的多个服务器上
+ 初次安装时没有配置文件，你可以启动一次服务器自动生成或者手动创建。启动时可能会因为数据库地址或者账号密码错误而启动失败。
+ 编辑配置文件，将 datasource.url 配置为你的数据库地址，确保账号密码正确
+ 启动服务器


### 配置文件
```yaml
version: 4

datasource:
  driver-class: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://127.0.0.1:3306/onesync
  username: root
  password: 123456
  size: 2

# 是否打印调试日志
debug: true

# 服务器 ID
# 如果配置了这项, 那么在服务器重启的时候能自动解锁因为异常关闭而锁住的玩家
# 注意 1: 要么别配置, 如果配置就一定不能重复!!!
# 注意 2: 因为服务器异常关闭的锁定, 可能导致玩家的数据不是最新的!!!
server-id: ''

# 同步配置
# 当前服务器同步则设置为: true
# 所有服务器都不同步则设置为: false
# 当前服务器不同步, 别的服务器同步则设置为: isolated
# 提示: 设置为 isolated 的服务器会将别的服务器保存的快照信息继续传递下去, 才能避免数据混乱
synchronize:

  # 玩家背包
  inventory: true

  # 末影箱
  ender-chest: true

  # 玩家持久化数据 PersistentDataContainer(PDC)
  pdc: false

  # 玩家成就数据
  advancements: false

  # 效果, 如药水效果、Buff
  potion-effects: false

  # 经济, 对 Vault 插件的支持
  # 注意: 只是提供了支持了 Vault 的跨服, 你仍需 ESS、CMI 之类的经济插件
  vault: false

  # 玩家档案
  profile:

    # 游戏模式
    game-mode: false

    # 是否 OP
    op: false

    # 生命值, 最大生命值
    health: false

    # 经验
    exp: false

    # 饥饿值, 饱食度, 饥饿度
    food: false

    # 氧气值
    air: false


# 快照配置
snapshot:

  # 每位玩家最大快照数量
  capacity: 45

  # 多少天内必须至少保存一份最后的快照
  keep-days: 7

  # 何时保存玩家快照
  # 除了这里可以指定的节点以外, 玩家退出游戏, 服务器关闭也会保存快照
  # 可选值:
    # WORLD_SAVE              : (全体) 保存地图时触发, 约 5 分钟一次, 经测试 50 名玩家约 400 ms
    # PERIODICAL              : (部分) 约每 5 分钟保存大约 1/4 的玩家
    # PLAYER_DEATH            : (个人) 玩家死亡时触发, 适合死亡掉落的服务器
    # PLAYER_GAME_MODE_CHANGE : (个人) 玩家游戏模式切换
  when:
    - WORLD_SAVE
    - PLAYER_GAME_MODE_CHANGE


# 跨服传送配置
teleportRequest:

  # 是否启用
  # 请保持多个服务器的这个配置统一, 否则会发生奇怪的事情
  # 修改此项配置重启才生效
  enabled: false

  # 预热
  # 玩家开始传送前需要等待多少秒
  # 等待期间如果移动了则取消传送
  # 单位: 秒
  warmup: 3

  # 玩家发起传送请求的有效时间
  # 单位: 秒
  expires-in: 60

  # 命令别名
  # 默认配置下覆盖常规的 tp 命令, 如果你不想覆盖可以修改为 '' 或者与原命令保持一致
  # 修改此项配置重启才生效
  commands:
    stpa: tpa
    stpahere: tpahere
    stpaccept: tpaccept
    stpdeny: tpdeny
    stpcancel: tpcancel
    stp: tp
    stphere: tphere
    stphereall: tphereall
    stpahereall: tpahereall

  # 当玩家预热时是否产生粒子特效(像念咒语一样)
  particle: true

  # 当玩家传送完毕后是否发出音效(末影人传送音效)
  sound: true
```