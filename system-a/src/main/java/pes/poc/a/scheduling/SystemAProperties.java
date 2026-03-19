package pes.poc.a.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "system-a")
public class SystemAProperties {

    private String zone = "Asia/Ho_Chi_Minh";
    private String mainWindowCron = "0 0,30 9,10 * * *";
    private String finalBatchCron = "0 0 11 * * *";
    private String responsePollStartCron = "0 0 9 * * *";
    private String responsePollMainCron = "0 * 9,10 * * *";
    private String responsePollEndCron = "0 0-30 11 * * *";
    private int batchSize = 1;
    private String stagingDirectory = "build/generated/outbound";
    private String responseDirectory = "build/generated/response";
    private String sftpHost = "localhost";
    private int sftpPort = 2022;
    private String sftpUsername = "system-a";
    private String sftpPassword = "SystemA123!";
    private String remoteSendDirectory = "/send";
    private String remoteRecvDirectory = "/recv";
    private String remoteAckDirectory = "/ack";
    private String pgpPassphrase = "LocalPgpPassphrase123!";
}
