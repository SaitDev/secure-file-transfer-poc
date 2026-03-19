package pes.poc.b.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pes.poc.b.config.SystemBProperties;

@Service
@RequiredArgsConstructor
public class SystemBPgpDecryptionService {

    private final SystemBProperties systemBProperties;

    public byte[] decrypt(byte[] encryptedContent, String sourceFileName) {
        registerProviderIfNeeded();

        try (InputStream encryptedInputStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(encryptedContent))) {
            JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(encryptedInputStream);
            PGPEncryptedDataList encryptedDataList = readEncryptedDataList(pgpObjectFactory);
            PGPPBEEncryptedData encryptedData = readPasswordEncryptedData(encryptedDataList);

            InputStream clearDataStream = encryptedData.getDataStream(
                    new JcePBEDataDecryptorFactoryBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(systemBProperties.getPgpPassphrase().toCharArray())
            );

            try (InputStream decryptedInputStream = clearDataStream) {
                byte[] plainBytes = readLiteralData(decryptedInputStream);
                if (encryptedData.isIntegrityProtected() && !encryptedData.verify()) {
                    throw new IllegalStateException("PGP integrity check failed for " + sourceFileName);
                }
                return plainBytes;
            }
        } catch (IOException | PGPException exception) {
            throw new IllegalStateException("Failed to decrypt inbound file " + sourceFileName, exception);
        }
    }

    private PGPEncryptedDataList readEncryptedDataList(JcaPGPObjectFactory pgpObjectFactory) throws IOException {
        Object candidate = pgpObjectFactory.nextObject();
        if (candidate instanceof PGPEncryptedDataList encryptedDataList) {
            return encryptedDataList;
        }

        Object nextCandidate = pgpObjectFactory.nextObject();
        if (nextCandidate instanceof PGPEncryptedDataList encryptedDataList) {
            return encryptedDataList;
        }

        throw new IllegalStateException("Inbound PGP payload does not contain encrypted data");
    }

    private PGPPBEEncryptedData readPasswordEncryptedData(PGPEncryptedDataList encryptedDataList) {
        Iterator<?> iterator = encryptedDataList.getEncryptedDataObjects();
        while (iterator.hasNext()) {
            Object candidate = iterator.next();
            if (candidate instanceof PGPPBEEncryptedData encryptedData) {
                return encryptedData;
            }
        }
        throw new IllegalStateException("Inbound PGP payload is not encrypted with a passphrase");
    }

    private byte[] readLiteralData(InputStream decryptedInputStream) throws IOException, PGPException {
        JcaPGPObjectFactory clearObjectFactory = new JcaPGPObjectFactory(decryptedInputStream);
        Object message = clearObjectFactory.nextObject();
        return readLiteralData(message);
    }

    private byte[] readLiteralData(Object message) throws IOException, PGPException {
        if (message instanceof PGPCompressedData compressedData) {
            try (InputStream compressedInputStream = compressedData.getDataStream()) {
                JcaPGPObjectFactory clearObjectFactory = new JcaPGPObjectFactory(compressedInputStream);
                return readLiteralData(clearObjectFactory.nextObject());
            }
        }
        if (!(message instanceof PGPLiteralData literalData)) {
            throw new IllegalStateException("Inbound PGP payload does not contain literal CSV data");
        }

        try (InputStream literalInputStream = literalData.getInputStream()) {
            return literalInputStream.readAllBytes();
        }
    }

    private void registerProviderIfNeeded() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
