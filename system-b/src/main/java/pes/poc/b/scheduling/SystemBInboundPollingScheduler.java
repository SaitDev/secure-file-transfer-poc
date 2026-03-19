package pes.poc.b.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pes.poc.b.processing.InboundTransferProcessingService;
import pes.poc.b.transfer.InboundTransferService;

@Component
@RequiredArgsConstructor
public class SystemBInboundPollingScheduler {

    private final InboundTransferService inboundTransferService;
    private final InboundTransferProcessingService inboundTransferProcessingService;

    @Scheduled(cron = "${system-b.poll-start-cron}", zone = "${system-b.zone}")
    public void pollAtWindowStart() {
        inboundTransferService.pollInboundFiles();
    }

    @Scheduled(cron = "${system-b.poll-main-cron}", zone = "${system-b.zone}")
    public void pollDuringMainWindow() {
        inboundTransferService.pollInboundFiles();
    }

    @Scheduled(cron = "${system-b.poll-end-cron}", zone = "${system-b.zone}")
    public void pollDuringGraceWindow() {
        inboundTransferService.pollInboundFiles();
    }

    @Scheduled(cron = "${system-b.processing-cron}", zone = "${system-b.zone}")
    public void processDownloadedInboundFiles() {
        inboundTransferProcessingService.processPendingTransfers();
    }
}
