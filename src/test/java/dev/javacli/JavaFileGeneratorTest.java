package dev.javacli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class JavaFileGeneratorTest {
    @TempDir Path temporary;
    private final JavaFileGenerator generator = new JavaFileGenerator();

    @ParameterizedTest
    @EnumSource(JavaFileGenerator.Kind.class)
    void generatesCompilableTypesInBothSourceSets(JavaFileGenerator.Kind kind) throws Exception {
        Path root = Files.createDirectory(temporary.resolve("projeto com espaços"));
        List<String> sources = new ArrayList<>();
        for (var sourceSet : JavaFileGenerator.SourceSet.values()) {
            String name = "Pessoa" + kind + sourceSet;
            Path file = generator.create(root, sourceSet, "com.exemplo.model", name, kind);
            assertEquals(root.resolve("src/" + sourceSet + "/java/com/exemplo/model/" + name + ".java"), file);
            String source = Files.readString(file);
            assertTrue(source.startsWith("package com.exemplo.model;\n\npublic "));
            if (kind == JavaFileGenerator.Kind.RECORD) {
                assertTrue(source.contains(name + "()"));
            }
            sources.add(file.toString());
        }
        compile(sources);
    }

    @Test
    void supportsDefaultPackageAndUnicodeIdentifier() throws Exception {
        Path file = generator.create(temporary, JavaFileGenerator.SourceSet.MAIN,
                "", "Ação", JavaFileGenerator.Kind.CLASS);
        assertEquals(temporary.resolve("src/main/java/Ação.java"), file);
        assertFalse(Files.readString(file).contains("package"));
        compile(List.of(file.toString()));
    }

    @Test
    void allowsContextualWordsInPackageNames() throws Exception {
        Path file = generator.create(temporary, JavaFileGenerator.SourceSet.TEST,
                "com.record.var", "Modelo", JavaFileGenerator.Kind.CLASS);
        compile(List.of(file.toString()));
    }

    @Test
    void existingFilesAreNeverOverwritten() throws Exception {
        Path file = generator.create(temporary, JavaFileGenerator.SourceSet.MAIN,
                "com.exemplo", "Pessoa", JavaFileGenerator.Kind.CLASS);
        Files.writeString(file, "// conteúdo do usuário\n");
        assertThrows(FileAlreadyExistsException.class, () -> generator.create(temporary,
                JavaFileGenerator.SourceSet.MAIN, "com.exemplo", "Pessoa", JavaFileGenerator.Kind.RECORD));
        assertEquals("// conteúdo do usuário\n", Files.readString(file));
    }

    @Test
    void fileBlockingPackageDirectoryIsReportedAsIoError() throws Exception {
        Path sources = Files.createDirectories(temporary.resolve("src/main/java"));
        Path blocker = sources.resolve("com");
        Files.writeString(blocker, "preservar");
        IOException failure = assertThrows(IOException.class, () -> generator.create(temporary,
                JavaFileGenerator.SourceSet.MAIN, "com", "Pessoa", JavaFileGenerator.Kind.CLASS));
        assertFalse(failure instanceof FileAlreadyExistsException);
        assertEquals("preservar", Files.readString(blocker));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "class", "true", "null", "_", "record", "var", "yield", "sealed",
            "permits", "9Pessoa", "Pessoa.java", "../Pessoa", "a/b", "a\\b", "Pessoa Nome", "a\u0000b"})
    void invalidTypeNamesDoNotCreateDirectories(String name) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> generator.create(temporary,
                JavaFileGenerator.SourceSet.MAIN, "com.exemplo", name, JavaFileGenerator.Kind.CLASS));
        assertEmptyRoot();
    }

    @ParameterizedTest
    @ValueSource(strings = {".com", "com.", "com..exemplo", "com.class", "com.null", "com._",
            "../fora", "com/exemplo", "com\\exemplo", "com.9exemplo", "com.ex emplo"})
    void invalidPackagesDoNotCreateDirectories(String packageName) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> generator.create(temporary,
                JavaFileGenerator.SourceSet.MAIN, packageName, "Pessoa", JavaFileGenerator.Kind.CLASS));
        assertEmptyRoot();
    }

    @Test
    void requiresAnExistingProjectDirectory() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> generator.create(temporary.resolve("missing"),
                JavaFileGenerator.SourceSet.MAIN, "", "Pessoa", JavaFileGenerator.Kind.CLASS));
        assertEmptyRoot();
    }

    private void assertEmptyRoot() throws Exception {
        try (Stream<Path> files = Files.list(temporary)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void writesDirectlyInSelectedFolderWithoutDuplicatingPackageDirectories() throws Exception {
        Path folder = Files.createDirectories(temporary.resolve("custom folder/com/exemplo"));
        Path file = generator.createInDirectory(folder, "com.exemplo", "Pessoa",
                JavaFileGenerator.Kind.RECORD);
        assertEquals(folder.resolve("Pessoa.java"), file);
        assertFalse(Files.exists(folder.resolve("com")));
        compile(List.of(file.toString()));
        assertThrows(FileAlreadyExistsException.class, () -> generator.createInDirectory(
                folder, "com.exemplo", "Pessoa", JavaFileGenerator.Kind.CLASS));
    }

    @Test
    void selectedFolderMustExistAndNamesMustBeValid() throws Exception {
        assertThrows(IOException.class, () -> generator.createInDirectory(temporary.resolve("missing"),
                "", "Pessoa", JavaFileGenerator.Kind.CLASS));
        assertThrows(IllegalArgumentException.class, () -> generator.createInDirectory(temporary,
                "", "../Pessoa", JavaFileGenerator.Kind.CLASS));
        assertEmptyRoot();
    }

    private void compile(List<String> sources) throws Exception {
        Path output = Files.createDirectories(temporary.resolve("classes"));
        List<String> args = new ArrayList<>(List.of("--release", "21", "-encoding", "UTF-8",
                "-d", output.toString()));
        args.addAll(sources);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics,
                args.toArray(String[]::new));
        assertEquals(0, result, diagnostics.toString(StandardCharsets.UTF_8));
    }
}
