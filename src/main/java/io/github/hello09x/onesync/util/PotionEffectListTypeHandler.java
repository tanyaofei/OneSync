package io.github.hello09x.onesync.util;

import com.google.gson.reflect.TypeToken;
import io.github.hello09x.bedrock.database.typehandler.TypeHandler;
import io.github.hello09x.onesync.Main;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class PotionEffectListTypeHandler implements TypeHandler<List<PotionEffect>> {

    public final static PotionEffectListTypeHandler instance = new PotionEffectListTypeHandler();

    private final static TypeToken<List<SerializedPotionEffect>> TYPE = new TypeToken<>() {
    };


    @Override
    public void setParameter(@NotNull PreparedStatement stm, int i, @NotNull List<PotionEffect> value) throws SQLException {
        var json = Main.getGson().toJson(value.stream().map(SerializedPotionEffect::new).toList());
        stm.setString(i, json);
    }

    @Override
    public @Nullable List<PotionEffect> getResult(@NotNull ResultSet rs, @NotNull String columnName) throws SQLException {
        var value = rs.getString(columnName);
        if (value == null) {
            return null;
        }

        var mapList = Main.getGson().fromJson(value, TYPE);
        return mapList
                .stream()
                .map(SerializedPotionEffect::toPotionEffect)
                .filter(Objects::nonNull)
                .toList();
    }

    public record SerializedPotionEffect(
            String type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {
        public SerializedPotionEffect(@NotNull PotionEffect effect) {
            this(effect.getType().getName(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon());
        }

        public @Nullable PotionEffect toPotionEffect() {
            var type = PotionEffectType.getByName(this.type);
            if (type == null) {
                // 遇到版本升级可能出现 null
                return null;
            }
            return new PotionEffect(
                    type,
                    this.duration,
                    this.amplifier,
                    this.ambient,
                    this.particles,
                    this.icon
            );
        }

    }


}
