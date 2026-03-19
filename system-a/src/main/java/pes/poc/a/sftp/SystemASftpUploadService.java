package pes.poc.a.sftp;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
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
            KeyProvider keyProvider = loadKeyProvider(sshClient);
            sshClient.authPublickey(systemAProperties.getSftpUsername(), keyProvider);

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

    private KeyProvider loadKeyProvider(SSHClient sshClient) throws IOException {
        String privateKeyPassphrase = systemAProperties.getSftpPrivateKeyPassphrase();
        if (privateKeyPassphrase == null || privateKeyPassphrase.isBlank()) {
            return sshClient.loadKeys(systemAProperties.getSftpPrivateKeyPath());
        }
        return sshClient.loadKeys(systemAProperties.getSftpPrivateKeyPath(), privateKeyPassphrase);
    }
}
