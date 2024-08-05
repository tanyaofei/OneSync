package io.github.hello09x.onesync.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class PotionEffectListTypeCodec {

    private final static TypeToken<List<SerializedPotionEffect>> TYPE = new TypeToken<>() {
    };

    private final Gson gson;

    @Inject
    public PotionEffectListTypeCodec(Gson gson) {
        this.gson = gson;
    }

    public @NotNull String serialize(@NotNull List<PotionEffect> effects) {
        var values = effects.stream().map(SerializedPotionEffect::new).toList();
        return this.gson.toJson(values);
    }

    public @NotNull List<PotionEffect> deserialize(@NotNull String json) {
        return this.gson.fromJson(json, TYPE).stream().map(SerializedPotionEffect::toPotionEffect).collect(Collectors.toList());
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
