package pes.poc.b.processing;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import pes.poc.b.transfer.InboundTransferFileName;

@Component
public class ProcessResultFileContentFactory {

    public String create(
            InboundTransferFileName fileName,
            ZonedDateTime processedAt,
            String status,
            int totalRecords,
            int successRecords,
            int failedRecords,
            String message
    ) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator(), "", System.lineSeparator());
        joiner.add("TYPE=PROCESS_RESULT");
        joiner.add("FLOW=B2A");
        joiner.add("FILE_ID=" + fileName.fileId());
        joiner.add("SOURCE_FILE=" + fileName.fileName());
        joiner.add("RESULT_FILE=" + fileName.resultAckFileName());
        joiner.add("PROCESSED_BY=SYSTEM_B");
        joiner.add("PROCESSED_AT=" + processedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        joiner.add("STATUS=" + status);
        joiner.add("TOTAL_RECORDS=" + totalRecords);
        joiner.add("SUCCESS_RECORDS=" + successRecords);
        joiner.add("FAILED_RECORDS=" + failedRecords);
        joiner.add("MESSAGE=" + sanitizeMessage(message));
        return joiner.toString();
    }

    private String sanitizeMessage(String message) {
        return message == null ? "" : message.replace(System.lineSeparator(), " ").trim();
    }
}
