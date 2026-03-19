package pes.poc.b.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pes.poc.b.persistence.entity.TransferFileEntity;

public interface TransferFileRepository extends JpaRepository<TransferFileEntity, UUID> {

    Optional<TransferFileEntity> findByCorrelationId(String correlationId);

    @Query("""
            select transferFile
            from TransferFileEntity transferFile
            where transferFile.storageStatus = :storageStatus
              and (
                    transferFile.processingStatus in :resultPendingStatuses
                    or (
                        transferFile.processingStatus = :retryablePendingStatus
                        and (
                            transferFile.nextProcessingAttemptAt is null
                            or transferFile.nextProcessingAttemptAt <= :eligibleAt
                        )
                    )
                )
            order by transferFile.downloadedAt asc, transferFile.id asc
            """)
    List<TransferFileEntity> findEligibleForProcessing(
            @Param("storageStatus") String storageStatus,
            @Param("resultPendingStatuses") List<String> resultPendingStatuses,
            @Param("retryablePendingStatus") String retryablePendingStatus,
            @Param("eligibleAt") OffsetDateTime eligibleAt,
            Pageable pageable
    );
}
