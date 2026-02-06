package com.admitcard.dto;



public class ApiResponseDTO {
    private boolean success;
    private String message;
    private String downloadUrl;

    public ApiResponseDTO(boolean success, String message, String downloadUrl) {
        this.success = success;
        this.message = message;
        this.downloadUrl = downloadUrl;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
