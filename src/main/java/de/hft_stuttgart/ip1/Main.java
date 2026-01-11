package de.hft_stuttgart.ip1;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

public class Main {

    private static final String PROP_FILE = ".pdfscript.properties";
    private static final String PROP_VIEWER = "viewer.command";

    private static final String DEFAULT_SCRIPT = """
            output "out.pdf".
            font size 12 style regular colour black "Helvetica".
            print @ 50,800 width 500 alignment left "Hallo PDFScript! Zeilenumbruch funktioniert auch bei längeren Texten, solange width gesetzt ist.".
            table columns 3 rows 3 width 120,160,200 height 20,20,20 lines black background 0xF0F0F0 thickness 2.
            print @cell 0,0 width 110 alignment left "Zelle (0,0)".
            print @cell 1,1 width 150 alignment left "Mehrzeiliger Text in einer Zelle mit Umbruch.".
            nextpage.
            control @ 50,800 type textbox content "Textfeld".
            control @ 50,760 type dropdown "A;B;C" content "B".
            control @ 50,720 type option content "true".
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startUi);
    }

    private static void startUi() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame f = new JFrame("Scripting PDF (kompakt)");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout(8, 8));

        JTextArea area = new JTextArea(DEFAULT_SCRIPT, 28, 100);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JLabel status = new JLabel(" ");

        JButton run = new JButton("Run");
        JButton open = new JButton("Open…");
        JButton save = new JButton("Save…");
        JButton viewer = new JButton("Viewer…");

        JFileChooser chooser = new JFileChooser();

        open.addActionListener(e -> {
            if (chooser.showOpenDialog(f) != JFileChooser.APPROVE_OPTION) return;
            try {
                Path p = chooser.getSelectedFile().toPath();
                area.setText(Files.readString(p, StandardCharsets.UTF_8));
                status.setText("geladen: " + p.getFileName());
            } catch (Exception ex) {
                status.setText("Fehler: " + ex.getMessage());
            }
        });

        save.addActionListener(e -> {
            if (chooser.showSaveDialog(f) != JFileChooser.APPROVE_OPTION) return;
            try {
                Path p = chooser.getSelectedFile().toPath();
                Files.writeString(p, area.getText(), StandardCharsets.UTF_8);
                status.setText("gespeichert: " + p.getFileName());
            } catch (Exception ex) {
                status.setText("Fehler: " + ex.getMessage());
            }
        });

        viewer.addActionListener(e -> configureViewer(f, status));

        run.addActionListener((ActionEvent e) -> {
            try {
                Path pdf = PdfScript.run(area.getText());
                status.setText("OK: " + pdf.toAbsolutePath());
                openPdf(pdf);
            } catch (Exception ex) {
                status.setText("Fehler: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(run);
        top.add(open);
        top.add(save);
        top.add(viewer);

        f.add(top, BorderLayout.NORTH);
        f.add(new JScrollPane(area), BorderLayout.CENTER);
        f.add(status, BorderLayout.SOUTH);

        f.pack();
        f.setLocationByPlatform(true);
        f.setVisible(true);
    }

    private static void configureViewer(Component parent, JLabel status) {
        Properties p = loadProps();
        String current = p.getProperty(PROP_VIEWER, defaultViewer());
        String input = JOptionPane.showInputDialog(parent,
                "PDF-Viewer Kommando\nPlatzhalter: {file}\n\nBeispiele:\n" +
                        "Linux: xdg-open {file}\nmacOS: open {file}\nWindows: cmd /c start \"\" {file}",
                current);
        if (input == null) return;
        p.setProperty(PROP_VIEWER, input.trim().isEmpty() ? defaultViewer() : input.trim());
        saveProps(p);
        status.setText("Viewer gespeichert.");
    }

    private static void openPdf(Path pdf) {
        Properties p = loadProps();
        String cmd = p.getProperty(PROP_VIEWER, defaultViewer());
        cmd = cmd.replace("{file}", pdf.toAbsolutePath().toString());

        try {
            // simple split – reicht für die typischen Standard-Commands
            new ProcessBuilder(cmd.split("\\s+")).inheritIO().start();
        } catch (Exception ignored) {}
    }

    private static Properties loadProps() {
        Properties p = new Properties();
        Path file = Paths.get(System.getProperty("user.home"), PROP_FILE);
        if (Files.exists(file)) {
            try (var in = Files.newInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {}
        }
        return p;
    }

    private static void saveProps(Properties p) {
        Path file = Paths.get(System.getProperty("user.home"), PROP_FILE);
        try (var out = Files.newOutputStream(file)) {
            p.store(out, "pdfscript settings");
        } catch (Exception ignored) {}
    }

    private static String defaultViewer() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "cmd /c start \"\" {file}";
        if (os.contains("mac")) return "open {file}";
        return "xdg-open {file}";
    }
}

