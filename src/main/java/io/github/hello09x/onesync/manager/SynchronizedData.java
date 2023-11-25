package io.github.hello09x.onesync.manager;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 同步数据
 *
 * @param inventory  背包以及末影箱数据
 * @param pdc        玩家持久化数据
 * @param health     生命值
 * @param experience 经验值
 * @param food       饥饿值
 */
public record SynchronizedData(

        @Nullable
        Inventory inventory,

        byte @Nullable [] pdc,

        @Nullable
        Health health,

        @Nullable
        Food food,

        @Nullable
        Experience experience

) {

        /**
         * @param items      背包物品
         * @param enderItems 末影箱物品
         * @param legacy     遗产
         */
        public record Inventory(

                @NotNull
                Map<Integer, ItemStack> items,

                @NotNull
                Map<Integer, ItemStack> enderItems,

                @NotNull
                List<ItemStack> legacy

        ) {
        }


        /**
         * @param health    当前生命值
         * @param maxHealth 最大生命值
         */
        public record Health(

                double health,

                double maxHealth

        ) {
        }

        /**
         * @param level      饥饿值
         * @param saturation 饱食度
         */
        public record Food(
                int level,
                float saturation
        ) {
        }


        /**
         * @param level 当前等级
         * @param exp   当前经验值百分比
         */
        public record Experience(
                int level,

                float exp
        ) {
        }

}
