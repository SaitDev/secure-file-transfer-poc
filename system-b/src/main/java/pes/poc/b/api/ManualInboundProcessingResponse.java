package pes.poc.b.api;

import java.time.OffsetDateTime;

public record ManualInboundProcessingResponse(
        OffsetDateTime triggeredAt,
        int processedTransferCount
) {
}
