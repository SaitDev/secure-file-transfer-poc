package pes.poc.a.persistence.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "transfer_file",
        uniqueConstraints = @UniqueConstraint(name = "uk_transfer_file_file_id", columnNames = "file_id")
)
@Getter
@Setter
@NoArgsConstructor
public class TransferFileEntity {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false, length = 32)
    private String batchId;

    @Column(name = "file_id", nullable = false, length = 32)
    private String fileId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "batch_number", nullable = false)
    private short batchNumber;

    @Column(nullable = false, length = 16)
    private String flow;

    @Column(name = "source_filename", nullable = false, length = 255)
    private String sourceFilename;

    @Column(name = "local_path", nullable = false, length = 512)
    private String localPath;

    @Column(name = "remote_path", length = 512)
    private String remotePath;

    @Column(name = "source_size_bytes")
    private Long sourceSizeBytes;

    @Column(name = "encryption_flag", nullable = false)
    private boolean encryptionFlag;

    @Column(name = "transfer_status", nullable = false, length = 64)
    private String transferStatus;

    @Column(name = "batch_size", nullable = false)
    private int batchSize;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "encrypted_at")
    private OffsetDateTime encryptedAt;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    @Column(name = "response_filename", length = 255)
    private String responseFilename;

    @Column(name = "response_downloaded_at")
    private OffsetDateTime responseDownloadedAt;

    @Column(name = "response_download_ack_sent_at")
    private OffsetDateTime responseDownloadAckSentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;
}
