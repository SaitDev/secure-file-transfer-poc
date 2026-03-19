package pes.poc.b.persistence.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pes.poc.b.persistence.entity.TransferBatchEntity;

public interface TransferBatchRepository extends JpaRepository<TransferBatchEntity, UUID> {

    Optional<TransferBatchEntity> findByBusinessDateAndBatchNumberAndFlow(
            LocalDate businessDate,
            short batchNumber,
            String flow
    );
}
