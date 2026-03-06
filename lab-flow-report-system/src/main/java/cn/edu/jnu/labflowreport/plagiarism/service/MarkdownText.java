package cn.edu.jnu.labflowreport.plagiarism.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class MarkdownText {

    private static final Pattern FENCE = Pattern.compile("(?s)```.*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern MD_SYMBOLS = Pattern.compile("(?m)^\\s{0,3}(#+\\s+|>\\s+|[-*+]\\s+|\\d+\\.\\s+)");

    private MarkdownText() {
    }

    static String toPlainText(String md) {
        if (md == null) return "";
        String s = md;
        // Keep code content but remove the ``` fences.
        s = s.replace("```", "");
        s = INLINE_CODE.matcher(s).replaceAll("$1");
        s = LINK.matcher(s).replaceAll("$1");
        s = MD_SYMBOLS.matcher(s).replaceAll("");
        // Remove leftover markdown emphasis markers.
        s = s.replace("*", " ").replace("_", " ");
        s = s.replaceAll("[\\r\\t]+", " ");
        return s;
    }

    static List<String> splitSentences(String plain) {
        if (plain == null || plain.isBlank()) return List.of();
        String[] parts = plain.split("[。！？!?;；\\n]+");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.length() < 10) continue;
            out.add(t);
        }
        return out;
    }
}

