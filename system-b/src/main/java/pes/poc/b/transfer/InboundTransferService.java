package pes.poc.b.transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pes.poc.b.config.SystemBProperties;
import pes.poc.b.persistence.service.InboundTransferPersistenceService;
import pes.poc.b.persistence.service.PersistedInboundTransfer;
import pes.poc.b.sftp.SystemBSftpClient;

@Service
@RequiredArgsConstructor
public class InboundTransferService {

    private static final Logger log = LoggerFactory.getLogger(InboundTransferService.class);

    private final SystemBProperties systemBProperties;
    private final SystemBSftpClient systemBSftpClient;
    private final DownloadAckFileContentFactory downloadAckFileContentFactory;
    private final Sha256ChecksumService sha256ChecksumService;
    private final TransferReceiptStore transferReceiptStore;
    private final InboundTransferPersistenceService inboundTransferPersistenceService;

    public int pollInboundFiles() {
        List<InboundTransferFile> inboundFiles = systemBSftpClient.listInboundFiles();
        int processedCount = 0;

        for (InboundTransferFile inboundFile : inboundFiles) {
            if (transferReceiptStore.hasAcknowledged(inboundFile.fileName().fileId())) {
                log.info(
                        "Skipping inbound file {} because file ID {} has already been acknowledged locally",
                        inboundFile.fileName().fileName(),
                        inboundFile.fileName().fileId()
                );
                continue;
            }

            downloadAndAcknowledge(inboundFile);
            processedCount++;
        }

        if (processedCount > 0) {
            log.info("Processed {} inbound file(s) during the current polling run", processedCount);
        }
        return processedCount;
    }

    private void downloadAndAcknowledge(InboundTransferFile inboundFile) {
        Path stagingDirectory = Path.of(systemBProperties.getStagingDirectory());
        Path localFile = stagingDirectory.resolve(inboundFile.fileName().fileName());
        Path tempFile = stagingDirectory.resolve(inboundFile.fileName().fileName() + ".part");
        PersistedInboundTransfer persistedTransfer = null;

        try {
            Files.createDirectories(stagingDirectory);
            Files.deleteIfExists(tempFile);
            systemBSftpClient.download(inboundFile.remotePath(), tempFile);
            Files.move(tempFile, localFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            String sha256 = sha256ChecksumService.calculate(localFile);
            ZonedDateTime downloadedAt = ZonedDateTime.now(ZoneId.of(systemBProperties.getZone()));
            String ackContent = downloadAckFileContentFactory.create(inboundFile.fileName(), downloadedAt, sha256);
            String ackFileName = inboundFile.fileName().downloadAckFileName();
            persistedTransfer = inboundTransferPersistenceService.recordDownloadedTransferAndAcknowledge(
                    inboundFile,
                    localFile,
                    sha256,
                    downloadedAt,
                    ackFileName,
                    ZonedDateTime.now(ZoneId.of(systemBProperties.getZone())),
                    () -> systemBSftpClient.uploadAckFile(ackFileName, ackContent)
            );
            transferReceiptStore.markAcknowledged(inboundFile.fileName(), localFile, downloadedAt, sha256);

            log.info(
                    "Downloaded inbound file {} to {} and uploaded ACK {}",
                    inboundFile.fileName().fileName(),
                    localFile,
                    ackFileName
            );
        } catch (IOException exception) {
            markTransferErrorIfPersisted(persistedTransfer, "INBOUND_FILE_STAGING_FAILED", exception, inboundFile);
            throw new IllegalStateException(
                    "Failed to stage inbound file " + inboundFile.fileName().fileName(),
                    exception
            );
        } catch (RuntimeException exception) {
            markTransferErrorIfPersisted(persistedTransfer, "INBOUND_TRANSFER_FAILED", exception, inboundFile);
            throw exception;
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Best effort cleanup for the local temp download.
            }
        }
    }

    private void markTransferErrorIfPersisted(
            PersistedInboundTransfer persistedTransfer,
            String errorCode,
            Exception exception,
            InboundTransferFile inboundFile
    ) {
        if (persistedTransfer == null) {
            log.warn(
                    "Inbound file {} failed before a DB transfer record was created: {}",
                    inboundFile.fileName().fileName(),
                    exception.getMessage()
            );
            return;
        }

        inboundTransferPersistenceService.markTransferError(
                persistedTransfer.transferId(),
                errorCode,
                exception.getMessage(),
                ZonedDateTime.now(ZoneId.of(systemBProperties.getZone()))
        );
    }
}
