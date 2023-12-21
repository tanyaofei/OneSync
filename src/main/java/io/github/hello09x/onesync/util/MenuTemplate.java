package io.github.hello09x.onesync.util;

import io.github.hello09x.onesync.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

import static io.github.hello09x.bedrock.util.Components.noItalic;

public class MenuTemplate {

    public static void openInventoryMenu(@NotNull Player viewer, @NotNull Component title, @NotNull Map<Integer, ItemStack> items, @NotNull Consumer<InventoryClickEvent> back) {
        var menu = Main.getMenuRegistry().createMenu(54, title);
        for (var entry : items.entrySet()) {
            Main.getMenuRegistry().setButton(menu, entry.getKey(), entry.getValue(), e -> {
                var item = e.getCurrentItem();
                if (item == null) {
                    return;
                }

                viewer.getInventory().addItem(item);
            });
        }

        Main.getMenuRegistry().setButton(menu, 49, Material.BARRIER, noItalic("取消"), back);

        viewer.openInventory(menu);
    }


}
