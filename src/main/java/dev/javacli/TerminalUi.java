package dev.javacli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;

final class TerminalUi {
    static final class Cancelled extends RuntimeException {}

    private enum Key { UP, DOWN, ENTER, CANCEL, IGNORE }

    private final Terminal terminal;
    private final LineReader lineReader;
    private final BindingReader bindings;

    TerminalUi(Terminal terminal) {
        this.terminal = terminal;
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        this.bindings = new BindingReader(terminal.reader());
    }

    <T> T select(String title, List<T> options) throws IOException {
        KeyMap<Key> keys = new KeyMap<>();
        keys.setNomatch(Key.IGNORE);
        keys.setUnicode(Key.IGNORE);
        keys.setAmbiguousTimeout(150);
        keys.bind(Key.UP, "\033[A", "\033OA");
        keys.bind(Key.DOWN, "\033[B", "\033OB");
        bindCapability(keys, Key.UP, Capability.key_up);
        bindCapability(keys, Key.DOWN, Capability.key_down);
        keys.bind(Key.ENTER, "\r", "\n");
        keys.bind(Key.CANCEL, "\033", "\003", "\004");

        Attributes original = terminal.enterRawMode();
        Attributes menuAttributes = terminal.getAttributes();
        menuAttributes.setLocalFlag(Attributes.LocalFlag.ISIG, false);
        terminal.setAttributes(menuAttributes);
        Display display = new Display(terminal, false);
        int selected = 0;
        terminal.puts(Capability.keypad_xmit);
        try {
            while (true) {
                List<AttributedString> lines = new ArrayList<>();
                int width = terminal.getWidth() > 0 ? terminal.getWidth() : 80;
                int height = terminal.getHeight() > 0 ? terminal.getHeight() : 24;
                int visible = Math.max(1, height - 4);
                int first = Math.max(0, Math.min(selected - visible / 2, options.size() - visible));
                lines.add(new AttributedString(title.replaceAll("[\\p{Cntrl}]", "?")).columnSubSequence(0, width));
                for (int i = first; i < Math.min(options.size(), first + visible); i++) {
                    lines.add(new AttributedStringBuilder()
                            .style(i == selected
                                    ? AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN)
                                    : AttributedStyle.DEFAULT)
                            .append(i == selected ? "❯ " : "  ")
                            .append(options.get(i).toString().replaceAll("[\\p{Cntrl}]", "?"))
                            .toAttributedString().columnSubSequence(0, width));
                }
                lines.add(new AttributedString("↑ ↓ navegar · Enter selecionar · Esc cancelar")
                        .columnSubSequence(0, width));
                display.resize(height, width);
                display.update(lines, 0);
                terminal.flush();
                Key key = bindings.readBinding(keys);
                if (key == null || key == Key.CANCEL) {
                    throw new Cancelled();
                }
                switch (key) {
                    case UP -> selected = Math.floorMod(selected - 1, options.size());
                    case DOWN -> selected = (selected + 1) % options.size();
                    case ENTER -> {
                        return options.get(selected);
                    }
                    default -> { }
                }
            }
        } finally {
            try {
                display.update(List.of(), 0);
                terminal.puts(Capability.keypad_local);
                terminal.flush();
            } finally {
                terminal.setAttributes(original);
            }
        }
    }

    String read(String prompt) {
        return read(prompt, "");
    }

    String read(String prompt, String initial) {
        try {
            return lineReader.readLine(prompt, null, initial).strip();
        } catch (UserInterruptException | EndOfFileException e) {
            throw new Cancelled();
        }
    }

    void message(String message) {
        terminal.writer().println(message);
        terminal.flush();
    }

    private void bindCapability(KeyMap<Key> keys, Key key, Capability capability) {
        String sequence = KeyMap.key(terminal, capability);
        if (sequence != null) {
            keys.bind(key, sequence);
        }
    }
}
