package pes.poc.a.persistence.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class TransferBatchScheduleResolver {

    private static final short UNSCHEDULED_BATCH_NUMBER = 0;

    private TransferBatchScheduleResolver() {
    }

    public static ResolvedTransferBatch resolve(LocalDateTime triggeredAt) {
        short batchNumber = resolveBatchNumber(triggeredAt.toLocalTime());
        return new ResolvedTransferBatch(triggeredAt.toLocalDate(), batchNumber);
    }

    private static short resolveBatchNumber(LocalTime triggeredTime) {
        int minuteOfDay = triggeredTime.getHour() * 60 + triggeredTime.getMinute();
        if (minuteOfDay < 9 * 60) {
            return UNSCHEDULED_BATCH_NUMBER;
        }
        if (minuteOfDay < 9 * 60 + 30) {
            return 1;
        }
        if (minuteOfDay < 10 * 60) {
            return 2;
        }
        if (minuteOfDay < 10 * 60 + 30) {
            return 3;
        }
        if (minuteOfDay < 11 * 60) {
            return 4;
        }
        if (minuteOfDay < 12 * 60) {
            return 5;
        }
        return UNSCHEDULED_BATCH_NUMBER;
    }

    public record ResolvedTransferBatch(
            LocalDate businessDate,
            short batchNumber
    ) {
    }
}
