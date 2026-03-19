package pes.poc.a.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pes.poc.a.scheduling.SystemAProperties;

@Service
@RequiredArgsConstructor
public class SystemAPgpEncryptionService {

    private final SystemAProperties systemAProperties;

    public Path encrypt(Path plainFile, Path encryptedFile) {
        registerProviderIfNeeded();

        try {
            byte[] plainBytes = Files.readAllBytes(plainFile);
            Files.createDirectories(encryptedFile.getParent());

            try (OutputStream fileOutputStream = Files.newOutputStream(encryptedFile)) {
                PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                        new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                                .setWithIntegrityPacket(true)
                                .setSecureRandom(new SecureRandom())
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                );
                encryptedDataGenerator.addMethod(
                        new JcePBEKeyEncryptionMethodGenerator(systemAProperties.getPgpPassphrase().toCharArray())
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                );

                try (OutputStream encryptedOutputStream = encryptedDataGenerator.open(fileOutputStream, new byte[8192])) {
                    PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
                    try (OutputStream compressedOutputStream = compressedDataGenerator.open(encryptedOutputStream)) {
                        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
                        try (OutputStream literalOutputStream = literalDataGenerator.open(
                                compressedOutputStream,
                                PGPLiteralData.BINARY,
                                plainFile.getFileName().toString(),
                                plainBytes.length,
                                new Date()
                        )) {
                            literalOutputStream.write(plainBytes);
                        }
                    } finally {
                        compressedDataGenerator.close();
                    }
                } finally {
                    encryptedDataGenerator.close();
                }
            }
        } catch (IOException | PGPException exception) {
            throw new IllegalStateException("Failed to encrypt outbound CSV file at " + plainFile, exception);
        }

        return encryptedFile;
    }

    private void registerProviderIfNeeded() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
