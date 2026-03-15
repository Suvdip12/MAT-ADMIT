package com.admitcard.dto;

public class VerificationResponseDTO {
    private boolean valid;
    private String name;
    private String date;
    private String reason;
    private String location;

    public VerificationResponseDTO() {}

    public VerificationResponseDTO(boolean valid, String name, String date, String reason, String location) {
        this.valid = valid;
        this.name = name;
        this.date = date;
        this.reason = reason;
        this.location = location;
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
