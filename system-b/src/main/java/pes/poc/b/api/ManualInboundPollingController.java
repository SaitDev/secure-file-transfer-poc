package pes.poc.b.api;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pes.poc.b.config.SystemBProperties;
import pes.poc.b.processing.InboundTransferProcessingService;
import pes.poc.b.transfer.InboundTransferService;

@RestController
@RequestMapping("/internal/system-b/jobs")
@RequiredArgsConstructor
public class ManualInboundPollingController {

    private final InboundTransferService inboundTransferService;
    private final InboundTransferProcessingService inboundTransferProcessingService;
    private final SystemBProperties systemBProperties;

    @PostMapping("/inbound-polling")
    public ManualInboundPollingResponse triggerInboundPolling() {
        int processedCount = inboundTransferService.pollInboundFiles();
        OffsetDateTime triggeredAt = OffsetDateTime.now(ZoneId.of(systemBProperties.getZone()));
        return new ManualInboundPollingResponse(triggeredAt, processedCount);
    }

    @PostMapping("/inbound-processing")
    public ManualInboundProcessingResponse triggerInboundProcessing() {
        int processedCount = inboundTransferProcessingService.processPendingTransfers();
        OffsetDateTime triggeredAt = OffsetDateTime.now(ZoneId.of(systemBProperties.getZone()));
        return new ManualInboundProcessingResponse(triggeredAt, processedCount);
    }
}
