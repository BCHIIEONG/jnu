package cn.edu.jnu.labflowreport.plagiarism.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SimHash64 {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_]{2,}");

    private SimHash64() {
    }

    static long fingerprint(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }

        Map<String, Integer> freq = new HashMap<>();
        Matcher m = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String t = m.group();
            freq.merge(t, 1, Integer::sum);
        }

        int[] v = new int[64];
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            long h = fnv1a64(e.getKey());
            int w = Math.max(1, e.getValue());
            for (int i = 0; i < 64; i++) {
                long bit = (h >>> i) & 1L;
                v[i] += (bit == 1L) ? w : -w;
            }
        }

        long out = 0L;
        for (int i = 0; i < 64; i++) {
            if (v[i] > 0) {
                out |= (1L << i);
            }
        }
        return out;
    }

    static double similarity(long a, long b) {
        int dist = Long.bitCount(a ^ b);
        return 1.0 - (dist / 64.0);
    }

    static long fnv1a64(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte b : bytes) {
            hash ^= (b & 0xffL);
            hash *= 0x100000001b3L;
        }
        return mix64(hash);
    }

    private static long mix64(long z) {
        z ^= (z >>> 33);
        z *= 0xff51afd7ed558ccdL;
        z ^= (z >>> 33);
        z *= 0xc4ceb9fe1a85ec53L;
        z ^= (z >>> 33);
        return z;
    }
}

