package cn.edu.jnu.labflowreport.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReviewIssueTags {

    private static final LinkedHashMap<String, String> TAGS = new LinkedHashMap<>();

    static {
        TAGS.put("FORMAT", "格式不规范");
        TAGS.put("STEPS", "实验步骤不完整");
        TAGS.put("ANALYSIS", "结果分析不足");
        TAGS.put("CONCLUSION", "结论不清晰");
        TAGS.put("DATA", "数据异常");
        TAGS.put("CHART", "图表缺失或错误");
        TAGS.put("CODE", "代码/原理错误");
        TAGS.put("PLAGIARISM", "抄袭疑似");
        TAGS.put("OTHER", "其他");
    }

    private ReviewIssueTags() {
    }

    public static Set<String> codes() {
        return TAGS.keySet();
    }

    public static List<TagOption> options() {
        return TAGS.entrySet().stream()
                .map(item -> new TagOption(item.getKey(), item.getValue()))
                .toList();
    }

    public static String labelOf(String code) {
        return TAGS.getOrDefault(code, code);
    }

    public record TagOption(String code, String label) {
    }
}
