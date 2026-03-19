package pes.poc.a.response;

public record InboundResponseFile(
        InboundResponseFileName fileName,
        String remotePath
) {
}
