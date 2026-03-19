package pes.poc.a.api;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pes.poc.a.response.SystemAResponsePollingService;
import pes.poc.a.scheduling.OutboundBatchJobRunner;
import pes.poc.a.scheduling.SystemAProperties;

@RestController
@RequestMapping("/internal/system-a/jobs")
@RequiredArgsConstructor
public class ManualJobTriggerController {

    private final OutboundBatchJobRunner outboundBatchJobRunner;
    private final SystemAResponsePollingService systemAResponsePollingService;
    private final SystemAProperties systemAProperties;

    @PostMapping("/outbound")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ManualJobTriggerResponse triggerOutboundJob() {
        return ManualJobTriggerResponse.from(outboundBatchJobRunner.runBatch(LocalDateTime.now()));
    }

    @PostMapping("/response-polling")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ManualResponsePollingResponse triggerResponsePolling() {
        int processedCount = systemAResponsePollingService.pollResponseFiles();
        OffsetDateTime triggeredAt = OffsetDateTime.now(ZoneId.of(systemAProperties.getZone()));
        return new ManualResponsePollingResponse(triggeredAt, processedCount);
    }
}
