package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.util.List;

public class ImageCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) throws Exception {
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

        float px = (x != null) ? x : ctx.cursorX();
        float py = (y != null) ? y : ctx.cursorY();

        PDImageXObject img = PDImageXObject.createFromFile(file, ctx.doc());
        if (w != null && h != null) ctx.cs().drawImage(img, px, py - h, w, h);
        else ctx.cs().drawImage(img, px, py - img.getHeight(), img.getWidth(), img.getHeight());

        ctx.setCursor(px, py - 10);
    }
}
