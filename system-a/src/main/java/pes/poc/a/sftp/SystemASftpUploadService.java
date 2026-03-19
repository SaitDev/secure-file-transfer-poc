package pes.poc.a.sftp;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import pes.poc.a.scheduling.SystemAProperties;

@Service
@RequiredArgsConstructor
public class SystemASftpUploadService {

    private final SystemAProperties systemAProperties;

    public String uploadOutboundFile(Path localFile) {
        String fileName = localFile.getFileName().toString();
        String remoteDirectory = systemAProperties.getRemoteSendDirectory();
        String remoteFile = remoteDirectory + "/" + fileName;

        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(systemAProperties.getSftpHost(), systemAProperties.getSftpPort());
            sshClient.authPassword(systemAProperties.getSftpUsername(), systemAProperties.getSftpPassword());

            try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
                // SFTPGo users only have upload/rename permission, so avoid post-upload chmod/mtime sync.
                sftpClient.getFileTransfer().setPreserveAttributes(false);
                sftpClient.put(localFile.toString(), remoteFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload outbound CSV file to SFTPGo: " + remoteFile, exception);
        }

        return remoteFile;
    }
}
