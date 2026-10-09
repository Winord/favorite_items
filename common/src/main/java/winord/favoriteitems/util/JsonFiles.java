package winord.favoriteitems.util;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonFiles {
    private JsonFiles() { }

    public static void writeAtomically(Gson gson, Path target, Object data) throws IOException {
        Files.createDirectories(target.getParent());

        var temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
