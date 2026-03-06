package cn.edu.jnu.labflowreport.common.util;

import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256Hex(byte[] bytes) {
        if (bytes == null) {
            bytes = new byte[0];
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes);
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "SHA-256 不可用");
        }
    }

    public static String sha256Hex(InputStream in) {
        if (in == null) {
            return sha256Hex((byte[]) null);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "SHA-256 不可用");
        } catch (IOException e) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "文件读取失败");
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }
}

