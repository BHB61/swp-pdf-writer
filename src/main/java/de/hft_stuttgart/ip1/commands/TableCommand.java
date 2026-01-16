package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import de.hft_stuttgart.ip1.Table;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TableCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {
        int cols = -1, rows = -1;
        List<Float> widths = null;
        List<Float> heights = null;
        Color line = Color.BLACK;
        Color bg = Color.WHITE;
        float thickness = 2f;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i).toLowerCase();
            if ("columns".equals(t)) cols = Integer.parseInt(toks.get(++i));
            else if ("rows".equals(t)) rows = Integer.parseInt(toks.get(++i));
            else if ("width".equals(t)) widths = PdfScript.parseList(toks.get(++i), cols);
            else if ("height".equals(t)) heights = PdfScript.parseList(toks.get(++i), rows);
            else if ("lines".equals(t)) line = PdfScript.parseColor(toks.get(++i));
            else if ("background".equals(t)) bg = PdfScript.parseColor(toks.get(++i));
            else if ("thickness".equals(t)) thickness = Float.parseFloat(toks.get(++i));
        }

        if (cols <= 0 || rows <= 0) throw new IllegalArgumentException("table needs columns/rows");
        if (widths == null || heights == null) throw new IllegalArgumentException("table needs width/height");

        // ----- FIT TABLE INTO PAGE -----
        widths = new ArrayList<>(widths);
        heights = new ArrayList<>(heights);

        float margin = 50f;
        float pageW = ctx.page().getMediaBox().getWidth();
        float pageH = ctx.page().getMediaBox().getHeight();

        float boxW = pageW - 2 * margin;

        // aktuelle Tabellengröße
        float sumW = 0;
        for (float v : widths) sumW += v;

        float sumH = 0;
        for (float v : heights) sumH += v;

        // falls Tabelle unten rausläuft → neue Seite
        float bottomY = ctx.cursorY() - sumH;
        if (bottomY < margin) {
            ctx.newPage();
        }

        // falls Tabelle zu breit → proportional skalieren
        if (sumW > boxW) {
            float scale = boxW / sumW;
            for (int i = 0; i < widths.size(); i++) {
                widths.set(i, widths.get(i) * scale);
            }
        }

        Table table = new Table(ctx.cursorX(), ctx.cursorY(), cols, rows, widths, heights);
        ctx.setTable(table);

        float w = table.totalW();
        float h = table.totalH();

        ctx.cs().setNonStrokingColor(bg);
        ctx.cs().addRect(table.x, table.y - h, w, h);
        ctx.cs().fill();

        ctx.cs().setStrokingColor(line);
        ctx.cs().setLineWidth(thickness);

        ctx.cs().addRect(table.x, table.y - h, w, h);
        ctx.cs().stroke();

        float xx = table.x;
        for (int c = 0; c < cols - 1; c++) {
            xx += widths.get(c);
            ctx.cs().moveTo(xx, table.y);
            ctx.cs().lineTo(xx, table.y - h);
        }

        float yy = table.y;
        for (int r = 0; r < rows - 1; r++) {
            yy -= heights.get(r);
            ctx.cs().moveTo(table.x, yy);
            ctx.cs().lineTo(table.x + w, yy);
        }
        ctx.cs().stroke();

        ctx.setCursor(ctx.cursorX(), table.y - h - 10);
    }
}
