package dev.javacli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import javax.lang.model.SourceVersion;

public final class JavaFileGenerator {
    private static final Set<String> RESTRICTED_TYPE_NAMES =
            Set.of("var", "yield", "record", "sealed", "permits");

    public enum Kind {
        CLASS("Class", "class"), RECORD("Record", "record"),
        ENUM("Enum", "enum"), INTERFACE("Interface", "interface");

        private final String label;
        private final String keyword;

        Kind(String label, String keyword) {
            this.label = label;
            this.keyword = keyword;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum SourceSet {
        MAIN("main"), TEST("test");

        private final String directory;

        SourceSet(String directory) {
            this.directory = directory;
        }

        @Override
        public String toString() {
            return directory;
        }
    }

    public static void validatePackage(String packageName) {
        if (packageName.isEmpty()) {
            return;
        }
        for (String segment : packageName.split("\\.", -1)) {
            if (!validIdentifier(segment)) {
                throw new IllegalArgumentException(
                        "Pacote inválido. Use nomes Java separados por pontos, como com.exemplo.model.");
            }
        }
    }

    public static void validateTypeName(String name) {
        if (!validIdentifier(name) || RESTRICTED_TYPE_NAMES.contains(name)) {
            throw new IllegalArgumentException(
                    "Nome inválido. Informe um identificador Java, sem .java ou palavras reservadas.");
        }
    }

    private static boolean validIdentifier(String name) {
        // Reject identifier-ignorable controls as well as separators and keywords.
        return SourceVersion.isIdentifier(name)
                && !SourceVersion.isKeyword(name, SourceVersion.RELEASE_21)
                && name.codePoints().noneMatch(Character::isIdentifierIgnorable);
    }

    public Path create(Path projectRoot, SourceSet sourceSet, String packageName,
                       String name, Kind kind) throws IOException {
        validatePackage(packageName);
        validateTypeName(name);
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("A raiz do projeto deve ser uma pasta existente: " + root);
        }
        Path directory = root.resolve("src").resolve(sourceSet.directory).resolve("java");
        if (!packageName.isEmpty()) {
            for (String segment : packageName.split("\\.")) {
                directory = directory.resolve(segment);
            }
        }
        try {
            Files.createDirectories(directory);
        } catch (FileAlreadyExistsException e) {
            // A file blocking a package directory cannot be fixed by choosing another type name.
            throw new IOException("Um arquivo impede a criação da pasta: " + directory, e);
        }
        return createInDirectory(directory, packageName, name, kind);
    }

    public Path createInDirectory(Path directory, String packageName,
                                  String name, Kind kind) throws IOException {
        validatePackage(packageName);
        validateTypeName(name);
        directory = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IOException("A pasta de destino não existe: " + directory);
        }
        String source = (packageName.isEmpty() ? "" : "package " + packageName + ";\n\n")
                + "public " + kind.keyword + " " + name
                + (kind == Kind.RECORD ? "()" : "") + " {\n}\n";
        Path file = directory.resolve(name + ".java");
        // CREATE_NEW is atomic: even a concurrent invocation cannot overwrite this file.
        Files.writeString(file, source, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return file;
    }
}
