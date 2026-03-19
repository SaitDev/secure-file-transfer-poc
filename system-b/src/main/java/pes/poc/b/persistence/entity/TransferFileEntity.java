package pes.poc.b.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "transfer_file",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transfer_file_correlation_id",
                columnNames = "correlation_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TransferFileEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TransferBatchEntity batch;

    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;

    @Column(name = "source_filename", nullable = false, length = 255)
    private String sourceFilename;

    @Column(name = "response_filename", length = 255)
    private String responseFilename;

    @Column(name = "remote_path", nullable = false, length = 512)
    private String remotePath;

    @Column(name = "source_checksum", length = 64)
    private String sourceChecksum;

    @Column(name = "source_size_bytes")
    private Long sourceSizeBytes;

    @Column(name = "encryption_flag", nullable = false)
    private boolean encryptionFlag;

    @Column(name = "storage_backend", nullable = false, length = 32)
    private String storageBackend;

    @Column(name = "storage_status", nullable = false, length = 64)
    private String storageStatus;

    @Column(name = "downloaded_at")
    private OffsetDateTime downloadedAt;

    @Column(name = "download_ack_sent_at")
    private OffsetDateTime downloadAckSentAt;

    @Column(name = "processing_status", length = 64)
    private String processingStatus;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;
}
