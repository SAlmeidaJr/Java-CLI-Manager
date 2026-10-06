package dev.javacli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FolderBrowser {
    record Entry(String label, Path path, boolean choose) {
        @Override public String toString() { return label; }
    }

    static List<Entry> entries(Path directory) throws IOException {
        Path current = directory.toAbsolutePath().normalize();
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("Criar nesta pasta", current, true));
        if (current.getParent() != null) {
            entries.add(new Entry(".. (voltar)", current.getParent(), false));
        }
        try (var children = Files.list(current)) {
            children.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> entries.add(new Entry(path.getFileName() + "/", path, false)));
        }
        return entries;
    }

    static Path choose(TerminalUi ui, Path start) throws IOException {
        Path current = start.toAbsolutePath().normalize();
        List<Entry> options = entries(current);
        while (true) {
            Entry entry = ui.select("Pasta: " + current, options);
            if (entry.choose()) {
                return current;
            }
            try {
                List<Entry> next = entries(entry.path());
                current = entry.path();
                options = next;
            } catch (IOException e) {
                ui.message("Não foi possível abrir a pasta: " + entry.path());
            }
        }
    }

    static String suggestedPackage(Path directory) {
        Path current = directory.toAbsolutePath().normalize();
        for (Path ancestor = current; ancestor != null; ancestor = ancestor.getParent()) {
            Path parent = ancestor.getParent();
            if (ancestor.getFileName() == null || !ancestor.getFileName().toString().equals("java")
                    || parent == null || parent.getFileName() == null
                    || !(parent.getFileName().toString().equals("main")
                    || parent.getFileName().toString().equals("test"))
                    || parent.getParent() == null || parent.getParent().getFileName() == null
                    || !parent.getParent().getFileName().toString().equals("src")) {
                continue;
            }
            String name = ancestor.relativize(current).toString().replace(
                    current.getFileSystem().getSeparator(), ".");
            try {
                JavaFileGenerator.validatePackage(name);
                return name;
            } catch (IllegalArgumentException e) {
                return "";
            }
        }
        return "";
    }
}
