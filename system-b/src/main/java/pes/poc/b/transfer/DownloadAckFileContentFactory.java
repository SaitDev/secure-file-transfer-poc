package pes.poc.b.transfer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

@Component
public class DownloadAckFileContentFactory {

    public String create(InboundTransferFileName fileName, ZonedDateTime downloadedAt, String sha256) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator(), "", System.lineSeparator());
        joiner.add("TYPE=DOWNLOAD_ACK");
        joiner.add("FLOW=" + fileName.flow());
        joiner.add("FILE_ID=" + fileName.fileId());
        joiner.add("SOURCE_FILE=" + fileName.fileName());
        joiner.add("DOWNLOADED_BY=SYSTEM_B");
        joiner.add("DOWNLOADED_AT=" + downloadedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        joiner.add("STATUS=SUCCESS");
        joiner.add("SHA256=" + sha256);
        return joiner.toString();
    }
}
