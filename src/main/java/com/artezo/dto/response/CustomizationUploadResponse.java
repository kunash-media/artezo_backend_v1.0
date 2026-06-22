package com.artezo.dto.response;

/**
 * Returned after successful image upload.
 * Frontend stores assetUuid and sends it with add-customized-to-cart request.
 */
public class CustomizationUploadResponse {
    private String assetUuid;       // UUID to reference this upload
    private String previewUrl;      // URL to show preview in UI
    private String originalFilename;
    private Long fileSizeBytes;

    public CustomizationUploadResponse() {}

    public CustomizationUploadResponse(String assetUuid, String previewUrl,
                                       String originalFilename, Long fileSizeBytes) {
        this.assetUuid = assetUuid;
        this.previewUrl = previewUrl;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getAssetUuid() { return assetUuid; }
    public void setAssetUuid(String assetUuid) { this.assetUuid = assetUuid; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
}