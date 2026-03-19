package pes.poc.b.persistence.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pes.poc.b.persistence.entity.TransferBatchEntity;
import pes.poc.b.persistence.entity.TransferEventEntity;
import pes.poc.b.persistence.entity.TransferFileEntity;
import pes.poc.b.persistence.entity.TransferPayloadEntity;
import pes.poc.b.persistence.repository.TransferBatchRepository;
import pes.poc.b.persistence.repository.TransferEventRepository;
import pes.poc.b.persistence.repository.TransferFileRepository;
import pes.poc.b.persistence.repository.TransferPayloadRepository;
import pes.poc.b.transfer.InboundTransferFile;

@Service
@RequiredArgsConstructor
public class InboundTransferPersistenceService {

    private static final short DEFAULT_BATCH_NUMBER = 0;
    private static final String STORAGE_BACKEND = "SFTPGO";
    private static final String FLOW_A2B = "A2B";
    private static final String STATUS_DOWNLOADED = "DOWNLOADED";
    private static final String STATUS_DOWNLOAD_ACK_SENT = "DOWNLOAD_ACK_SENT";
    private static final String STATUS_ERROR = "ERROR";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_PROCESSED_SUCCESS = "PROCESSED_SUCCESS";
    private static final String STATUS_PROCESSED_FAILED = "PROCESSED_FAILED";
    private static final String STATUS_RESPONSE_UPLOADED = "RESPONSE_UPLOADED";
    private static final String STATUS_RESPONSE_UPLOAD_FAILED = "RESPONSE_UPLOAD_FAILED";
    private static final String PROCESSING_PENDING = "PENDING";

    private final TransferBatchRepository transferBatchRepository;
    private final TransferFileRepository transferFileRepository;
    private final TransferPayloadRepository transferPayloadRepository;
    private final TransferEventRepository transferEventRepository;

    @Transactional(rollbackFor = Exception.class)
    public PersistedInboundTransfer recordDownloadedTransfer(
            InboundTransferFile inboundFile,
            Path localFile,
            String sourceChecksum,
            ZonedDateTime downloadedAt
    ) throws IOException {
        OffsetDateTime eventTime = downloadedAt.toOffsetDateTime();
        TransferBatchEntity batch = findOrCreateBatch(downloadedAt.toLocalDate(), eventTime);
        TransferFileEntity transferFile = transferFileRepository.findByCorrelationId(inboundFile.fileName().fileId())
                .orElseGet(() -> newTransferFile(batch, inboundFile));

        transferFile.setBatch(batch);
        transferFile.setSourceFilename(inboundFile.fileName().fileName());
        transferFile.setRemotePath(inboundFile.remotePath());
        transferFile.setSourceChecksum(sourceChecksum);
        transferFile.setSourceSizeBytes(Files.size(localFile));
        transferFile.setEncryptionFlag(inboundFile.fileName().fileName().endsWith(".pgp"));
        transferFile.setStorageBackend(STORAGE_BACKEND);
        transferFile.setStorageStatus(STATUS_DOWNLOADED);
        transferFile.setDownloadedAt(eventTime);
        transferFile.setProcessingStatus(PROCESSING_PENDING);
        transferFile.setNextProcessingAttemptAt(null);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFile = transferFileRepository.save(transferFile);

        upsertPayload(transferFile, localFile, sourceChecksum, eventTime);
        recordEvent(
                transferFile,
                STATUS_DOWNLOADED,
                eventTime,
                "sourceFile=%s,remotePath=%s,localPath=%s,checksum=%s,sizeBytes=%d".formatted(
                        inboundFile.fileName().fileName(),
                        inboundFile.remotePath(),
                        localFile.toAbsolutePath(),
                        sourceChecksum,
                        transferFile.getSourceSizeBytes()
                )
        );

        batch.setStatus(STATUS_DOWNLOADED);
        transferBatchRepository.save(batch);

        return new PersistedInboundTransfer(transferFile.getId(), transferFile.getCorrelationId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PersistedInboundTransfer recordDownloadedTransferAndAcknowledge(
            InboundTransferFile inboundFile,
            Path localFile,
            String sourceChecksum,
            ZonedDateTime downloadedAt,
            String ackFileName,
            ZonedDateTime acknowledgedAt,
            Runnable ackUploadAction
    ) throws IOException {
        PersistedInboundTransfer persistedTransfer = recordDownloadedTransfer(
                inboundFile,
                localFile,
                sourceChecksum,
                downloadedAt
        );
        ackUploadAction.run();
        markDownloadAcknowledged(persistedTransfer.transferId(), ackFileName, acknowledgedAt);
        return persistedTransfer;
    }

    @Transactional
    public void markDownloadAcknowledged(UUID transferId, String ackFileName, ZonedDateTime acknowledgedAt) {
        OffsetDateTime eventTime = acknowledgedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setDownloadAckSentAt(eventTime);
        transferFile.setStorageStatus(STATUS_DOWNLOAD_ACK_SENT);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_DOWNLOAD_ACK_SENT);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                STATUS_DOWNLOAD_ACK_SENT,
                eventTime,
                "ackFile=%s".formatted(ackFileName)
        );
    }

