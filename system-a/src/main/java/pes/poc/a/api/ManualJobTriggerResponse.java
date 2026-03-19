package pes.poc.a.api;

import pes.poc.a.scheduling.OutboundBatchExecution;

public record ManualJobTriggerResponse(
        String batchId,
        String fileId,
        String localFile,
        String remoteFile,
        int batchSize
) {

    public static ManualJobTriggerResponse from(OutboundBatchExecution execution) {
        return new ManualJobTriggerResponse(
                execution.batchId(),
                execution.fileId(),
                execution.localFile().toString(),
                execution.remoteFile(),
                execution.batchSize()
        );
    }
}
