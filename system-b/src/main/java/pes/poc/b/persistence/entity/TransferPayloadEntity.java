package pes.poc.b.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transfer_payload")
@Getter
@Setter
@NoArgsConstructor
public class TransferPayloadEntity {

    @Id
    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "encrypted_content", nullable = false, columnDefinition = "BYTEA")
    private byte[] encryptedContent;

    @Column(name = "content_checksum", length = 64)
    private String contentChecksum;

    @Column(name = "content_size_bytes", nullable = false)
    private long contentSizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "purged_at")
    private OffsetDateTime purgedAt;
}
