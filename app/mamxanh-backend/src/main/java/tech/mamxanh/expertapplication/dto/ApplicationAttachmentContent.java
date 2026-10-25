package tech.mamxanh.expertapplication.dto;

public final class ApplicationAttachmentContent {
    private final byte[] content;
    private final String contentType;
    private final String filename;

    public ApplicationAttachmentContent(byte[] content, String contentType, String filename) {
        this.content = content;
        this.contentType = contentType;
        this.filename = filename;
    }

    public byte[] content() { return content; }
    public String contentType() { return contentType; }
    public String filename() { return filename; }
}
