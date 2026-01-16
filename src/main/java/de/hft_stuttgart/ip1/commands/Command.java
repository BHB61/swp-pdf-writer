package de.hft_stuttgart.ip1.commands;

import de.hft_stuttgart.ip1.PdfScript;
import java.util.List;

public interface Command {
    void execute(PdfScript ctx, List<String> tokens) throws Exception;
}