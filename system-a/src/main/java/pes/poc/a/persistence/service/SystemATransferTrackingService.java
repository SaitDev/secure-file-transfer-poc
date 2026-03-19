package pes.poc.a.persistence.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pes.poc.a.persistence.entity.TransferFileEntity;
import pes.poc.a.persistence.repository.TransferFileRepository;

@Service
@RequiredArgsConstructor
public class SystemATransferTrackingService {

    private static final String FLOW_A2B = "A2B";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_ENCRYPTED = "ENCRYPTED";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_RESPONSE_DOWNLOADED = "RESPONSE_DOWNLOADED";
    private static final String STATUS_RESPONSE_DOWNLOAD_ACK_SENT = "RESPONSE_DOWNLOAD_ACK_SENT";
    private static final String STATUS_ERROR = "ERROR";

    private final TransferFileRepository transferFileRepository;

    @Transactional
    public UUID recordCreated(
            String batchId,
            String fileId,
            int batchSize,
            Path encryptedFile,
            TransferBatchScheduleResolver.ResolvedTransferBatch batchSchedule,
            OffsetDateTime createdAt
    ) {
        TransferFileEntity transferFile = new TransferFileEntity();
        transferFile.setId(UUID.randomUUID());
        transferFile.setBatchId(batchId);
        transferFile.setFileId(fileId);
        transferFile.setBusinessDate(batchSchedule.businessDate());
        transferFile.setBatchNumber(batchSchedule.batchNumber());
        transferFile.setFlow(FLOW_A2B);
        transferFile.setSourceFilename(encryptedFile.getFileName().toString());
        transferFile.setLocalPath(encryptedFile.toAbsolutePath().toString());
        transferFile.setEncryptionFlag(true);
        transferFile.setTransferStatus(STATUS_CREATED);
        transferFile.setBatchSize(batchSize);
        transferFile.setCreatedAt(createdAt);
        transferFile.setRetryCount(0);

        return transferFileRepository.save(transferFile).getId();
    }

    @Transactional
    public void markEncrypted(UUID transferId, Path encryptedFile, long sourceSizeBytes, OffsetDateTime encryptedAt) {
        TransferFileEntity transferFile = findRequired(transferId);
        transferFile.setLocalPath(encryptedFile.toAbsolutePath().toString());
        transferFile.setSourceFilename(encryptedFile.getFileName().toString());
        transferFile.setSourceSizeBytes(sourceSizeBytes);
        transferFile.setEncryptedAt(encryptedAt);
        transferFile.setTransferStatus(STATUS_ENCRYPTED);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);
    }

    @Transactional
    public void markUploaded(UUID transferId, String remotePath, OffsetDateTime uploadedAt) {
        TransferFileEntity transferFile = findRequired(transferId);
        transferFile.setRemotePath(remotePath);
        transferFile.setUploadedAt(uploadedAt);
        transferFile.setTransferStatus(STATUS_UPLOADED);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);
    }

    @Transactional
    public void markResponseDownloaded(
            UUID transferId,
            String responseFileName,
            OffsetDateTime downloadedAt
    ) {
        TransferFileEntity transferFile = findRequired(transferId);
        transferFile.setResponseFilename(responseFileName);
        transferFile.setResponseDownloadedAt(downloadedAt);
        transferFile.setTransferStatus(STATUS_RESPONSE_DOWNLOADED);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);
    }

    @Transactional
    public void markResponseDownloadAcknowledged(UUID transferId, OffsetDateTime acknowledgedAt) {
        TransferFileEntity transferFile = findRequired(transferId);
        transferFile.setResponseDownloadAckSentAt(acknowledgedAt);
        transferFile.setTransferStatus(STATUS_RESPONSE_DOWNLOAD_ACK_SENT);
        transferFile.setLastErrorCode(null);
        transferFile.setLastErrorMessage(null);
        transferFileRepository.save(transferFile);
    }

    @Transactional
    public void markError(UUID transferId, String errorCode, String errorMessage) {
        TransferFileEntity transferFile = findRequired(transferId);
        transferFile.setTransferStatus(STATUS_ERROR);
        transferFile.setLastErrorCode(errorCode);
        transferFile.setLastErrorMessage(truncate(errorMessage, 1024));
        transferFileRepository.save(transferFile);
    }

    private TransferFileEntity findRequired(UUID transferId) {
        return transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
