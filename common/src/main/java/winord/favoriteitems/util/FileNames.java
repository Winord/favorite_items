package winord.favoriteitems.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class FileNames {
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");
    private static final Pattern WINDOWS_RESERVED = Pattern.compile("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$");

    private FileNames() { }

    public static String sanitize(String raw, String fallback) {
        var cleaned = ILLEGAL_CHARS.matcher(raw.trim()).replaceAll("_");
        cleaned = cleaned.replaceAll("[ .]+$", "");
        if (cleaned.isEmpty()) cleaned = fallback;
        if (WINDOWS_RESERVED.matcher(cleaned).matches()) cleaned = "_" + cleaned;
        return cleaned;
    }

    public static String truncateUtf8(String text, int maxBytes) {
        var bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return text;

        var decoder = StandardCharsets.UTF_8.newDecoder();
        for (int end = maxBytes; end > 0; end--) {
            try {
                return decoder.decode(ByteBuffer.wrap(bytes, 0, end)).toString();
            } catch (CharacterCodingException ignored) {
            }
        }
        return "_";
    }
}
