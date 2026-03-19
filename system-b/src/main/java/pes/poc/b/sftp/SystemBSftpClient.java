package pes.poc.b.sftp;

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
import pes.poc.b.config.SystemBProperties;
import pes.poc.b.transfer.InboundTransferFile;
import pes.poc.b.transfer.InboundTransferFileName;

@Service
@RequiredArgsConstructor
public class SystemBSftpClient {

    private final SystemBProperties systemBProperties;

    public List<InboundTransferFile> listInboundFiles() {
        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            return sftpClient.ls(systemBProperties.getRemoteRecvDirectory()).stream()
                    .map(RemoteResourceInfo::getName)
                    .map(InboundTransferFileName::parse)
                    .flatMap(java.util.Optional::stream)
                    .map(fileName -> new InboundTransferFile(
                            fileName,
                            buildRemotePath(systemBProperties.getRemoteRecvDirectory(), fileName.fileName())
                    ))
                    .sorted(Comparator.comparing(file -> file.fileName().fileName()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list inbound files from SFTPGo", exception);
        }
    }

    public void download(String remotePath, Path localPath) {
        try {
            Files.createDirectories(localPath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create local staging directory for " + localPath, exception);
        }

        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.get(remotePath, localPath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to download inbound file " + remotePath, exception);
        }
    }

    public void uploadAckFile(String ackFileName, String content) {
        uploadTextFile(systemBProperties.getRemoteAckDirectory(), ackFileName, content, "download ACK");
    }

    public void uploadResultFile(String resultFileName, String content) {
        uploadTextFile(systemBProperties.getRemoteSendDirectory(), resultFileName, content, "process result");
    }

    private SSHClient newSshClient() throws IOException {
        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(systemBProperties.getSftpHost(), systemBProperties.getSftpPort());
        sshClient.authPassword(systemBProperties.getSftpUsername(), systemBProperties.getSftpPassword());
        return sshClient;
    }

    private String buildRemotePath(String directory, String fileName) {
        if (directory.endsWith("/")) {
            return directory + fileName;
        }
        return directory + "/" + fileName;
    }

    private void uploadTextFile(String remoteDirectory, String fileName, String content, String fileTypeLabel) {
        String remoteFile = buildRemotePath(remoteDirectory, fileName);
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("system-b-" + fileTypeLabel.replace(' ', '-') + "-", ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to stage " + fileTypeLabel + " content for " + fileName, exception);
        }

        try (SSHClient sshClient = newSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            // SFTPGo users only have upload/rename permission, so avoid post-upload chmod/mtime sync.
            sftpClient.getFileTransfer().setPreserveAttributes(false);
            sftpClient.put(tempFile.toString(), remoteFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload " + fileTypeLabel + " file " + remoteFile, exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Best effort cleanup for the local temp file.
                }
            }
        }
    }
}
