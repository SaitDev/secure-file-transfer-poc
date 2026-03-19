package pes.poc.a.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import pes.poc.a.scheduling.SystemAProperties;

@Service
@RequiredArgsConstructor
public class SystemAResponseSftpClient {

    private final SystemAProperties systemAProperties;

    public List<InboundResponseFile> listResponseFiles() {
        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            return sftpClient.ls(systemAProperties.getRemoteRecvDirectory()).stream()
                    .map(RemoteResourceInfo::getName)
                    .map(InboundResponseFileName::parse)
                    .flatMap(java.util.Optional::stream)
                    .map(fileName -> new InboundResponseFile(
                            fileName,
                            buildRemotePath(systemAProperties.getRemoteRecvDirectory(), fileName.fileName())
                    ))
                    .sorted(Comparator.comparing(file -> file.fileName().fileName()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list response files from SFTPGo", exception);
        }
    }

    public void download(String remotePath, Path localPath) {
        try {
            Files.createDirectories(localPath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create local response directory for " + localPath, exception);
        }

        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.get(remotePath, localPath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download response file " + remotePath, exception);
        }
    }

    public void uploadDownloadAck(String ackFileName, String content) {
        String remoteFile = buildRemotePath(systemAProperties.getRemoteAckDirectory(), ackFileName);
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("system-a-response-ack-", ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to stage response download ACK content for " + ackFileName, exception);
        }

        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.getFileTransfer().setPreserveAttributes(false);
            sftpClient.put(tempFile.toString(), remoteFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload response download ACK file " + remoteFile, exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Best effort cleanup for the local temp ACK file.
                }
            }
        }
    }

    private SSHClient newSshClient() throws IOException {
        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(systemAProperties.getSftpHost(), systemAProperties.getSftpPort());
        KeyProvider keyProvider = loadKeyProvider(sshClient);
        sshClient.authPublickey(systemAProperties.getSftpUsername(), keyProvider);
        return sshClient;
    }

    private KeyProvider loadKeyProvider(SSHClient sshClient) throws IOException {
        String privateKeyPassphrase = systemAProperties.getSftpPrivateKeyPassphrase();
        if (privateKeyPassphrase == null || privateKeyPassphrase.isBlank()) {
            return sshClient.loadKeys(systemAProperties.getSftpPrivateKeyPath());
        }
        return sshClient.loadKeys(systemAProperties.getSftpPrivateKeyPath(), privateKeyPassphrase);
    }

    private String buildRemotePath(String directory, String fileName) {
        if (directory.endsWith("/")) {
            return directory + fileName;
        }
        return directory + "/" + fileName;
    }
}
