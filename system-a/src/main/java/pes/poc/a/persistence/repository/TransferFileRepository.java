package pes.poc.a.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pes.poc.a.persistence.entity.TransferFileEntity;

public interface TransferFileRepository extends JpaRepository<TransferFileEntity, UUID> {

    Optional<TransferFileEntity> findByFileId(String fileId);
}
