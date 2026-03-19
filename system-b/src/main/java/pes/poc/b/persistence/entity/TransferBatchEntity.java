package pes.poc.b.persistence.entity;

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
        name = "transfer_batch",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transfer_batch_business_date_batch_number_flow",
                columnNames = {"business_date", "batch_number", "flow"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TransferBatchEntity {

    @Id
    private UUID id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "batch_number", nullable = false)
    private short batchNumber;

    @Column(nullable = false, length = 16)
    private String flow;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(nullable = false, length = 64)
    private String status;
}
