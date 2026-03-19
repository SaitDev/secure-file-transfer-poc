package pes.poc.b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "system-b")
public class SystemBProperties {

    private String zone = "Asia/Ho_Chi_Minh";
    private String pollStartCron = "0 59 8 * * *";
    private String pollMainCron = "0 * 9,10 * * *";
    private String pollEndCron = "0 0-15 11 * * *";
    private String processingCron = "20 * * * * *";
    private String stagingDirectory = "build/generated/inbound/staging";
    private String receiptDirectory = "build/generated/inbound/receipts";
    private String sftpHost = "localhost";
    private int sftpPort = 2022;
    private String sftpUsername = "system-b";
    private String sftpPassword = "SystemB123!";
    private String remoteRecvDirectory = "/recv";
    private String remoteAckDirectory = "/ack";
    private String remoteSendDirectory = "/send";
    private String pgpPassphrase = "LocalPgpPassphrase123!";
}
