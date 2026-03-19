package pes.poc.a.scheduling;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(SystemAProperties.class)
@RequiredArgsConstructor
public class OutboundBatchSchedulingConfiguration implements SchedulingConfigurer {

    private final OutboundBatchJobRunner outboundBatchJobRunner;
    private final SystemAProperties systemAProperties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ZoneId zoneId = ZoneId.of(systemAProperties.getZone());
        taskRegistrar.addTriggerTask(
                () -> runScheduledBatch(zoneId),
                new CronTrigger(systemAProperties.getMainWindowCron(), zoneId)
        );
        taskRegistrar.addTriggerTask(
                () -> runScheduledBatch(zoneId),
                new CronTrigger(systemAProperties.getFinalBatchCron(), zoneId)
        );
    }

    private void runScheduledBatch(ZoneId zoneId) {
        outboundBatchJobRunner.runBatch(ZonedDateTime.now(zoneId).toLocalDateTime());
    }
}
