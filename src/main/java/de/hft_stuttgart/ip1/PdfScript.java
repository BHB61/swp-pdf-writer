package de.hft_stuttgart.ip1;

import de.hft_stuttgart.ip1.commands.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class PdfScript {

    // -------- runtime state --------
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

    // -------- command registry --------
    private final Map<String, Command> commands = new HashMap<>();

    public PdfScript() {
        commands.put("output", new OutputCommand());
        commands.put("font", new FontCommand());
        commands.put("print", new PrintCommand());
        commands.put("table", new TableCommand());
        commands.put("nextpage", new NextPageCommand());
        commands.put("image", new ImageCommand());
        commands.put("control", new ControlCommand());
    }

    public static Path run(String script) throws Exception {
        return new PdfScript().execute(script);
    }

    private Path execute(String script) throws Exception {
        doc = new PDDocument();
        acroForm = new PDAcroForm(doc);
        doc.getDocumentCatalog().setAcroForm(acroForm);

        // default appearance so text fields work
        acroForm.setDefaultResources(new org.apache.pdfbox.pdmodel.PDResources());
        var helvName = acroForm.getDefaultResources().add(PDType1Font.HELVETICA);
        acroForm.setDefaultAppearance("/" + helvName.getName() + " 12 Tf 0 g");

        // IMPORTANT: helps many viewers show checkbox/radio visuals
        acroForm.setNeedAppearances(true);

        newPage();

        for (String stmt : splitStatements(script)) {
            stmt = stmt.trim();
            if (stmt.isEmpty()) continue;

            List<String> toks = tokenize(stmt);
            if (toks.isEmpty()) continue;

            String name = toks.get(0).toLowerCase(Locale.ROOT);
            Command cmd = commands.get(name);
            if (cmd == null) throw new IllegalArgumentException("Unknown command: " + name);

            cmd.execute(this, toks);
        }

        closeStream();

        // IMPORTANT: generate/refresh appearance streams for form fields
        try { acroForm.refreshAppearances(); } catch (Exception ignored) {}

        doc.save(out.toFile());
        doc.close();
        return out;
    }

    // =====================
    // Accessors for Commands
    // =====================
    public PDDocument doc() { return doc; }
    public PDPage page() { return page; }
    public PDPageContentStream cs() { return cs; }
    public PDAcroForm acroForm() { return acroForm; }

    public PDFont font() { return font; }
    public float fontSize() { return fontSize; }
    public Color fontColor() { return fontColor; }

    public float cursorX() { return cursorX; }
    public float cursorY() { return cursorY; }

    public Table table() { return table; }
    public void setTable(Table t) { table = t; }
    public void clearTable() { table = null; }

    public Path out() { return out; }
    public void setOut(Path out) { this.out = out; }

    public void setFont(PDFont f) { font = f; }
    public void setFontSize(float s) { fontSize = s; }
    public void setFontColor(Color c) { fontColor = c; }

    public void setCursor(float x, float y) { cursorX = x; cursorY = y; }

    public void newPage() throws Exception {
        closeStream();
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        cursorX = 50;
        cursorY = page.getMediaBox().getHeight() - 50;
        table = null;
    }

    private void closeStream() throws Exception {
        if (cs != null) cs.close();
        cs = null;
    }

    // =========================
    // Parser: '.' outside strings
    // supports "..." and """..."""
    // =========================
    static List<String> splitStatements(String s) {
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

    // tokenizing: quoted strings become one token, supports """..."""
    static List<String> tokenize(String stmt) {
        List<String> out = new ArrayList<>();
        int i = 0;

        while (i < stmt.length()) {
            while (i < stmt.length() && Character.isWhitespace(stmt.charAt(i))) i++;
            if (i >= stmt.length()) break;

            char c = stmt.charAt(i);

            if (c == '"') {
                // """..."""
                if (i + 2 < stmt.length() && stmt.charAt(i + 1) == '"' && stmt.charAt(i + 2) == '"') {
                    int end = stmt.indexOf("\"\"\"", i + 3);
                    if (end < 0) throw new IllegalArgumentException("Unclosed \"\"\" string");
                    String raw = stmt.substring(i + 3, end).replace("\r", "").replace("\n", " ");
                    out.add(decode(raw));
                    i = end + 3;
                } else {
                    // "..."
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
    static String decode(String raw) {
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

    // =========================
    // Helpers used by Commands
    // =========================

    public static PDFont mapFont(String name, String style) {
        String n = name.toLowerCase(Locale.ROOT);
        String s = (style == null ? "regular" : style).toLowerCase(Locale.ROOT);

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

    public static Color parseColor(String token) {
        if (token == null) return Color.BLACK;
        String t = token.trim();

        if (t.startsWith("0x") || t.startsWith("0X")) {
            return new Color((int) Long.parseLong(t.substring(2), 16));
        }
        if (t.startsWith("#")) {
            return new Color((int) Long.parseLong(t.substring(1), 16));
        }

        try {
            return (Color) Color.class.getField(t.toUpperCase(Locale.ROOT)).get(null);
        } catch (Exception ignored) {}

        return Color.BLACK;
    }

    public static List<Float> parseList(String spec, int targetLen) {
        // "10,10,20,20,50*" -> repeat last to reach targetLen; '*' means "repeat last"
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
}
