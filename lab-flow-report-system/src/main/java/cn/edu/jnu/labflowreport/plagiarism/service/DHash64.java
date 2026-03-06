package cn.edu.jnu.labflowreport.plagiarism.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class DHash64 {

    private DHash64() {
    }

    static long fingerprint(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return 0L;
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
        if (src == null) {
            throw new IOException("unsupported image");
        }

        int sw = Math.max(1, src.getWidth());
        int sh = Math.max(1, src.getHeight());
        int[] g9x8 = new int[9 * 8];
        for (int y = 0; y < 8; y++) {
            int sy = (int) ((y / 8.0) * sh);
            if (sy >= sh) sy = sh - 1;
            for (int x = 0; x < 9; x++) {
                int sx = (int) ((x / 9.0) * sw);
                if (sx >= sw) sx = sw - 1;
                int rgb = src.getRGB(sx, sy);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                g9x8[y * 9 + x] = gray;
            }
        }

        long hash = 0L;
        int bitIndex = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = g9x8[y * 9 + x];
                int right = g9x8[y * 9 + x + 1];
                if (left > right) {
                    hash |= (1L << bitIndex);
                }
                bitIndex++;
            }
        }
        return hash;
    }

    static double similarity(long a, long b) {
        int dist = Long.bitCount(a ^ b);
        return 1.0 - (dist / 64.0);
    }
}
