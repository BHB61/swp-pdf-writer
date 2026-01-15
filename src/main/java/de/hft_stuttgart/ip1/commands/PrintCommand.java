package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import de.hft_stuttgart.ip1.Table;
import de.hft_stuttgart.ip1.Wrap;

import java.util.List;

public class PrintCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {
        Float x = null, y = null;
        Integer cellC = null, cellR = null;
        Float width = null;
        String align = "left";
        String text = null;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i);
            if ("@".equals(t)) {
                String[] parts = toks.get(++i).split(",");
                x = Float.parseFloat(parts[0]);
                y = Float.parseFloat(parts[1]);
            } else if ("@cell".equalsIgnoreCase(t) || "cell".equalsIgnoreCase(t)) {
                String[] parts = toks.get(++i).split(",");
                cellC = Integer.parseInt(parts[0]);
                cellR = Integer.parseInt(parts[1]);
            } else if ("width".equalsIgnoreCase(t)) {
                width = Float.parseFloat(toks.get(++i));
            } else if ("alignment".equalsIgnoreCase(t)) {
                align = toks.get(++i);
            } else {
                text = toks.get(i);
            }
        }

        if (text == null) throw new IllegalArgumentException("print needs text");

        float px, py, maxW;
        if (cellC != null && cellR != null) {
            Table table = ctx.table();
            if (table == null) throw new IllegalStateException("print @cell requires table");
            float[] p = table.cellTextPos(cellC, cellR, 3);
            px = p[0];
            py = p[1];
            maxW = (width != null) ? width : (table.colW(cellC) - 6);
        } else {
            px = (x != null) ? x : ctx.cursorX();
            py = (y != null) ? y : ctx.cursorY();
            maxW = (width != null) ? width : -1;
        }

        ctx.cs().beginText();
        ctx.cs().setFont(ctx.font(), ctx.fontSize());
        ctx.cs().setNonStrokingColor(ctx.fontColor());
        ctx.cs().newLineAtOffset(px, py);

        float lineH = ctx.fontSize() * 4f / 3f;
        List<String> lines = (maxW > 0) ? Wrap.wrap(ctx.font(), ctx.fontSize(), text, maxW) : List.of(text);

        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);

            float dx = 0;
            if (maxW > 0) {
                float w = Wrap.width(ctx.font(), ctx.fontSize(), line);
                if ("center".equalsIgnoreCase(align)) dx = (maxW - w) / 2f;
                else if ("right".equalsIgnoreCase(align)) dx = (maxW - w);
            }
            if (dx != 0) ctx.cs().newLineAtOffset(dx, 0);
            ctx.cs().showText(line);
            if (dx != 0) ctx.cs().newLineAtOffset(-dx, 0);

            if (li < lines.size() - 1) ctx.cs().newLineAtOffset(0, -lineH);
        }
        ctx.cs().endText();

        ctx.setCursor(px, py - lineH * (lines.size() + 1));
    }
}
