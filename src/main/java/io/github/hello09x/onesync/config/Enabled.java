package io.github.hello09x.onesync.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public enum Enabled {

    TRUE("true"),

    FALSE("false"),

    ISOLATED("isolated");

    final String value;

    public @NotNull String value() {
        return this.value;
    }

    public static @NotNull Enabled ofValue(@NotNull String value) {
        for (var v : values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

}
