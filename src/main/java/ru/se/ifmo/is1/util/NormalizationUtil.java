package ru.se.ifmo.is1.util;

import java.text.Normalizer;

public final class NormalizationUtil {
    private NormalizationUtil() {}

    public static String canonicalKey(String s, boolean lower) {
        if (s == null) return null;
        String t = s.strip()
                .replace('\u00A0', ' ')       // NBSP -> space
                .replaceAll("\\s+", " ");     // collapse spaces
        t = Normalizer.normalize(t, Normalizer.Form.NFC);
        return lower ? t.toLowerCase() : t;
    }

    public static String canonicalPartNumber(String pn) {
        if (pn == null) return null;
        String t = canonicalKey(pn, false);
        t = t.replace('–','-').replace('—','-');     // en/em dash -> '-'
        t = t.replaceAll("\\s*[-_]\\s*", "-");       // вокруг тире/подчёркив. убрать пробелы
        return t.toUpperCase();
    }

    public static String compositeKey(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i] == null ? "" : parts[i].replace("|","||");
            if (i > 0) sb.append('|');
            sb.append(p);
        }
        return sb.toString();
    }
}
