package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.List;

public class FontCommand implements Command {

    @Override
    public void execute(PdfScript ctx, List<String> toks) {
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

        PDFont f = PdfScript.mapFont(name, style);
        ctx.setFont(f);
        ctx.setFontSize(size);
        ctx.setFontColor(PdfScript.parseColor(colour));
    }
}
