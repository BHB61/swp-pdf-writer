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
            
            font size 18 style bold colour 0x003366 "Helvetica".
            print @ 50,810 width 520 alignment left "PDFScript Demo".
            
            font size 11 style regular colour black "Helvetica".
            print @ 50,785 width 520 alignment left "Diese PDF zeigt: Fonts, Farben, Alignment, Zeilenumbruch, Tabellen und Formularfelder.".
            print @ 50,770 width 520 alignment left "Hinweis: Tabellenhöhen werden im Skript festgelegt (keine Auto-Höhe).".
            
            font size 12 style regular colour 0x222222 "Times".
            print @ 50,740 width 520 alignment left "Absatz linksbündig: Ein längerer Text, der automatisch umgebrochen wird, wenn eine maximale Breite angegeben ist. Das ist absichtlich kein Lorem Ipsum, sondern ein neutraler Beispieltext.".
            
            font size 12 style italic colour 0x444444 "Times".
            print @ 50,705 width 520 alignment center "Zentrierte Zeile (alignment center)".
            
            font size 12 style bold colour 0x444444 "Times".
            print @ 50,685 width 520 alignment right "Rechtsbündige Zeile (alignment right)".
            
            font size 11 style regular colour black "Helvetica".
            print @ 50,655 width 520 alignment left "Tabelle: 6 Spalten x 6 Zeilen (gefüllt mit mehreren Zellen, inkl. Umbruch in Zellen).".
            
            table columns 6 rows 6 width 80,110,110,110,110,90 height 28,34,34,34,34,34 lines 0x000000 background 0xF3F3F3 thickness 1.
            
            font size 10 style bold colour 0x003366 "Helvetica".
            print @cell 0,0 width 74 alignment left "Nr".
            print @cell 1,0 width 104 alignment left "Name".
            print @cell 2,0 width 104 alignment left "Kategorie".
            print @cell 3,0 width 104 alignment left "Status".
            print @cell 4,0 width 104 alignment left "Kommentar".
            print @cell 5,0 width 84 alignment left "Score".
            
            font size 10 style regular colour black "Helvetica".
            print @cell 0,1 width 74 "1".
            print @cell 1,1 width 104 "Druck".
            print @cell 2,1 width 104 "Text".
            print @cell 3,1 width 104 "OK".
            print @cell 4,1 width 104 "Ein kurzer Hinweis.".
            print @cell 5,1 width 84 alignment right "95".
            
            print @cell 0,2 width 74 "2".
            print @cell 1,2 width 104 "Tabelle".
            print @cell 2,2 width 104 "Layout".
            print @cell 3,2 width 104 "OK".
            print @cell 4,2 width 104 "Mehrzeiliger Text in einer Tabellenzelle mit Umbruch.".
            print @cell 5,2 width 84 alignment right "88".
            
            print @cell 0,3 width 74 "3".
            print @cell 1,3 width 104 "Forms".
            print @cell 2,3 width 104 "UI".
            print @cell 3,3 width 104 "OK".
            print @cell 4,3 width 104 "Textfeld/Dropdown/Checkbox auf Seite 2.".
            print @cell 5,3 width 84 alignment right "91".
            
            print @cell 0,4 width 74 "4".
            print @cell 1,4 width 104 "Bild".
            print @cell 2,4 width 104 "Media".
            print @cell 3,4 width 104 "Optional".
            print @cell 4,4 width 104 "Wenn logo.png existiert, wird es geladen.".
            print @cell 5,4 width 84 alignment right "—".
            
            print @cell 0,5 width 74 "5".
            print @cell 1,5 width 104 "Ausrichtung".
            print @cell 2,5 width 104 "Text".
            print @cell 3,5 width 104 "OK".
            print @cell 4,5 width 104 "Left/Center/Right demonstriert.".
            print @cell 5,5 width 84 alignment right "90".
            
            font size 10 style italic colour 0x666666 "Helvetica".
            print @ 50,420 width 520 alignment left "Optional: Lege eine Datei 'logo.png' in den Projektordner, dann kannst du unten ein Bild einfügen.".
            image @ 50,380 size 160,60 "logo.png".
            
            nextpage.
            
            font size 16 style bold colour 0x003366 "Helvetica".
            print @ 50,810 width 520 "Formularelemente".
            
            font size 11 style regular colour black "Helvetica".
            print @ 50,785 width 520 "Die folgenden Felder sind interaktiv (je nach PDF-Viewer).".
            
            print @ 50,750 width 520 "Name:".
            control @ 50,732 type textbox content "Max Mustermann".
            
            print @ 50,700 width 520 "Studiengang:".
            control @ 50,682 type dropdown "Informatik;Wirtschaftsinformatik;Bauinformatik;Sonstiges" content "Informatik".
            
            print @ 50,650 width 520 "Einverstanden:".
            control @ 50,632 type option content "true".
            
            font size 10 style italic colour 0x666666 "Helvetica".
            print @ 50,600 width 520 "Hinweis: Manche Browser-PDF-Viewer zeigen Formulare nicht perfekt. Adobe Reader/Okular funktionieren meist besser.".
            
            nextpage.
            
            font size 16 style bold colour 0x003366 "Helvetica".
            print @ 50,810 width 520 "Mehrzeilen-Text (\\\\n)".
            
            font size 11 style regular colour black "Helvetica".
            print @ 50,785 width 520 "Hier wird explizit ein Zeilenumbruch per \\\\n erzwungen:".
            
            font size 12 style regular colour 0x222222 "Times".
            print @ 50,740 width 520 "Zeile 1\\\\nZeile 2\\\\nZeile 3".
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startUi);
    }

    private static void startUi() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame f = new JFrame("Scripting PDF");
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
                        "Linux: xdg-open {file}\nmacOS: open {file}\nWindows: cmd /c start \"\" \"{file}\"",
                current);
        if (input == null) return;

        String v = input.trim().isEmpty() ? defaultViewer() : input.trim();
        p.setProperty(PROP_VIEWER, v);
        saveProps(p);
        status.setText("Viewer gespeichert.");
    }

    private static void openPdf(Path pdf) {
        Properties p = loadProps();
        String cmd = p.getProperty(PROP_VIEWER, defaultViewer());

        // make Windows paths safe by default
        if (cmd.contains("{file}")) {
            String file = pdf.toAbsolutePath().toString();
            if (!cmd.contains("\"{file}\"") && file.contains(" ")) {
                cmd = cmd.replace("{file}", "\"" + file + "\"");
            } else {
                cmd = cmd.replace("{file}", file);
            }
        }

        try {
            new ProcessBuilder(cmd.split("\\s+")).inheritIO().start();
        } catch (Exception ignored) {}
    }

    private static Properties loadProps() {
        Properties p = new Properties();
        Path file = Paths.get(System.getProperty("user.home"), PROP_FILE);
        if (Files.exists(file)) {
            try (var in = Files.newInputStream(file)) { p.load(in); }
            catch (Exception ignored) {}
        }
        return p;
    }

    private static void saveProps(Properties p) {
        Path file = Paths.get(System.getProperty("user.home"), PROP_FILE);
        try (var out = Files.newOutputStream(file)) { p.store(out, "pdfscript settings"); }
        catch (Exception ignored) {}
    }

    private static String defaultViewer() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "cmd /c start \"\" \"{file}\"";
        if (os.contains("mac")) return "open {file}";
        return "xdg-open {file}";
    }
}
