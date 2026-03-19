package pes.poc.b.api;

import java.time.OffsetDateTime;

public record ManualInboundPollingResponse(
        OffsetDateTime triggeredAt,
        int processedFileCount
) {
}
