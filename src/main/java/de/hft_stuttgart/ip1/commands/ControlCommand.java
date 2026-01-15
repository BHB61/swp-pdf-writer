package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import de.hft_stuttgart.ip1.Table;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ControlCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {
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
            Table table = ctx.table();
            if (table == null) throw new IllegalStateException("control @cell requires table");
            float[] p = table.cellTextPos(cellC, cellR, 3);
            px = p[0];
            py = p[1];
            w = Math.max(30, table.colW(cellC) - 6);
            h = Math.max(14, Math.min(22, table.rowH(cellR) - 6));
        } else {
            px = (x != null) ? x : ctx.cursorX();
            py = (y != null) ? y : ctx.cursorY();
            w = 160;
            h = 18;
        }

        String fieldName = "f" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        switch (type.toLowerCase()) {
            case "textbox" -> {
                PDTextField f = new PDTextField(ctx.acroForm());
                f.setPartialName(fieldName);
                if (content != null) f.setValue(content);
                addWidget(ctx, f, px, py - h, w, h);
                ctx.acroForm().getFields().add(f);
            }
            case "option" -> {
                PDCheckBox f = new PDCheckBox(ctx.acroForm());
                f.setPartialName(fieldName);
                addWidget(ctx, f, px, py - h, h, h);
                ctx.acroForm().getFields().add(f);
                if ("true".equalsIgnoreCase(content) || "1".equals(content)) f.check();
            }
            case "dropdown" -> {
                PDComboBox f = new PDComboBox(ctx.acroForm());
                f.setPartialName(fieldName);
                if (typeArg != null) f.setOptions(Arrays.asList(typeArg.split(";")));
                if (content != null) f.setValue(content);
                addWidget(ctx, f, px, py - h, w, h);
                ctx.acroForm().getFields().add(f);
            }
            default -> throw new IllegalArgumentException("unknown control type: " + type);
        }

        ctx.setCursor(px, py - h - 10);
    }

    private static void addWidget(PdfScript ctx, PDTerminalField field, float x, float y, float w, float h) throws Exception {
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(x, y, w, h));
        widget.setPage(ctx.page());
        ctx.page().getAnnotations().add(widget);
        field.setWidgets(List.of(widget));
    }
}
