package de.hft_stuttgart.ip1;


import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class PdfScript {

    private PDDocument doc;
    private PDPage page;
    private PDPageContentStream cs;
    private PDAcroForm acroForm;

    private PDFont font = PDType1Font.HELVETICA;
    private float fontSize = 12;
    private Color fontColor = Color.BLACK;

    private float cursorX = 50;
    private float cursorY = 800;

    private Table table;
    private Path out = Path.of("out.pdf");

    public static Path run(String script) throws Exception {
        return new PdfScript().execute(script);
    }

    private Path execute(String script) throws Exception {
        doc = new PDDocument();
        acroForm = new PDAcroForm(doc);
        doc.getDocumentCatalog().setAcroForm(acroForm);
        acroForm.setDefaultResources(new org.apache.pdfbox.pdmodel.PDResources());
        acroForm.setDefaultAppearance("/Helv 12 Tf 0 g");
        var helv = PDType1Font.HELVETICA;
        var helvName = acroForm.getDefaultResources().add(helv);
        acroForm.setDefaultAppearance("/" + helvName.getName() + " 12 Tf 0 g");

        newPage();

        for (String stmt : splitStatements(script)) {
            stmt = stmt.trim();
            if (stmt.isEmpty()) continue;
            execOne(stmt);
        }

        closeStream();
        doc.save(out.toFile());
        doc.close();
        return out;
    }

    // ---- statement splitting by '.' outside strings
    private static List<String> splitStatements(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                // """ ... """
                if (i + 2 < s.length() && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
                    int end = s.indexOf("\"\"\"", i + 3);
                    if (end < 0) throw new IllegalArgumentException("Unclosed \"\"\" string");
                    cur.append(s, i, end + 3);
                    i = end + 2;
                    continue;
                }
                inStr = !inStr;
            }

            if (c == '.' && !inStr) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ---- tokenizing: quoted strings become one token
    private static List<String> tokenize(String stmt) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < stmt.length()) {
            while (i < stmt.length() && Character.isWhitespace(stmt.charAt(i))) i++;
            if (i >= stmt.length()) break;

            char c = stmt.charAt(i);
            if (c == '"') {
                if (i + 2 < stmt.length() && stmt.charAt(i + 1) == '"' && stmt.charAt(i + 2) == '"') {
                    int end = stmt.indexOf("\"\"\"", i + 3);
                    if (end < 0) throw new IllegalArgumentException("Unclosed \"\"\" string");
                    String raw = stmt.substring(i + 3, end).replace("\r", "").replace("\n", " ");
                    out.add(decode(raw));
                    i = end + 3;
                } else {
                    i++;
                    StringBuilder sb = new StringBuilder();
                    while (i < stmt.length()) {
                        char ch = stmt.charAt(i);
                        if (ch == '"' && stmt.charAt(i - 1) != '\\') break;
                        sb.append(ch);
                        i++;
                    }
                    if (i >= stmt.length()) throw new IllegalArgumentException("Unclosed string");
                    out.add(decode(sb.toString()));
                    i++; // closing "
                }
            } else {
                int start = i;
                while (i < stmt.length() && !Character.isWhitespace(stmt.charAt(i))) i++;
                out.add(stmt.substring(start, i));
            }
        }
        return out;
    }

    // only \\ \n \"
    private static String decode(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\') { sb.append(c); continue; }
            if (i + 1 >= raw.length()) throw new IllegalArgumentException("bad escape");
            char n = raw.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case '\\' -> sb.append('\\');
                case '"' -> sb.append('"');
                default -> throw new IllegalArgumentException("bad escape: \\" + n);
            }
        }
        return sb.toString();
    }

    private void execOne(String stmt) throws Exception {
        List<String> toks = tokenize(stmt);
        if (toks.isEmpty()) return;

        String cmd = toks.get(0).toLowerCase();
        switch (cmd) {
            case "output" -> cmdOutput(toks);
            case "font" -> cmdFont(toks);
            case "print" -> cmdPrint(toks);
            case "table" -> cmdTable(toks);
            case "nextpage" -> cmdNextPage();
            case "image" -> cmdImage(toks);
            case "control" -> cmdControl(toks);
            default -> throw new IllegalArgumentException("Unknown command: " + cmd);
        }
    }

    // ---- commands

    private void cmdOutput(List<String> toks) {
        if (toks.size() < 2) throw new IllegalArgumentException("output needs a file name");
        out = Path.of(toks.get(1));

        File f = out.toFile();
        if (f.exists() && !f.delete()) throw new IllegalArgumentException("cannot delete: " + out);
    }

    private void cmdNextPage() throws Exception {
        newPage();
        table = null;
    }

    private void cmdFont(List<String> toks) {
        Float size = null;
        String style = "regular";
        String colour = "black";
        String name = null;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i).toLowerCase();
            if ("size".equals(t)) size = Float.parseFloat(toks.get(++i));
            else if ("style".equals(t)) style = toks.get(++i);
            else if ("colour".equals(t) || "color".equals(t)) colour = toks.get(++i);
            else name = toks.get(i);
        }

        if (size == null) throw new IllegalArgumentException("font requires size");
        if (name == null) throw new IllegalArgumentException("font requires font name");

        font = mapFont(name, style);
        fontSize = size;
        fontColor = parseColor(colour);
    }

    private void cmdPrint(List<String> toks) throws Exception {
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
            if (table == null) throw new IllegalStateException("print @cell requires table");
            float[] p = table.cellTextPos(cellC, cellR, 3);
            px = p[0];
            py = p[1];
            maxW = (width != null) ? width : (table.colW(cellC) - 6);
        } else {
            px = (x != null) ? x : cursorX;
            py = (y != null) ? y : cursorY;
            maxW = (width != null) ? width : -1;
        }

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(fontColor);
        cs.newLineAtOffset(px, py);

        float lineH = fontSize * 4f / 3f;
        List<String> lines = (maxW > 0) ? Wrap.wrap(font, fontSize, text, maxW) : List.of(text);

        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);

            float dx = 0;
            if (maxW > 0) {
                float w = Wrap.width(font, fontSize, line);
                if ("center".equalsIgnoreCase(align)) dx = (maxW - w) / 2f;
                else if ("right".equalsIgnoreCase(align)) dx = (maxW - w);
            }
            if (dx != 0) cs.newLineAtOffset(dx, 0);
            cs.showText(line);
            if (dx != 0) cs.newLineAtOffset(-dx, 0);

            if (li < lines.size() - 1) cs.newLineAtOffset(0, -lineH);
        }
        cs.endText();

        cursorX = px;
        cursorY = py - lineH * (lines.size() + 1);
    }

    private void cmdTable(List<String> toks) throws Exception {
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
            else if ("width".equals(t)) widths = parseList(toks.get(++i), cols);
            else if ("height".equals(t)) heights = parseList(toks.get(++i), rows);
            else if ("lines".equals(t)) line = parseColor(toks.get(++i));
            else if ("background".equals(t)) bg = parseColor(toks.get(++i));
            else if ("thickness".equals(t)) thickness = Float.parseFloat(toks.get(++i));
        }

        if (cols <= 0 || rows <= 0) throw new IllegalArgumentException("table needs columns/rows");
        if (widths == null || heights == null) throw new IllegalArgumentException("table needs width/height");

        // place at cursor; y is top edge
        table = new Table(cursorX, cursorY, cols, rows, widths, heights);

        float w = table.totalW();
        float h = table.totalH();

        cs.setNonStrokingColor(bg);
        cs.addRect(table.x, table.y - h, w, h);
        cs.fill();

        cs.setStrokingColor(line);
        cs.setLineWidth(thickness);

        cs.addRect(table.x, table.y - h, w, h);
        cs.stroke();

        float xx = table.x;
        for (int c = 0; c < cols - 1; c++) {
            xx += widths.get(c);
            cs.moveTo(xx, table.y);
            cs.lineTo(xx, table.y - h);
        }

        float yy = table.y;
        for (int r = 0; r < rows - 1; r++) {
            yy -= heights.get(r);
            cs.moveTo(table.x, yy);
            cs.lineTo(table.x + w, yy);
        }
        cs.stroke();

        cursorY = table.y - h - 10;
    }

    private void cmdImage(List<String> toks) throws Exception {
        Float x = null, y = null;
        Float w = null, h = null;
        String file = null;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i);
            if ("@".equals(t)) {
                String[] p = toks.get(++i).split(",");
                x = Float.parseFloat(p[0]);
                y = Float.parseFloat(p[1]);
            } else if ("size".equalsIgnoreCase(t)) {
                String[] p = toks.get(++i).split(",");
                w = Float.parseFloat(p[0]);
                h = Float.parseFloat(p[1]);
            } else {
                file = toks.get(i);
            }
        }
        if (file == null) throw new IllegalArgumentException("image needs file path");

        float px = (x != null) ? x : cursorX;
        float py = (y != null) ? y : cursorY;

        PDImageXObject img = PDImageXObject.createFromFile(file, doc);
        if (w != null && h != null) cs.drawImage(img, px, py - h, w, h);
        else cs.drawImage(img, px, py - img.getHeight(), img.getWidth(), img.getHeight());

        cursorX = px;
        cursorY = py - 10;
    }

    private void cmdControl(List<String> toks) throws Exception {
        // syntax (kompakt):
        // control @ x,y type textbox content "..."
        // control @ x,y type option content "true|false"
        // control @ x,y type dropdown "A;B;C" content "B"
        // control @cell c,r type textbox ...
        Float x = null, y = null;
        Integer cellC = null, cellR = null;
        String type = null;
        String typeArg = null;
        String content = null;

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i);

            if ("@".equals(t)) {
                String[] p = toks.get(++i).split(",");
                x = Float.parseFloat(p[0]);
                y = Float.parseFloat(p[1]);
            } else if ("@cell".equalsIgnoreCase(t) || "cell".equalsIgnoreCase(t)) {
                String[] p = toks.get(++i).split(",");
                cellC = Integer.parseInt(p[0]);
                cellR = Integer.parseInt(p[1]);
            } else if ("type".equalsIgnoreCase(t)) {
                type = toks.get(++i);
                // dropdown has one extra arg token (options separated by ;)
                if ("dropdown".equalsIgnoreCase(type) && i + 1 < toks.size()) {
                    String maybe = toks.get(i + 1);
                    if (!"content".equalsIgnoreCase(maybe) && !"@".equals(maybe) && !"@cell".equalsIgnoreCase(maybe)) {
                        typeArg = toks.get(++i);
                    }
                }
            } else if ("content".equalsIgnoreCase(t)) {
                content = toks.get(++i);
            }
        }

        if (type == null) throw new IllegalArgumentException("control needs type");

        float px, py, w, h;
        if (cellC != null && cellR != null) {
            if (table == null) throw new IllegalStateException("control @cell requires table");
            float[] p = table.cellTextPos(cellC, cellR, 3);
            px = p[0];
            py = p[1];
            w = Math.max(30, table.colW(cellC) - 6);
            h = Math.max(14, Math.min(22, table.rowH(cellR) - 6));
        } else {
            px = (x != null) ? x : cursorX;
            py = (y != null) ? y : cursorY;
            w = 160;
            h = 18;
        }

        String fieldName = "f" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        switch (type.toLowerCase()) {
            case "textbox" -> {
                PDTextField f = new PDTextField(acroForm);
                f.setPartialName(fieldName);
                if (content != null) f.setValue(content);
                addWidget(f, px, py - h, w, h);
                acroForm.getFields().add(f);
            }
            case "option" -> {
                PDCheckBox f = new PDCheckBox(acroForm);
                f.setPartialName(fieldName);
                addWidget(f, px, py - h, h, h);
                acroForm.getFields().add(f);
                if ("true".equalsIgnoreCase(content) || "1".equals(content)) f.check();
            }
            case "dropdown" -> {
                PDComboBox f = new PDComboBox(acroForm);
                f.setPartialName(fieldName);
                if (typeArg != null) f.setOptions(Arrays.asList(typeArg.split(";")));
                if (content != null) f.setValue(content);
                addWidget(f, px, py - h, w, h);
                acroForm.getFields().add(f);
            }
            default -> throw new IllegalArgumentException("unknown control type: " + type);
        }

        cursorX = px;
        cursorY = py - h - 10;
    }

    private void addWidget(PDTerminalField field, float x, float y, float w, float h) throws Exception {
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(x, y, w, h));
        widget.setPage(page);
        page.getAnnotations().add(widget);
        field.setWidgets(List.of(widget));
    }

    // ---- runtime helpers

    private void newPage() throws Exception {
        closeStream();
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        cursorX = 50;
        cursorY = page.getMediaBox().getHeight() - 50;
    }

    private void closeStream() throws Exception {
        if (cs != null) cs.close();
        cs = null;
    }

    private static PDFont mapFont(String name, String style) {
        String n = name.toLowerCase();
        String s = (style == null ? "regular" : style).toLowerCase();

        boolean helv = n.contains("helvetica");
        boolean times = n.contains("times");
        boolean cour = n.contains("courier");
        if (!helv && !times && !cour) helv = true;

        if (helv) {
            return switch (s) {
                case "bold" -> PDType1Font.HELVETICA_BOLD;
                case "italic" -> PDType1Font.HELVETICA_OBLIQUE;
                case "bolditalic" -> PDType1Font.HELVETICA_BOLD_OBLIQUE;
                default -> PDType1Font.HELVETICA;
            };
        }
        if (times) {
            return switch (s) {
                case "bold" -> PDType1Font.TIMES_BOLD;
                case "italic" -> PDType1Font.TIMES_ITALIC;
                case "bolditalic" -> PDType1Font.TIMES_BOLD_ITALIC;
                default -> PDType1Font.TIMES_ROMAN;
            };
        }
        return switch (s) {
            case "bold" -> PDType1Font.COURIER_BOLD;
            case "italic" -> PDType1Font.COURIER_OBLIQUE;
            case "bolditalic" -> PDType1Font.COURIER_BOLD_OBLIQUE;
            default -> PDType1Font.COURIER;
        };
    }

    private static Color parseColor(String token) {
        if (token == null) return Color.BLACK;
        String t = token.trim();
        if (t.startsWith("0x") || t.startsWith("0X")) return new Color((int) Long.parseLong(t.substring(2), 16));
        if (t.startsWith("#")) return new Color((int) Long.parseLong(t.substring(1), 16));
        try { return (Color) Color.class.getField(t.toUpperCase()).get(null); } catch (Exception ignored) {}
        return Color.BLACK;
    }

    private static List<Float> parseList(String spec, int targetLen) {
        // "10,10,20,20,50*" -> repeat last if shorter; '*' is ignored (repeat-last behavior)
        boolean star = spec.endsWith("*");
        String s = star ? spec.substring(0, spec.length() - 1) : spec;

        String[] parts = s.split(",");
        List<Float> vals = new ArrayList<>();
        for (String p : parts) vals.add(Float.parseFloat(p));

        if (targetLen > 0) {
            while (vals.size() < targetLen) vals.add(vals.get(vals.size() - 1));
            if (vals.size() > targetLen) vals = vals.subList(0, targetLen);
        }
        return vals;
    }

    // ---- simple table
    private static class Table {
        final float x, y; // y is TOP edge
        final int cols, rows;
        final List<Float> w, h;

        Table(float x, float y, int cols, int rows, List<Float> w, List<Float> h) {
            this.x = x; this.y = y; this.cols = cols; this.rows = rows;
            this.w = w; this.h = h;
        }

        float totalW() { float s = 0; for (float v : w) s += v; return s; }
        float totalH() { float s = 0; for (float v : h) s += v; return s; }

        float colW(int c) { return w.get(c); }
        float rowH(int r) { return h.get(r); }

        float cellX(int c) {
            float xx = x;
            for (int i = 0; i < c; i++) xx += w.get(i);
            return xx;
        }

        float cellY(int r) {
            float yy = y;
            for (int i = 0; i < r; i++) yy -= h.get(i);
            return yy - h.get(r);
        }

        float[] cellTextPos(int c, int r, float inset) {
            float px = cellX(c) + inset;
            float py = cellY(r) + h.get(r) - inset - 2;
            return new float[]{px, py};
        }
    }
}

