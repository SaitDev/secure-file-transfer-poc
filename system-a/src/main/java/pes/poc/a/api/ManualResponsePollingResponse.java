package pes.poc.a.api;

import java.time.OffsetDateTime;

public record ManualResponsePollingResponse(
        OffsetDateTime triggeredAt,
        int processedResponseFileCount
) {
}
