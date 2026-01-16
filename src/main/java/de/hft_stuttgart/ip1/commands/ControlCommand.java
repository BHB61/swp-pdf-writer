package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import de.hft_stuttgart.ip1.Table;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.util.*;

public class ControlCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {

        Float x = null, y = null;
        Integer cellC = null, cellR = null;

        String type = null;
        String typeArg = null;   // dropdown options "A;B;C"
        String content = null;   // value
        String group = null;     // radio group name
        String selected = null;  // selected radio value
        Integer max = null;      // textbox max len

        for (int i = 1; i < toks.size(); i++) {
            String t = toks.get(i);

            if ("@".equals(t)) {
                if (i + 1 >= toks.size()) break;
                String[] p = toks.get(++i).split(",");
                x = Float.parseFloat(p[0]);
                y = Float.parseFloat(p[1]);

            } else if ("@cell".equalsIgnoreCase(t) || "cell".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                String[] p = toks.get(++i).split(",");
                cellC = Integer.parseInt(p[0]);
                cellR = Integer.parseInt(p[1]);

            } else if ("type".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                type = toks.get(++i);

                if ("dropdown".equalsIgnoreCase(type) && i + 1 < toks.size()) {
                    String maybe = toks.get(i + 1);
                    if (!isKeyword(maybe)) {
                        typeArg = toks.get(++i);
                    }
                }

            } else if ("content".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                content = toks.get(++i);

            } else if ("group".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                group = toks.get(++i);

            } else if ("selected".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                selected = toks.get(++i);

            } else if ("max".equalsIgnoreCase(t) || "maxlength".equalsIgnoreCase(t)) {
                if (i + 1 >= toks.size()) break;
                max = Integer.parseInt(toks.get(++i));
            }
        }

        if (type == null) throw new IllegalArgumentException("control needs type");

        // ---------- compute position ----------
        float px, py, w, h;
        if (cellC != null && cellR != null) {
            Table table = ctx.table();
            if (table == null) throw new IllegalStateException("control @cell requires table");

            float inset = 4f;
            px = table.cellX(cellC) + inset;

            float cellTopY = table.y;
            for (int r = 0; r < cellR; r++) cellTopY -= table.rowH(r);
            py = cellTopY - inset - 2;

            w = Math.max(30, table.colW(cellC) - 2 * inset);
            h = Math.max(14, Math.min(22, table.rowH(cellR) - 2 * inset));
        } else {
            px = (x != null) ? x : ctx.cursorX();
            py = (y != null) ? y : ctx.cursorY();
            w = 180;
            h = 18;
        }

        String fieldName = "f" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        switch (type.toLowerCase(Locale.ROOT)) {

            case "textbox" -> {
                PDTextField f = new PDTextField(ctx.acroForm());
                f.setPartialName(fieldName);
                if (max != null && max > 0) f.setMaxLen(max);
                if (content != null) f.setValue(content);

                PDAnnotationWidget widget = createStyledWidget(ctx, px, py - h, w, h);
                attachSingleWidget(f, widget);
                ctx.acroForm().getFields().add(f);
            }

            case "dropdown" -> {
                PDComboBox f = new PDComboBox(ctx.acroForm());
                f.setPartialName(fieldName);
                if (typeArg != null) f.setOptions(Arrays.asList(typeArg.split(";")));
                if (content != null) f.setValue(content);

                PDAnnotationWidget widget = createStyledWidget(ctx, px, py - h, w, h);
                attachSingleWidget(f, widget);
                ctx.acroForm().getFields().add(f);
            }

            case "option" -> {
                PDCheckBox f = new PDCheckBox(ctx.acroForm());
                f.setPartialName(fieldName);

                PDAnnotationWidget widget = createStyledWidget(ctx, px, py - h, h, h);
                attachSingleWidget(f, widget);
                ctx.acroForm().getFields().add(f);

                if ("true".equalsIgnoreCase(content) || "1".equals(content)) {
                    try { f.check(); } catch (Exception ignored) {}
                } else {
                    try { f.unCheck(); } catch (Exception ignored) {}
                }
            }

            case "radio" -> {
                if (group == null) throw new IllegalArgumentException("radio needs group \"name\"");
                if (content == null) throw new IllegalArgumentException("radio needs content \"value\"");

                PDField existing = ctx.acroForm().getField(group);
                PDRadioButton rb;

                if (existing instanceof PDRadioButton) {
                    rb = (PDRadioButton) existing;
                } else {
                    rb = new PDRadioButton(ctx.acroForm());
                    rb.setPartialName(group);
                    rb.setExportValues(new ArrayList<>());
                    rb.setWidgets(new ArrayList<>());
                    ctx.acroForm().getFields().add(rb);
                }

                PDAnnotationWidget widget = createStyledWidget(ctx, px, py - h, h, h);

                List<PDAnnotationWidget> newWidgets = new ArrayList<>(rb.getWidgets() == null ? List.of() : rb.getWidgets());
                newWidgets.add(widget);
                rb.setWidgets(newWidgets);

                List<String> newEv = new ArrayList<>(rb.getExportValues() == null ? List.of() : rb.getExportValues());
                newEv.add(content);
                rb.setExportValues(newEv);

                if (selected != null && selected.equals(content)) {
                    rb.setValue(content);
                }
            }

            default -> throw new IllegalArgumentException("unknown control type: " + type);
        }

        ctx.setCursor(px, py - h - 14);
    }

    private static boolean isKeyword(String t) {
        if (t == null) return false;
        String s = t.toLowerCase(Locale.ROOT);
        return s.equals("content") || s.equals("@") || s.equals("@cell") || s.equals("cell")
                || s.equals("group") || s.equals("selected") || s.equals("max") || s.equals("maxlength");
    }

    /** Widget mit sichtbarem Rahmen & leichtem Hintergrund (PDFBox 2.0.30: PDColor) */
    private static PDAnnotationWidget createStyledWidget(PdfScript ctx, float x, float y, float w, float h) throws Exception {
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(x, y, w, h));
        widget.setPage(ctx.page());
        ctx.page().getAnnotations().add(widget);

        // Border
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(1f);
        border.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
        widget.setBorderStyle(border);

        PDAppearanceCharacteristicsDictionary mk =
                new PDAppearanceCharacteristicsDictionary(widget.getCOSObject());

        // Default: ruhiger Look
        mk.setBorderColour(new PDColor(new float[]{0.2f, 0.2f, 0.2f}, PDDeviceRGB.INSTANCE));
        mk.setBackground(new PDColor(new float[]{1f, 1f, 1f}, PDDeviceRGB.INSTANCE)); // weiß

        widget.setAppearanceCharacteristics(mk);
        return widget;
    }


    private static void attachSingleWidget(PDTerminalField field, PDAnnotationWidget widget) {
        field.setWidgets(List.of(widget));
    }
}
