package pes.poc.b.transfer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record InboundTransferFileName(
        String flow,
        String fileId,
        String fileTimestamp,
        String fileName,
        String downloadAckFileName
) {

    private static final Pattern CURRENT_PATTERN = Pattern.compile(
            "^customer_([A-Fa-f0-9]{32})_(\\d{14})\\.csv\\.pgp$"
    );

    public static Optional<InboundTransferFileName> parse(String rawFileName) {
        Matcher currentMatcher = CURRENT_PATTERN.matcher(rawFileName);
        if (currentMatcher.matches()) {
            return Optional.of(new InboundTransferFileName(
                    "A2B",
                    currentMatcher.group(1),
                    currentMatcher.group(2),
                    rawFileName,
                    rawFileName.replace(".csv.pgp", ".download.ack")
            ));
        }

        return Optional.empty();
    }

    public String resultAckFileName() {
        return fileName.replace(".csv.pgp", ".result.ack");
    }
}
