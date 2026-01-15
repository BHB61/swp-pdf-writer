package de.hft_stuttgart.ip1;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Wrap {
    private Wrap() {}

    public static List<String> wrap(PDFont font, float size, String text, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        if (text == null) { out.add(""); return out; }

        String[] parts = text.split("\\n", -1);
        for (int p = 0; p < parts.length; p++) {
            wordWrap(font, size, parts[p], maxWidth, out);
            if (p < parts.length - 1) out.add("");
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    private static void wordWrap(PDFont font, float size, String s, float maxWidth, List<String> out) throws IOException {
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= s.length()) break;

            int start = i;
            int lastBreak = -1;
            int pos = i;

            while (pos < s.length()) {
                char ch = s.charAt(pos);
                if (Character.isWhitespace(ch) || ch == '-') lastBreak = pos;

                String cand = s.substring(start, pos + 1);
                if (width(font, size, cand) > maxWidth) break;
                pos++;
            }

            if (pos >= s.length()) {
                out.add(s.substring(start));
                return;
            }

            if (lastBreak >= start) {
                int cut = lastBreak + (s.charAt(lastBreak) == '-' ? 1 : 0);
                out.add(rtrim(s.substring(start, cut)));
                i = lastBreak + 1;
            } else {
                out.add(s.substring(start, pos));
                i = pos;
            }
        }
    }

    public static float width(PDFont font, float size, String s) throws IOException {
        if (s == null || s.isEmpty()) return 0;
        return font.getStringWidth(s) / 1000f * size;
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
