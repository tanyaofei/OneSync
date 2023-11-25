package io.github.hello09x.onesync.manager;

import com.google.common.base.Throwables;
import io.github.hello09x.onesync.Main;
import io.github.hello09x.onesync.config.OnesyncConfig;
import io.github.hello09x.onesync.manager.impl.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SynchronizeManager {

    public final static SynchronizeManager instance = new SynchronizeManager();

    private final static Logger log = Main.getInstance().getLogger();

    private final InventorySynchronizeHandler inventories = InventorySynchronizeHandler.instance;
    private final PDCSynchronizeHandler pdcs = PDCSynchronizeHandler.instance;
    private final HealthSynchronizeHandler healths = HealthSynchronizeHandler.instance;
    private final FoodSynchronizeHandler foods = FoodSynchronizeHandler.instance;
    private final ExperienceSynchronizeHandler experiences = ExperienceSynchronizeHandler.instance;
    private final OnesyncConfig config = OnesyncConfig.instance;

    private final Map<@NotNull UUID, @NotNull SynchronizedData> prepared = new ConcurrentHashMap<>();

    private static boolean catchException(@NotNull String action, @NotNull Runnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Throwable e) {
            log.severe(action + "异常\n" + Throwables.getStackTraceAsString(e));
            return false;
        }
    }

    public void prepare(@NotNull UUID uuid) {
        this.prepared.put(uuid, new SynchronizedData(
                config.getSync().isInventory() ? inventories.load(uuid) : null,
                config.getSync().isPdc() ? pdcs.load(uuid) : null,
                config.getSync().isHealth() ? healths.load(uuid) : null,
                config.getSync().isFood() ? foods.load(uuid) : null,
                config.getSync().isExp() ? experiences.load(uuid) : null
        ));
    }

    public void removePrepared(@NotNull UUID uuid) {
        this.prepared.remove(uuid);
    }

    public boolean save(@NotNull Player player, boolean clean) {
        var success = true;
        if (config.getSync().isInventory()) {
            success &= catchException("保存物品", () -> this.inventories.save(player, clean));
        }
        if (config.getSync().isPdc()) {
            success &= catchException("保存 PDC", () -> this.pdcs.save(player, clean));
        }
        if (config.getSync().isHealth()) {
            success &= catchException("保存生命值", () -> this.healths.save(player, clean));
        }
        if (config.getSync().isFood()) {
            success &= catchException("保存饥饿值", () -> this.foods.save(player, clean));
        }
        if (config.getSync().isExp()) {
            success &= catchException("保存经验值", () -> this.experiences.save(player, clean));
        }
        return success;
    }

    public boolean restore(@NotNull Player player) {
        var data = this.prepared.get(player.getUniqueId());
        if (data == null) {
            return false;
        }

        this.prepared.remove(player.getUniqueId());

        {
            var inv = data.inventory();
            if (inv != null) {
                inventories.apply(player, data.inventory());
            }
        }

        {
            var persistentData = data.pdc();
            if (persistentData != null) {
                pdcs.apply(player, persistentData);
            }
        }

        {
            var health = data.health();
            if (health != null) {
                healths.apply(player, health);
            }
        }

        {
            var experience = data.experience();
            if (experience != null) {
                experiences.apply(player, experience);
            }
        }

        return true;
    }


}
