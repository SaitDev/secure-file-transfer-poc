package pes.poc.b.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pes.poc.b.persistence.entity.TransferPayloadEntity;

public interface TransferPayloadRepository extends JpaRepository<TransferPayloadEntity, UUID> {
}
