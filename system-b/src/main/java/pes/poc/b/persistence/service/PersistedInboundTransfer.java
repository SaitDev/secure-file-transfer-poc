package pes.poc.b.persistence.service;

import java.util.UUID;

public record PersistedInboundTransfer(
        UUID transferId,
        String fileId
) {
}
