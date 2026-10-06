package dev.javacli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderBrowserTest {
    @TempDir Path root;

    @Test
    void listsDirectoriesInOrderWithChooseAndParentActions() throws Exception {
        Files.createDirectory(root.resolve("z pasta"));
        Files.createDirectory(root.resolve("a pasta"));
        Files.writeString(root.resolve("arquivo.txt"), "");
        var entries = FolderBrowser.entries(root);
        assertEquals(List.of("Criar nesta pasta", ".. (voltar)", "a pasta/", "z pasta/"),
                entries.stream().map(FolderBrowser.Entry::label).toList());
        assertTrue(entries.getFirst().choose());
        assertEquals(root, entries.getFirst().path());
        assertEquals(root.getParent(), entries.get(1).path());
        assertFalse(entries.get(2).choose());
        assertEquals(root.resolve("a pasta"), entries.get(2).path());
    }

    @Test
    void emptyDirectoryCanBeSelected() throws Exception {
        assertTrue(FolderBrowser.entries(root).getFirst().choose());
        assertEquals(2, FolderBrowser.entries(root).size());
    }

    @Test
    void suggestsPackagesForBothSourceSetsAndNestedModules() {
        for (String sourceSet : List.of("main", "test")) {
            Path sources = root.resolve("module/src/" + sourceSet + "/java");
            assertEquals("com.exemplo", FolderBrowser.suggestedPackage(sources.resolve("com/exemplo")));
            assertEquals("", FolderBrowser.suggestedPackage(sources));
        }
    }

    @Test
    void unknownLayoutsAndInvalidNamesHaveNoSuggestion() {
        assertEquals("", FolderBrowser.suggestedPackage(root.resolve("custom/com/exemplo")));
        assertEquals("", FolderBrowser.suggestedPackage(root.resolve("src/main/java/invalid-name")));
        assertEquals("", FolderBrowser.suggestedPackage(root.resolve("src/main/java/com/class")));
    }
}
