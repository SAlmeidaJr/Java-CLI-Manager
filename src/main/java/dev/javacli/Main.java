package dev.javacli;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class Main {
    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        Path root = Path.of("").toAbsolutePath();
        boolean rootProvided = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> {
                        System.out.println("""
                                Java CLI Manager — crie arquivos Java no Zed

                                Uso: java -jar java-cli-manager.jar [--project-root PASTA]

                                A raiz padrão é a pasta atual. Requer terminal interativo.
                                ↑/↓ selecionam; Enter confirma; Esc cancela menus.
                                Ctrl+C cancela em qualquer etapa antes da criação.
                                """);
                        return 0;
                    }
                    case "--project-root" -> {
                        if (rootProvided || ++i >= args.length || args[i].isBlank()) {
                            throw new IllegalArgumentException("Informe --project-root PASTA uma única vez.");
                        }
                        root = Path.of(args[i]).toAbsolutePath().normalize();
                        rootProvided = true;
                    }
                    default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
                }
            }
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("A raiz do projeto deve ser uma pasta existente: " + root);
            }
            Path created;
            try (Terminal terminal = TerminalBuilder.builder().system(true).dumb(false).build()) {
                if (terminal.getType().startsWith("dumb")) {
                    throw new IOException("Execute em um terminal interativo, como o terminal integrado do Zed.");
                }
                TerminalUi ui = new TerminalUi(terminal);
                ui.message("Java CLI Manager · " + root);
                ui.message("Ctrl+C cancela antes da criação.\n");
                var kind = ui.select("Qual tipo deseja criar?", Arrays.asList(JavaFileGenerator.Kind.values()));
                ui.message("Tipo: " + kind);
                String destination = ui.select("Onde deseja criar?", List.of(
                        "Escolher pasta", "src/main/java + pacote", "src/test/java + pacote"));
                Path selectedDirectory = null;
                JavaFileGenerator.SourceSet sourceSet = null;
                String packageName;
                if (destination.equals("Escolher pasta")) {
                    selectedDirectory = FolderBrowser.choose(ui, root);
                    ui.message("Destino: " + selectedDirectory);
                    ui.message("O pacote altera apenas a declaração; o arquivo será criado nesta pasta.");
                    packageName = readValid(ui, "Pacote (editável; vazio para nenhum): ",
                            FolderBrowser.suggestedPackage(selectedDirectory),
                            JavaFileGenerator::validatePackage);
                } else {
                    sourceSet = destination.startsWith("src/main/")
                            ? JavaFileGenerator.SourceSet.MAIN : JavaFileGenerator.SourceSet.TEST;
                    ui.message("Destino: src/" + sourceSet + "/java");
                    packageName = readValid(ui, "Pacote (Enter para nenhum): ",
                            JavaFileGenerator::validatePackage);
                }
                JavaFileGenerator generator = new JavaFileGenerator();
                while (true) {
                    String name = readValid(ui, "Nome (sem .java): ", JavaFileGenerator::validateTypeName);
                    try {
                        created = selectedDirectory == null
                                ? generator.create(root, sourceSet, packageName, name, kind)
                                : generator.createInDirectory(selectedDirectory, packageName, name, kind);
                        break;
                    } catch (FileAlreadyExistsException e) {
                        ui.message("Esse arquivo já existe. Escolha outro nome ou use Ctrl+C para cancelar.");
                    }
                }
                ui.message("Criado: " + created);
            }
            openInZed(created);
            return 0;
        } catch (TerminalUi.Cancelled e) {
            System.out.println("Cancelado. Nenhum arquivo foi criado.");
            return 130;
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            System.err.println("Erro: " + e.getMessage());
            return 1;
        }
    }

    private static String readValid(TerminalUi ui, String prompt, Consumer<String> validator) {
        return readValid(ui, prompt, "", validator);
    }

    private static String readValid(TerminalUi ui, String prompt, String initial,
                                    Consumer<String> validator) {
        while (true) {
            String value = ui.read(prompt, initial);
            try {
                validator.accept(value);
                return value;
            } catch (IllegalArgumentException e) {
                ui.message(e.getMessage());
            }
        }
    }

    private static void openInZed(Path file) {
        try {
            Process process = new ProcessBuilder("zed", "--existing", file.toString()).inheritIO().start();
            if (process.waitFor() != 0) {
                System.err.println("Não foi possível abrir no Zed. O arquivo foi preservado: " + file);
            }
        } catch (IOException e) {
            System.err.println("Não foi possível executar zed. Abra manualmente: " + file);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("A abertura foi interrompida. O arquivo foi preservado: " + file);
        }
    }
}
