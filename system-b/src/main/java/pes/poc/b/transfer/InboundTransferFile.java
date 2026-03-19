package pes.poc.b.transfer;

public record InboundTransferFile(
        InboundTransferFileName fileName,
        String remotePath
) {
}
