package pes.poc.b.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pes.poc.b.persistence.entity.TransferFileEntity;

public interface TransferFileRepository extends JpaRepository<TransferFileEntity, UUID> {

    Optional<TransferFileEntity> findByCorrelationId(String correlationId);

    List<TransferFileEntity> findTop20ByProcessingStatusAndStorageStatusOrderByDownloadedAtAscIdAsc(
            String processingStatus,
            String storageStatus
    );

    List<TransferFileEntity> findTop20ByStorageStatusAndProcessingStatusInOrderByDownloadedAtAscIdAsc(
            String storageStatus,
            List<String> processingStatuses
    );
}
