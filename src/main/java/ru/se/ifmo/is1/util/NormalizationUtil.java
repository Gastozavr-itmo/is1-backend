package ru.se.ifmo.is1.util;

import java.text.Normalizer;

public final class NormalizationUtil {
    private NormalizationUtil() {}

    public static String canonicalKey(String s, boolean lower) {
        if (s == null) return null;
        String t = s.strip()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");
        t = Normalizer.normalize(t, Normalizer.Form.NFC);
        return lower ? t.toLowerCase() : t;
    }

    public static String canonicalPartNumber(String pn) {
        if (pn == null) return null;
        String t = canonicalKey(pn, false);
        t = t.replace('–','-').replace('—','-');
        t = t.replaceAll("\\s*[-_]\\s*", "-");
        return t.toUpperCase();
    }
}
