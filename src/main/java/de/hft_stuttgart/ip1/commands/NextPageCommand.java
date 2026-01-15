package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;

import java.util.List;

public class NextPageCommand implements Command {
    @Override
    public void execute(PdfScript ctx, List<String> tokens) throws Exception {
        ctx.newPage();
        ctx.clearTable();
    }
}
