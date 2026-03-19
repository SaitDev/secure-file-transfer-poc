package pes.poc.a.response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pes.poc.a.persistence.entity.TransferFileEntity;
import pes.poc.a.persistence.repository.TransferFileRepository;
import pes.poc.a.persistence.service.SystemATransferTrackingService;
import pes.poc.a.scheduling.SystemAProperties;

@Service
@RequiredArgsConstructor
public class SystemAResponsePollingService {

    private static final Logger log = LoggerFactory.getLogger(SystemAResponsePollingService.class);

    private final SystemAProperties systemAProperties;
    private final TransferFileRepository transferFileRepository;
    private final SystemATransferTrackingService systemATransferTrackingService;
    private final SystemAResponseSftpClient systemAResponseSftpClient;
    private final ResponseDownloadAckFileContentFactory responseDownloadAckFileContentFactory;

    public int pollResponseFiles() {
        List<InboundResponseFile> responseFiles = systemAResponseSftpClient.listResponseFiles();
        int processedCount = 0;

        for (InboundResponseFile responseFile : responseFiles) {
            if (downloadAndAcknowledge(responseFile)) {
                processedCount++;
            }
        }

        if (processedCount > 0) {
            log.info("Processed {} response file(s) during the current polling run", processedCount);
        }
        return processedCount;
    }

    private boolean downloadAndAcknowledge(InboundResponseFile responseFile) {
        TransferFileEntity transferFile = transferFileRepository.findByFileId(responseFile.fileName().fileId())
                .orElse(null);

        if (transferFile == null) {
            log.warn(
                    "Skipping response file {} because no outbound transfer was found for fileId {}",
                    responseFile.fileName().fileName(),
                    responseFile.fileName().fileId()
            );
            return false;
        }

        if (transferFile.getResponseDownloadAckSentAt() != null) {
            return false;
        }

        Path responseDirectory = Path.of(systemAProperties.getResponseDirectory());
        Path localFile = responseDirectory.resolve(responseFile.fileName().fileName());
        Path tempFile = responseDirectory.resolve(responseFile.fileName().fileName() + ".part");

        try {
            if (transferFile.getResponseDownloadedAt() == null) {
                Files.createDirectories(responseDirectory);
                Files.deleteIfExists(tempFile);
                systemAResponseSftpClient.download(responseFile.remotePath(), tempFile);
                Files.move(tempFile, localFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                systemATransferTrackingService.markResponseDownloaded(
                        transferFile.getId(),
                        responseFile.fileName().fileName(),
                        now()
                );
            }

            String ackContent = responseDownloadAckFileContentFactory.create(responseFile.fileName(), ZonedDateTime.now(zoneId()));
            systemAResponseSftpClient.uploadDownloadAck(responseFile.fileName().downloadAckFileName(), ackContent);
            systemATransferTrackingService.markResponseDownloadAcknowledged(transferFile.getId(), now());
            log.info(
                    "Downloaded response file {} for batchId={} fileId={} and uploaded ACK {}",
                    responseFile.fileName().fileName(),
                    transferFile.getBatchId(),
                    transferFile.getFileId(),
                    responseFile.fileName().downloadAckFileName()
            );
            return true;
        } catch (IOException exception) {
            systemATransferTrackingService.markError(
                    transferFile.getId(),
                    "SYSTEM_A_RESPONSE_STAGING_FAILED",
                    exception.getMessage()
            );
            throw new IllegalStateException(
                    "Failed to stage response file " + responseFile.fileName().fileName(),
                    exception
            );
        } catch (RuntimeException exception) {
            systemATransferTrackingService.markError(
                    transferFile.getId(),
                    "SYSTEM_A_RESPONSE_POLL_FAILED",
                    exception.getMessage()
            );
            throw exception;
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Best effort cleanup for the local temp response download.
            }
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(zoneId());
    }

    private ZoneId zoneId() {
        return ZoneId.of(systemAProperties.getZone());
    }
}
