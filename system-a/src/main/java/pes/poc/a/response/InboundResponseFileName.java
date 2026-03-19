package pes.poc.a.response;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record InboundResponseFileName(
        String fileId,
        String fileTimestamp,
        String fileName,
        String downloadAckFileName,
        boolean encrypted
) {

    private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("^customer_([A-Fa-f0-9]{32})_(\\d{14})\\.result\\.ack\\.pgp$");
    private static final Pattern PLAIN_PATTERN = Pattern.compile("^customer_([A-Fa-f0-9]{32})_(\\d{14})\\.result\\.ack$");

    public static Optional<InboundResponseFileName> parse(String rawFileName) {
        Matcher encryptedMatcher = ENCRYPTED_PATTERN.matcher(rawFileName);
        if (encryptedMatcher.matches()) {
            return Optional.of(new InboundResponseFileName(
                    encryptedMatcher.group(1),
                    encryptedMatcher.group(2),
                    rawFileName,
                    rawFileName.replace(".result.ack.pgp", ".download.ack"),
                    true
            ));
        }

        Matcher plainMatcher = PLAIN_PATTERN.matcher(rawFileName);
        if (plainMatcher.matches()) {
            return Optional.of(new InboundResponseFileName(
                    plainMatcher.group(1),
                    plainMatcher.group(2),
                    rawFileName,
                    rawFileName.replace(".result.ack", ".download.ack"),
                    false
            ));
        }

        return Optional.empty();
    }
}
