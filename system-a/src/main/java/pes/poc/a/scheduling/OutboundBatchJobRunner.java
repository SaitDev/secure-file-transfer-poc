package pes.poc.a.scheduling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pes.poc.a.crypto.SystemAPgpEncryptionService;
import pes.poc.a.csv.MockCustomerCsvDataService;
import pes.poc.a.persistence.service.SystemATransferTrackingService;
import pes.poc.a.persistence.service.TransferBatchScheduleResolver;
import pes.poc.a.sftp.SystemASftpUploadService;

@Service
@RequiredArgsConstructor
public class OutboundBatchJobRunner {

    private static final Logger log = LoggerFactory.getLogger(OutboundBatchJobRunner.class);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MockCustomerCsvDataService mockCustomerCsvDataService;
    private final SystemAProperties systemAProperties;
    private final SystemAPgpEncryptionService systemAPgpEncryptionService;
    private final SystemASftpUploadService systemASftpUploadService;
    private final SystemATransferTrackingService systemATransferTrackingService;

    public OutboundBatchExecution runBatch(LocalDateTime triggeredAt) {
        int batchSize = systemAProperties.getBatchSize();
        validateBatchSize(batchSize);

        ZoneId zoneId = ZoneId.of(systemAProperties.getZone());
        TransferBatchScheduleResolver.ResolvedTransferBatch batchSchedule = TransferBatchScheduleResolver.resolve(triggeredAt);
        String batchId = UUID.randomUUID().toString().replace("-", "");
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String baseFileName = buildBaseFileName(fileId, triggeredAt);
        String csvPayload = mockCustomerCsvDataService.generateCsv(batchSize);
        Path stagingDirectory = Path.of(systemAProperties.getStagingDirectory());
        Path plainCsvFile = stagingDirectory.resolve(baseFileName + ".csv");
        Path encryptedFile = stagingDirectory.resolve(baseFileName + ".csv.pgp");
        UUID transferId = systemATransferTrackingService.recordCreated(
                batchId,
                fileId,
                batchSize,
                encryptedFile,
                batchSchedule,
                triggeredAt.atZone(zoneId).toOffsetDateTime()
        );
        String errorCode = "OUTBOUND_FILE_STAGING_FAILED";

        try {
            Files.createDirectories(stagingDirectory);
            Files.writeString(plainCsvFile, csvPayload, StandardCharsets.UTF_8);
            errorCode = "OUTBOUND_FILE_ENCRYPT_FAILED";
            systemAPgpEncryptionService.encrypt(plainCsvFile, encryptedFile);
            systemATransferTrackingService.markEncrypted(
                    transferId,
                    encryptedFile,
                    Files.size(encryptedFile),
                    now(zoneId)
            );
            deletePlainCsvFile(plainCsvFile);

            errorCode = "OUTBOUND_FILE_UPLOAD_FAILED";
            String remoteFile = systemASftpUploadService.uploadOutboundFile(encryptedFile);
            systemATransferTrackingService.markUploaded(transferId, remoteFile, now(zoneId));
            log.info(
                    "Uploaded outbound file for batchId={} fileId={} to {} with {} mock customer records",
                    batchId,
                    fileId,
                    remoteFile,
                    batchSize
            );
            return new OutboundBatchExecution(batchId, fileId, encryptedFile, remoteFile, batchSize);
        } catch (IOException exception) {
            systemATransferTrackingService.markError(transferId, errorCode, exception.getMessage());
            throw new IllegalStateException("Failed to stage outbound CSV file at " + plainCsvFile, exception);
        } catch (RuntimeException exception) {
            systemATransferTrackingService.markError(transferId, errorCode, exception.getMessage());
            throw exception;
        } finally {
            if (Objects.equals(errorCode, "OUTBOUND_FILE_ENCRYPT_FAILED")) {
                deletePlainCsvFile(plainCsvFile);
            }
        }
    }

    private void validateBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalStateException("system-a.batch-size must be greater than 0");
        }
    }

    private void deletePlainCsvFile(Path plainCsvFile) {
        try {
            Files.deleteIfExists(plainCsvFile);
        } catch (IOException exception) {
            log.warn("Failed to delete plaintext CSV file {}", plainCsvFile, exception);
        }
    }

    private String buildBaseFileName(String fileId, LocalDateTime triggeredAt) {
        return "customer_%s_%s".formatted(
                fileId,
                triggeredAt.format(FILE_TIMESTAMP)
        );
    }

    private OffsetDateTime now(ZoneId zoneId) {
        return OffsetDateTime.now(zoneId);
    }
}
