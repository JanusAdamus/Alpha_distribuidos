package mx.unam.alpha.common.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static void writeHeaderIfNeeded(Path path, String header) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.writeString(path, header + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void appendRow(Path path, List<String> columns) {
        String row = String.join(",", columns) + System.lineSeparator();
        try {
            Files.writeString(path, row, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
