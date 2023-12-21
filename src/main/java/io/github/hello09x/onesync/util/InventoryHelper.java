package io.github.hello09x.onesync.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class InventoryHelper {

    public static @NotNull Map<Integer, ItemStack> toMap(@NotNull Inventory inventory) {
        var items = new HashMap<Integer, ItemStack>(inventory.getSize(), 1.0F);
        var itr = inventory.iterator();
        while (itr.hasNext()) {
            var i = itr.nextIndex();
            var item = itr.next();
            if (item == null) {
                continue;
            }
            items.put(i, item);
        }
        return items;
    }

    public static void replace(@NotNull Inventory replaceTo, @NotNull Map<Integer, ItemStack> replacement) {
        for (int i = replaceTo.getSize() - 1; i >= 0; i--) {
            replaceTo.setItem(i, replacement.get(i));
        }
    }

}
