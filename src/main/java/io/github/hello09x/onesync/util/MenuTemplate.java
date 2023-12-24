package io.github.hello09x.onesync.util;

import io.github.hello09x.onesync.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

import static io.github.hello09x.bedrock.util.Components.noItalic;
import static net.kyori.adventure.text.Component.text;

public class MenuTemplate {

    public static void openInventoryMenu(@NotNull Player viewer, @NotNull Component title, @NotNull Map<Integer, ItemStack> items, @NotNull Consumer<InventoryClickEvent> onCancel) {
        var menu = Main.getChestMenuRegistry().createMenu(54, title, onCancel);
        for (var entry : items.entrySet()) {
            menu.setButton(entry.getKey(), entry.getValue(), e -> {
                var item = e.getCurrentItem();
                if (item == null) {
                    return;
                }

                viewer.getInventory().addItem(item);
            });
        }

        viewer.openInventory(menu.getInventory());
    }


}
