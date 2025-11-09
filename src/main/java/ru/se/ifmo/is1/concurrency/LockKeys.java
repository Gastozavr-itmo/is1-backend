package ru.se.ifmo.is1.concurrency;

import ru.se.ifmo.is1.model.Country;

import java.util.Locale;

public final class LockKeys {
    private LockKeys() {
    }

    public static String join(Object... parts) {
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(parts[i] == null ? "null" : parts[i]);
        }
        return sb.toString();
    }

    public static String norm(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    public static String productKey(String partNumber, Long manufacturerId) {
        return join(manufacturerId, norm(partNumber));
    }

    public static String personKey(String name, Country nationality, String locationName) {
        return join(
                norm(name),
                nationality == null ? "null" : nationality.name(),
                norm(locationName)
        );
    }

    public static String productKey(String partNumber, Number manufacturerId) {
        Long mid = manufacturerId == null ? null : manufacturerId.longValue();
        return productKey(partNumber, mid);
    }
}
