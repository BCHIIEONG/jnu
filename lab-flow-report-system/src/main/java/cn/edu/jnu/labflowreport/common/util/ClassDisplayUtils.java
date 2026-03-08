package cn.edu.jnu.labflowreport.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class ClassDisplayUtils {

    private static final Pattern LEGACY_GRADE_PATTERN = Pattern.compile("^\\s*(\\d{4})级(.+?)\\s*$");

    private ClassDisplayUtils() {
    }

    public static String buildDisplayName(Integer grade, String name) {
        String trimmed = name == null ? null : name.trim();
        if (!StringUtils.hasText(trimmed)) {
            return "";
        }
        if (grade != null) {
            return grade + "级" + trimmed;
        }
        return trimmed;
    }

    public static Integer effectiveGrade(Integer grade, String name) {
        if (grade != null) {
            return grade;
        }
        if (!StringUtils.hasText(name)) {
            return null;
        }
        Matcher matcher = LEGACY_GRADE_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    public static String effectiveDisplayName(Integer grade, String name) {
        if (grade != null) {
            return buildDisplayName(grade, name);
        }
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim();
    }
}
