package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import de.hft_stuttgart.ip1.Table;
import de.hft_stuttgart.ip1.Wrap;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.List;
import java.util.Locale;

public class PrintCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {

        Float x = null, y = null;
        Integer cellC = null, cellR = null;
        Float width = null;
        String alignment = "left";

        // statt null -> später fallback
        String text = null;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i);

            if ("@".equals(t)) {
                if (i + 1 >= toks.size()) break;
                String[] p = toks.get(++i).split(",");
                if (p.length == 2) {
                    x = Float.parseFloat(p[0]);
                    y = Float.parseFloat(p[1]);
                }

            } else if ("@cell".equalsIgnoreCase(t) || "cell".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                String[] p = toks.get(++i).split(",");
                if (p.length == 2) {
                    cellC = Integer.parseInt(p[0]);
                    cellR = Integer.parseInt(p[1]);
                }

            } else if ("width".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                width = Float.parseFloat(toks.get(++i));

            } else if ("alignment".equalsIgnoreCase(t) || "align".equalsIgnoreCase(t)) {
                String raw = (i + 1 < toks.size()) ? toks.get(++i) : "left";
                alignment = parseAlignmentLenient(raw);

            } else if (looksLikeAlignmentPlaceholder(t)) {
                alignment = "left";

            } else {
                // Nicht-Keyword -> als Text interpretieren (letzter gewinnt)
                text = t;
            }
        }

        // ✅ NIE MEHR crashen: wenn Text fehlt, drucke einfach nichts (oder ein Leerzeichen)
        if (text == null) text = "";

        float px, py, maxW;
        float inset = 6f;

        if (cellC != null && cellR != null) {
            Table table = ctx.table();
            if (table == null) {
                // ohne Tabelle: einfach an Cursor schreiben, statt crash
                px = (x != null) ? x : ctx.cursorX();
                py = (y != null) ? y : ctx.cursorY();
                maxW = (width != null) ? width : -1;
            } else {
                px = table.cellX(cellC) + inset;

                float cellTopY = table.y;
                for (int r = 0; r < cellR; r++) cellTopY -= table.rowH(r);
                py = cellTopY - inset - 2;

                float cellW = table.colW(cellC);
                float autoW = Math.max(1, cellW - 2 * inset);
                maxW = (width != null) ? Math.min(width, autoW) : autoW;
            }
        } else {
            px = (x != null) ? x : ctx.cursorX();
            py = (y != null) ? y : ctx.cursorY();
            maxW = (width != null) ? width : -1;
        }

        PDFont font = ctx.font();
        float fontSize = ctx.fontSize();
        float lineH = fontSize * 4f / 3f;

        List<String> lines = (maxW > 0)
                ? Wrap.wrap(font, fontSize, text, maxW)
                : List.of(text);

        ctx.cs().beginText();
        ctx.cs().setFont(font, fontSize);
        ctx.cs().setNonStrokingColor(ctx.fontColor());
        ctx.cs().newLineAtOffset(px, py);

        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);

            float dx = 0f;
            if (maxW > 0) {
                float wLine = Wrap.width(font, fontSize, line);
                if ("center".equals(alignment)) dx = (maxW - wLine) / 2f;
                else if ("right".equals(alignment)) dx = (maxW - wLine);
            }

            if (dx != 0) ctx.cs().newLineAtOffset(dx, 0);
            ctx.cs().showText(line);
            if (dx != 0) ctx.cs().newLineAtOffset(-dx, 0);

            if (li < lines.size() - 1) ctx.cs().newLineAtOffset(0, -lineH);
        }

        ctx.cs().endText();

        ctx.setCursor(px, py - lineH * (lines.size() + 1));
    }

    private static boolean looksLikeAlignmentPlaceholder(String s) {
        if (s == null) return false;
        String t = s.toLowerCase(Locale.ROOT);
        return t.contains("left") && t.contains("center") && t.contains("right");
    }

    private static String parseAlignmentLenient(String raw) {
        if (raw == null) return "left";
        String s = raw.toLowerCase(Locale.ROOT).trim();

        if (looksLikeAlignmentPlaceholder(s)) return "left";

        s = s.replaceAll("[^a-z]", "");
        if (s.equals("centre")) s = "center";

        if (s.equals("left") || s.equals("center") || s.equals("right")) return s;
        return "left";
    }
}
