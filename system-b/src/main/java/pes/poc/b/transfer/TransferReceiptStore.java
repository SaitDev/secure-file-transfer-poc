package pes.poc.b.transfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pes.poc.b.config.SystemBProperties;

@Component
@RequiredArgsConstructor
public class TransferReceiptStore {

    private final SystemBProperties systemBProperties;

    public boolean hasAcknowledged(String fileId) {
        return Files.exists(receiptPath(fileId));
    }

    public void markAcknowledged(
            InboundTransferFileName fileName,
            Path localFile,
            ZonedDateTime downloadedAt,
            String sha256
    ) {
        Path receiptPath = receiptPath(fileName.fileId());
        String receiptContent = """
                fileId=%s
                sourceFile=%s
                downloadAckFile=%s
                localFile=%s
                downloadedAt=%s
                sha256=%s
                """.formatted(
                fileName.fileId(),
                fileName.fileName(),
                fileName.downloadAckFileName(),
                localFile.toAbsolutePath(),
                downloadedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                sha256
        );

        try {
            Files.createDirectories(receiptPath.getParent());
            Files.writeString(receiptPath, receiptContent, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist inbound receipt marker for " + fileName.fileName(), exception);
        }
    }

    private Path receiptPath(String fileId) {
        return Path.of(systemBProperties.getReceiptDirectory()).resolve(sanitize(fileId) + ".receipt");
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