    @Transactional
    public void markTransferError(UUID transferId, String errorCode, String errorMessage, ZonedDateTime failedAt) {
        OffsetDateTime eventTime = failedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setStorageStatus(STATUS_ERROR);
        transferFile.setLastErrorCode(errorCode);
        transferFile.setLastErrorMessage(truncate(errorMessage, 1024));
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_ERROR);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                STATUS_ERROR,
                eventTime,
                "errorCode=%s,message=%s".formatted(errorCode, truncate(errorMessage, 1024))
        );
    }

    @Transactional
    public void markProcessingStarted(UUID transferId, ZonedDateTime startedAt) {
        OffsetDateTime eventTime = startedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setProcessingStatus(STATUS_PROCESSING);
        transferFile.setNextProcessingAttemptAt(null);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_PROCESSING);
        transferBatchRepository.save(batch);

        recordEvent(transferFile, STATUS_PROCESSING, eventTime, "Processing started");
    }

    @Transactional
    public void markProcessingSucceeded(UUID transferId, int importedRowCount, ZonedDateTime processedAt) {
        OffsetDateTime eventTime = processedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setProcessingStatus(STATUS_PROCESSED_SUCCESS);
        transferFile.setProcessedAt(eventTime);
        transferFile.setNextProcessingAttemptAt(null);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_PROCESSED_SUCCESS);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                STATUS_PROCESSED_SUCCESS,
                eventTime,
                "importedRowCount=%d".formatted(importedRowCount)
        );
    }

    @Transactional
    public void markProcessingRetryScheduled(
            UUID transferId,
            String errorCode,
            String errorMessage,
            ZonedDateTime failedAt,
            ZonedDateTime nextAttemptAt
    ) {
        OffsetDateTime eventTime = failedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setProcessingStatus(PROCESSING_PENDING);
        transferFile.setProcessedAt(null);
        transferFile.setNextProcessingAttemptAt(nextAttemptAt.toOffsetDateTime());
        transferFile.setRetryCount(transferFile.getRetryCount() + 1);
        transferFile.setLastErrorCode(errorCode);
        transferFile.setLastErrorMessage(truncate(errorMessage, 1024));
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_PROCESSING);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                "PROCESSING_RETRY_SCHEDULED",
                eventTime,
                "retryCount=%d,nextProcessingAttemptAt=%s,errorCode=%s,message=%s".formatted(
                        transferFile.getRetryCount(),
                        nextAttemptAt,
                        errorCode,
                        truncate(errorMessage, 1024)
                )
        );
    }

    @Transactional
    public void markProcessingFailedFinal(UUID transferId, String errorCode, String errorMessage, ZonedDateTime failedAt) {
        OffsetDateTime eventTime = failedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setProcessingStatus(STATUS_PROCESSED_FAILED);
        transferFile.setProcessedAt(eventTime);
        transferFile.setNextProcessingAttemptAt(null);
        transferFile.setRetryCount(transferFile.getRetryCount() + 1);
        transferFile.setLastErrorCode(errorCode);
        transferFile.setLastErrorMessage(truncate(errorMessage, 1024));
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_PROCESSED_FAILED);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                STATUS_PROCESSED_FAILED,
                eventTime,
                "retryCount=%d,errorCode=%s,message=%s".formatted(
                        transferFile.getRetryCount(),
                        errorCode,
                        truncate(errorMessage, 1024)
                )
        );
    }

    @Transactional
    public void markResponseUploaded(UUID transferId, String responseFileName, ZonedDateTime uploadedAt) {
        OffsetDateTime eventTime = uploadedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        transferFile.setResponseFilename(responseFileName);
        transferFile.setStorageStatus(STATUS_RESPONSE_UPLOADED);
        transferFileRepository.save(transferFile);

        TransferBatchEntity batch = transferFile.getBatch();
        batch.setStatus(STATUS_RESPONSE_UPLOADED);
        transferBatchRepository.save(batch);

        recordEvent(
                transferFile,
                STATUS_RESPONSE_UPLOADED,
                eventTime,
                "responseFile=%s".formatted(responseFileName)
        );
    }

    @Transactional
    public void markResponseUploadFailed(UUID transferId, String errorCode, String errorMessage, ZonedDateTime failedAt) {
        OffsetDateTime eventTime = failedAt.toOffsetDateTime();
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        recordEvent(
                transferFile,
                STATUS_RESPONSE_UPLOAD_FAILED,
                eventTime,
                "errorCode=%s,message=%s".formatted(errorCode, truncate(errorMessage, 1024))
        );
    }

    @Transactional
    public boolean purgeEncryptedPayload(UUID transferId, ZonedDateTime purgedAt) {
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));
        TransferPayloadEntity payload = transferPayloadRepository.findById(transferId).orElse(null);

        if (payload == null) {
            return false;
        }

        transferPayloadRepository.delete(payload);
        recordEvent(
                transferFile,
                "PAYLOAD_PURGED",
                purgedAt.toOffsetDateTime(),
                "contentSizeBytes=%d,contentChecksum=%s".formatted(
                        payload.getContentSizeBytes(),
                        payload.getContentChecksum()
                )
        );
        return true;
    }

    private TransferBatchEntity findOrCreateBatch(LocalDate businessDate, OffsetDateTime scheduledAt) {
        return transferBatchRepository.findByBusinessDateAndBatchNumberAndFlow(
                businessDate,
                DEFAULT_BATCH_NUMBER,
                FLOW_A2B
        ).orElseGet(() -> {
            TransferBatchEntity batch = new TransferBatchEntity();
            batch.setId(UUID.randomUUID());
            batch.setBusinessDate(businessDate);
            batch.setBatchNumber(DEFAULT_BATCH_NUMBER);
            batch.setFlow(FLOW_A2B);
            batch.setScheduledAt(scheduledAt);
            batch.setStatus(STATUS_DOWNLOADED);
            return transferBatchRepository.save(batch);
        });
    }

    private TransferFileEntity newTransferFile(TransferBatchEntity batch, InboundTransferFile inboundFile) {
        TransferFileEntity transferFile = new TransferFileEntity();
        transferFile.setId(UUID.randomUUID());
        transferFile.setBatch(batch);
        transferFile.setCorrelationId(inboundFile.fileName().fileId());
        transferFile.setSourceFilename(inboundFile.fileName().fileName());
        transferFile.setRemotePath(inboundFile.remotePath());
        transferFile.setStorageBackend(STORAGE_BACKEND);
        transferFile.setStorageStatus(STATUS_DOWNLOADED);
        transferFile.setProcessingStatus(PROCESSING_PENDING);
        transferFile.setNextProcessingAttemptAt(null);
        transferFile.setRetryCount(0);
        return transferFile;
    }

    private void upsertPayload(
            TransferFileEntity transferFile,
            Path localFile,
            String sourceChecksum,
            OffsetDateTime createdAt
    ) throws IOException {
        TransferPayloadEntity payload = transferPayloadRepository.findById(transferFile.getId())
                .orElseGet(TransferPayloadEntity::new);

        payload.setTransferId(transferFile.getId());
        payload.setEncryptedContent(Files.readAllBytes(localFile));
        payload.setContentChecksum(sourceChecksum);
        payload.setContentSizeBytes(Files.size(localFile));
        if (payload.getCreatedAt() == null) {
            payload.setCreatedAt(createdAt.withOffsetSameInstant(ZoneOffset.UTC));
        }

        transferPayloadRepository.save(payload);
    }

    private void recordEvent(
            TransferFileEntity transferFile,
            String eventType,
            OffsetDateTime eventTimestamp,
            String detail
    ) {
        TransferEventEntity event = new TransferEventEntity();
        event.setId(UUID.randomUUID());
        event.setTransferFile(transferFile);
        event.setEventType(eventType);
        event.setEventTimestamp(eventTimestamp);
        event.setDetail(detail);
        transferEventRepository.save(event);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
