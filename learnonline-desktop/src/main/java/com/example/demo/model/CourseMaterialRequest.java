package com.example.demo.model;

/**
 * Request payload for creating course materials.
 *
 * <p>Includes display title, optional type (e.g. PDF, link, video), URL or
 * file path, and optional metadata for the backend.</p>
 */
public class CourseMaterialRequest {
    private String title;
    private String materialType;
    private String url;
    private String metadata;

    public CourseMaterialRequest() {}

    public CourseMaterialRequest(String title, String materialType, String url, String metadata) {
        this.title = title;
        this.materialType = materialType;
        this.url = url;
        this.metadata = metadata;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
