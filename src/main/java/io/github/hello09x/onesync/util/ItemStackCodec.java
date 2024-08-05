package io.github.hello09x.onesync.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author tanyaofei
 * @since 2024/8/5
 **/
@Singleton
public class ItemStackCodec {

    private final static TypeToken<Map<Integer, byte[]>> TYPE = new TypeToken<>() {
    };

    private final Gson gson;

    @Inject
    public ItemStackCodec(Gson gson) {
        this.gson = gson;
    }

    public @NotNull String serialize(@NotNull Map<Integer, ItemStack> items) {
        var bytes = new HashMap<Integer, byte[]>(items.size());
        for (var entry : items.entrySet()) {
            var item = entry.getValue();
            bytes.put(entry.getKey(), item == null ? null : item.serializeAsBytes());
        }

        return gson.toJson(bytes);
    }

    public @NotNull Map<Integer, ItemStack> deserialize(@NotNull String json) {
        var bytes = Objects.requireNonNull(gson.fromJson(json, TYPE));

        var items = new HashMap<Integer, ItemStack>(bytes.size(), 1.0F);
        for (var entry : bytes.entrySet()) {
            items.put(entry.getKey(), ItemStack.deserializeBytes(entry.getValue()));    // 版本升级的兼容问题 Purpur 会自行处理
        }

        return items;
    }

}
