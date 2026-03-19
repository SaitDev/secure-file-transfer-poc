package pes.poc.a.scheduling;

import java.nio.file.Path;

public record OutboundBatchExecution(
        String batchId,
        String fileId,
        Path localFile,
        String remoteFile,
        int batchSize
) {
}
