package pes.poc.a.response;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemAResponsePollingScheduler {

    private final SystemAResponsePollingService systemAResponsePollingService;

    @Scheduled(cron = "${system-a.response-poll-start-cron}", zone = "${system-a.zone}")
    public void pollAtWindowStart() {
        systemAResponsePollingService.pollResponseFiles();
    }

    @Scheduled(cron = "${system-a.response-poll-main-cron}", zone = "${system-a.zone}")
    public void pollDuringMainWindow() {
        systemAResponsePollingService.pollResponseFiles();
    }

    @Scheduled(cron = "${system-a.response-poll-end-cron}", zone = "${system-a.zone}")
    public void pollDuringGraceWindow() {
        systemAResponsePollingService.pollResponseFiles();
    }
}
