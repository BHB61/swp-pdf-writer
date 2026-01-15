package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class OutputCommand implements Command {
    @Override
    public void execute(PdfScript ctx, List<String> toks) {
        if (toks.size() < 2) throw new IllegalArgumentException("output needs a file name");
        Path out = Path.of(toks.get(1));

        File f = out.toFile();
        if (f.exists() && !f.delete()) throw new IllegalArgumentException("cannot delete: " + out);

        ctx.setOut(out);
    }
}
