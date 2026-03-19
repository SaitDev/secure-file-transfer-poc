package pes.poc.b.processing;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import pes.poc.b.config.SystemBProperties;
import pes.poc.b.customer.CustomerEntity;
import pes.poc.b.customer.CustomerRepository;
import pes.poc.b.persistence.entity.TransferFileEntity;
import pes.poc.b.persistence.entity.TransferPayloadEntity;
import pes.poc.b.persistence.repository.TransferFileRepository;
import pes.poc.b.persistence.repository.TransferPayloadRepository;
import pes.poc.b.persistence.service.InboundTransferPersistenceService;
import pes.poc.b.sftp.SystemBSftpClient;
import pes.poc.b.transfer.InboundTransferFileName;

@Service
@RequiredArgsConstructor
public class InboundTransferProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InboundTransferProcessingService.class);
    private static final String PROCESSING_PENDING = "PENDING";
    private static final String PROCESSING_IN_PROGRESS = "PROCESSING";
    private static final String PROCESSING_SUCCESS = "PROCESSED_SUCCESS";
    private static final String PROCESSING_FAILED = "PROCESSED_FAILED";
    private static final String STORAGE_STATUS_READY = "DOWNLOAD_ACK_SENT";
    private static final String ERROR_CODE_DECRYPT = "INBOUND_DECRYPT_FAILED";
    private static final String ERROR_CODE_PARSE = "INBOUND_CSV_PARSE_FAILED";
    private static final String ERROR_CODE_IMPORT = "INBOUND_CUSTOMER_IMPORT_FAILED";
    private static final String ERROR_CODE_RESPONSE_UPLOAD = "OUTBOUND_RESULT_UPLOAD_FAILED";
    private static final String RESULT_STATUS_SUCCESS = "SUCCESS";
    private static final String RESULT_STATUS_FAILED = "FAILED";

    private final SystemBProperties systemBProperties;
    private final TransferFileRepository transferFileRepository;
    private final TransferPayloadRepository transferPayloadRepository;
    private final InboundTransferPersistenceService inboundTransferPersistenceService;
    private final SystemBPgpDecryptionService systemBPgpDecryptionService;
    private final CustomerCsvParser customerCsvParser;
    private final CustomerRepository customerRepository;
    private final ProcessResultFileContentFactory processResultFileContentFactory;
    private final SystemBSftpClient systemBSftpClient;
    private final TransactionTemplate transactionTemplate;

    public int processPendingTransfers() {
        List<TransferFileEntity> pendingTransfers = transferFileRepository
                .findTop20ByStorageStatusAndProcessingStatusInOrderByDownloadedAtAscIdAsc(
                        STORAGE_STATUS_READY,
                        List.of(PROCESSING_PENDING, PROCESSING_SUCCESS, PROCESSING_FAILED)
                );

        int processedCount = 0;
        for (TransferFileEntity transferFile : pendingTransfers) {
            if (processSingleTransfer(transferFile.getId())) {
                processedCount++;
            }
        }

        if (processedCount > 0) {
            log.info("Processed {} staged inbound transfer(s) into the customer table", processedCount);
        }
        return processedCount;
    }

    private boolean processSingleTransfer(UUID transferId) {
        TransferFileEntity transferFile = transferFileRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transfer file not found for id " + transferId));

        return switch (transferFile.getProcessingStatus()) {
            case PROCESSING_PENDING, PROCESSING_IN_PROGRESS -> processAndUploadResult(transferFile);
            case PROCESSING_SUCCESS, PROCESSING_FAILED -> uploadOutstandingResult(transferFile);
            default -> false;
        };
    }

    private boolean processAndUploadResult(TransferFileEntity transferFile) {
        UUID transferId = transferFile.getId();
        ZonedDateTime startedAt = now();
        byte[] decryptedBytes = null;
        List<CustomerCsvRecord> customerRecords = null;

        try {
            inboundTransferPersistenceService.markProcessingStarted(transferId, startedAt);

            TransferPayloadEntity payload = transferPayloadRepository.findById(transferId)
                    .orElseThrow(() -> new IllegalStateException("Encrypted payload not found for transfer id " + transferId));

            decryptedBytes = systemBPgpDecryptionService.decrypt(payload.getEncryptedContent(), transferFile.getSourceFilename());
            customerRecords = parseCustomers(decryptedBytes);
            importCustomers(transferId, customerRecords, startedAt.toOffsetDateTime());
            inboundTransferPersistenceService.markProcessingSucceeded(transferId, customerRecords.size(), now());

            uploadSuccessResult(transferFile, customerRecords.size(), "Imported successfully");
            log.info(
                    "Imported {} customer row(s) from inbound file {}",
                    customerRecords.size(),
                    transferFile.getSourceFilename()
            );
            return true;
        } catch (IllegalStateException exception) {
            String errorCode = resolveErrorCode(exception);
            inboundTransferPersistenceService.markProcessingFailed(transferId, errorCode, exception.getMessage(), now());
            uploadFailureResult(transferFile, customerRecords == null ? 0 : customerRecords.size(), exception.getMessage());
            log.warn("Inbound processing failed for {}: {}", transferFile.getSourceFilename(), exception.getMessage());
            return false;
        } finally {
            if (decryptedBytes != null) {
                Arrays.fill(decryptedBytes, (byte) 0);
            }
        }
    }

    private List<CustomerCsvRecord> parseCustomers(byte[] decryptedBytes) {
        try {
            return customerCsvParser.parse(decryptedBytes);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException(exception.getMessage(), new ProcessingStageException(ERROR_CODE_PARSE, exception));
        }
    }

    private void importCustomers(UUID transferId, List<CustomerCsvRecord> customerRecords, OffsetDateTime importedAt) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                var customerIds = customerRecords.stream()
                        .map(CustomerCsvRecord::customerId)
                        .collect(Collectors.toSet());

                List<String> existingCustomerIds = customerRepository.findByCustomerIdIn(customerIds).stream()
                        .map(CustomerEntity::getCustomerId)
                        .sorted()
                        .toList();

                if (!existingCustomerIds.isEmpty()) {
                    throw new IllegalStateException(
                            "Customer IDs already exist in target table: " + String.join(", ", existingCustomerIds)
                    );
                }

                List<CustomerEntity> customers = customerRecords.stream()
                        .map(record -> toCustomerEntity(transferId, record, importedAt))
                        .toList();

                customerRepository.saveAll(customers);
            });
        } catch (RuntimeException exception) {
            throw new IllegalStateException(exception.getMessage(), new ProcessingStageException(ERROR_CODE_IMPORT, exception));
        }
    }

    private boolean uploadOutstandingResult(TransferFileEntity transferFile) {
        if (PROCESSING_SUCCESS.equals(transferFile.getProcessingStatus())) {
            long importedCount = customerRepository.countBySourceTransferId(transferFile.getId());
            uploadSuccessResult(transferFile, Math.toIntExact(importedCount), "Imported successfully");
            return true;
        }

        if (PROCESSING_FAILED.equals(transferFile.getProcessingStatus())) {
            uploadFailureResult(transferFile, 0, transferFile.getLastErrorMessage());
            return false;
        }

        return false;
    }

    private void uploadSuccessResult(TransferFileEntity transferFile, int importedRecordCount, String message) {
        uploadResult(
                transferFile,
                RESULT_STATUS_SUCCESS,
                importedRecordCount,
                importedRecordCount,
                0,
                message
        );
    }

    private void uploadFailureResult(TransferFileEntity transferFile, int totalRecords, String message) {
        uploadResult(
                transferFile,
                RESULT_STATUS_FAILED,
                totalRecords,
                0,
                totalRecords,
                message
        );
    }

    private void uploadResult(
            TransferFileEntity transferFile,
            String resultStatus,
            int totalRecords,
            int successRecords,
            int failedRecords,
            String message
    ) {
        try {
            InboundTransferFileName fileName = InboundTransferFileName.parse(transferFile.getSourceFilename())
                    .orElseThrow(() -> new IllegalStateException("Unsupported inbound file name " + transferFile.getSourceFilename()));
            ZonedDateTime processedAt = transferFile.getProcessedAt() == null
                    ? now()
                    : transferFile.getProcessedAt().atZoneSameInstant(ZoneId.of(systemBProperties.getZone()));
            String resultFileName = fileName.resultAckFileName();
            String content = processResultFileContentFactory.create(
                    fileName,
                    processedAt,
                    resultStatus,
                    totalRecords,
                    successRecords,
                    failedRecords,
                    message
            );

            systemBSftpClient.uploadResultFile(resultFileName, content);
            inboundTransferPersistenceService.markResponseUploaded(transferFile.getId(), resultFileName, now());
        } catch (IllegalStateException exception) {
            inboundTransferPersistenceService.markResponseUploadFailed(
                    transferFile.getId(),
                    ERROR_CODE_RESPONSE_UPLOAD,
                    exception.getMessage(),
                    now()
            );
            log.warn("Result upload failed for {}: {}", transferFile.getSourceFilename(), exception.getMessage());
        }
    }

    private CustomerEntity toCustomerEntity(UUID transferId, CustomerCsvRecord record, OffsetDateTime importedAt) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setSourceTransferId(transferId);
        customer.setCustomerId(record.customerId());
        customer.setFullName(record.fullName());
        customer.setDateOfBirth(record.dateOfBirth());
        customer.setGender(record.gender());
        customer.setNationalId(record.nationalId());
        customer.setMobileNumber(record.mobileNumber());
        customer.setEmail(record.email());
        customer.setAddressLine(record.addressLine());
        customer.setCreatedAt(importedAt);
        return customer;
    }

    private String resolveErrorCode(IllegalStateException exception) {
        if (exception.getCause() instanceof ProcessingStageException stageException) {
            return stageException.errorCode();
        }
        return ERROR_CODE_DECRYPT;
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.of(systemBProperties.getZone()));
    }

    private static final class ProcessingStageException extends RuntimeException {

        private final String errorCode;

        private ProcessingStageException(String errorCode, Throwable cause) {
            super(cause);
            this.errorCode = errorCode;
        }

        private String errorCode() {
            return errorCode;
        }
    }
}
