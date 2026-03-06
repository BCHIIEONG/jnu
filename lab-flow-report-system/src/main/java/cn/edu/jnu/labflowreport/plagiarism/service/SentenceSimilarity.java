package cn.edu.jnu.labflowreport.plagiarism.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SentenceSimilarity {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_]{2,}");

    private SentenceSimilarity() {
    }

    static double jaccard(String a, String b) {
        Set<String> sa = toSet(a);
        Set<String> sb = toSet(b);
        if (sa.isEmpty() && sb.isEmpty()) return 1.0;
        if (sa.isEmpty() || sb.isEmpty()) return 0.0;
        int inter = 0;
        for (String t : sa) {
            if (sb.contains(t)) inter++;
        }
        int union = sa.size() + sb.size() - inter;
        return union == 0 ? 0.0 : (inter * 1.0 / union);
    }

    static Set<String> toSet(String s) {
        if (s == null || s.isBlank()) return Set.of();
        Matcher m = TOKEN.matcher(s.toLowerCase(Locale.ROOT));
        Set<String> out = new HashSet<>();
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    static List<Fragment> topFragments(String aPlain, String bPlain, int max, double threshold) {
        List<String> aSent = MarkdownText.splitSentences(aPlain);
        List<String> bSent = MarkdownText.splitSentences(bPlain);
        java.util.ArrayList<Fragment> frags = new java.util.ArrayList<>();
        for (String s : aSent) {
            double best = 0.0;
            for (String t : bSent) {
                double score = jaccard(s, t);
                if (score > best) best = score;
            }
            if (best >= threshold) {
                frags.add(new Fragment(s, best));
            }
        }
        frags.sort((x, y) -> Double.compare(y.score(), x.score()));
        if (frags.size() > max) {
            return frags.subList(0, max);
        }
        return frags;
    }

    record Fragment(String text, double score) {
    }
}

