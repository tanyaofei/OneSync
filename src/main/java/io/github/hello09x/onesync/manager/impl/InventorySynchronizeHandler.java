package io.github.hello09x.onesync.manager.impl;

import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.manager.SynchronizeHandler;
import io.github.hello09x.onesync.manager.SynchronizedData;
import io.github.hello09x.onesync.repository.InventoryRepository;
import io.github.hello09x.onesync.repository.model.Inventory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class InventorySynchronizeHandler implements SynchronizeHandler<SynchronizedData.Inventory> {

    public final static InventorySynchronizeHandler instance = new InventorySynchronizeHandler();
    private final static NamespacedKey UNSUPPORTED_ITEM = new NamespacedKey(Main.getInstance(), "unsupported-item");
    private final static NamespacedKey UNSUPPORTED_NOISE = new NamespacedKey(Main.getInstance(), "noise");
    private final InventoryRepository repository = InventoryRepository.instance;

    @Override
    public @Nullable SynchronizedData.Inventory load(@NotNull UUID uuid) {
        var inv = repository.selectById(uuid);
        if (inv == null) {
            return null;
        }

        return new SynchronizedData.Inventory(
                this.deserialize(inv.items()),
                this.deserialize(inv.enderItems()),
                this.deserialize(inv.legacy())
        );
    }

    @Override
    public boolean save(@NotNull Player player, boolean clean) {
        try {
            return repository.insertOrUpdate(new Inventory(
                    player.getUniqueId(),
                    player.getName(),
                    this.serialize(player.getInventory()),
                    this.serialize(player.getEnderChest()),
                    Optional.ofNullable(repository.selectLegacy(player.getUniqueId())).orElse(Collections.emptyList()),
                    null,
                    null
            )) > 0;
        } finally {
            if (clean) {
                player.getInventory().clear();
                player.getEnderChest().clear();
            }
        }
    }

    @Override
    public void apply(@NotNull Player player, @NotNull SynchronizedData.Inventory inventory) {
        this.restore(player, player.getInventory(), inventory.items());
        this.restore(player, player.getEnderChest(), inventory.enderItems());
        this.restoreLegacy(player, inventory.legacy());
    }

    /**
     * 恢复遗产
     *
     * @param player 玩家
     * @param items  遗产
     */
    private void restoreLegacy(@NotNull Player player, @NotNull List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }

        var unfit = player.getInventory().addItem(items.toArray(ItemStack[]::new));
        if (!unfit.isEmpty()) {
            repository.updateLegacy(player.getUniqueId(), this.serialize(unfit.values()));
            player.sendMessage(text(
                    "你有 %d 样物品由于空间不足没有同步, 在你腾出背包空间后重新登陆即可恢复".formatted(unfit.size()),
                    GRAY)
            );
        }
    }

    /**
     * 恢复物品到指定箱子
     *
     * @param player    玩家
     * @param inventory 箱子
     * @param items     物品
     */
    private void restore(@NotNull Player player, @NotNull org.bukkit.inventory.Inventory inventory, @NotNull Map<Integer, ItemStack> items) {
        var legacy = inventory.getContents();
        inventory.clear();
        try {
            for (var entry : items.entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
        } finally {
            var mergedLegacy = Arrays
                    .stream(legacy)
                    .filter(i -> i != null && i.getType().isAir() && i.getAmount() > 0)
                    .map(ItemStack::serializeAsBytes)
                    .collect(Collectors.toList());

            if (!mergedLegacy.isEmpty()) {
                mergedLegacy.addAll(Optional.ofNullable(repository.selectLegacy(player.getUniqueId())).orElse(Collections.emptyList()));
                repository.updateLegacy(player.getUniqueId(), mergedLegacy);
            }
        }
    }

    /**
     * 尝试解析 "不支持的物品", 如果该物品不是 "不支持的物品" 或当前仍不被支持则返回该物品本身
     *
     * @param item 物品
     * @return 物品
     */
    private @NotNull ItemStack tryDecodeUnsupportedItem(@NotNull ItemStack item) {
        var data = item.getItemMeta().getPersistentDataContainer().get(UNSUPPORTED_ITEM, PersistentDataType.BYTE_ARRAY);
        if (data == null) {
            return item;
        }
        try {
            return ItemStack.deserializeBytes(data);
        } catch (Throwable e) {
            return item;
        }
    }

    /**
     * 创建一个当前服务器版本不支持的物品
     *
     * @param data 物品数据
     * @return 物品
     */
    private @NotNull ItemStack createUnsupportedItem(byte @NotNull [] data) {
        var item = new ItemStack(Material.AMETHYST_SHARD);
        item.editMeta(meta -> {
            meta.displayName(text("未知物品"));
            meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS);
            meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES);
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            meta.lore(List.of(
                    text("当前服务器不支持该物品, 带回原来的服务器能够恢复原状")
            ));

            var pdc = meta.getPersistentDataContainer();
            pdc.set(UNSUPPORTED_ITEM, PersistentDataType.BYTE_ARRAY, data);
            pdc.set(UNSUPPORTED_NOISE, PersistentDataType.STRING, UUID.randomUUID().toString());  // 防止堆叠
        });
        return item;
    }

    /**
     * 序列化
     *
     * @param inventory 箱子
     * @return 序列化后的数据
     */
    private @NotNull Map<Integer, byte[]> serialize(@NotNull org.bukkit.inventory.Inventory inventory) {
        var contents = inventory.getContents();
        var items = new HashMap<Integer, byte[]>(contents.length, 1.0F);
        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null) {
                continue;
            }
            items.put(i, item.serializeAsBytes());
        }
        return items;
    }

    private @NotNull @Unmodifiable List<byte[]> serialize(@NotNull Collection<ItemStack> items) {
        return items.stream().map(ItemStack::serializeAsBytes).toList();
    }

    /**
     * 反序列化
     *
     * @param items 物品
     * @return 反序列化后的物品
     */
    private @NotNull Map<Integer, ItemStack> deserialize(@NotNull Map<Integer, byte[]> items) {
        Map<Integer, ItemStack> ret = new HashMap<>(items.size(), 1.0F);
        for (var entry : items.entrySet()) {
            ItemStack item;
            try {
                item = ItemStack.deserializeBytes(entry.getValue());
            } catch (Throwable e) {
                item = this.createUnsupportedItem(entry.getValue());
            }

            item = this.tryDecodeUnsupportedItem(item);
            ret.put(entry.getKey(), item);
        }
        return ret;
    }

    /**
     * 反序列化
     *
     * @param items 物品
     * @return 反序列化后的物品
     */
    private @NotNull List<ItemStack> deserialize(@NotNull List<byte[]> items) {
        var ret = new ArrayList<ItemStack>(items.size());
        for (var bytes : items) {
            ItemStack item;
            try {
                item = ItemStack.deserializeBytes(bytes);
            } catch (Throwable e) {
                item = this.createUnsupportedItem(bytes);
            }

            item = this.tryDecodeUnsupportedItem(item);
            ret.add(item);
        }

        return ret;
    }

}
