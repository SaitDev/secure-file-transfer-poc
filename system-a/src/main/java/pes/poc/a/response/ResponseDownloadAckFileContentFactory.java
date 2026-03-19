package pes.poc.a.response;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

@Component
public class ResponseDownloadAckFileContentFactory {

    public String create(InboundResponseFileName fileName, ZonedDateTime downloadedAt) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator(), "", System.lineSeparator());
        joiner.add("TYPE=DOWNLOAD_ACK");
        joiner.add("FLOW=B2A");
        joiner.add("FILE_ID=" + fileName.fileId());
        joiner.add("SOURCE_FILE=" + fileName.fileName());
        joiner.add("DOWNLOADED_BY=SYSTEM_A");
        joiner.add("DOWNLOADED_AT=" + downloadedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        joiner.add("STATUS=SUCCESS");
        return joiner.toString();
    }
}
