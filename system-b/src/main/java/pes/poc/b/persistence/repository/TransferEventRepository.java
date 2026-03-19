package pes.poc.b.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pes.poc.b.persistence.entity.TransferEventEntity;

public interface TransferEventRepository extends JpaRepository<TransferEventEntity, UUID> {
}
