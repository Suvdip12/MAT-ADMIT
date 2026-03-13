package com.admitcard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generation_sessions")
public class AdmitCardSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionName;
    
    private String status; // PENDING, COMPLETED, FAILED
    
    private String outputType; // PDF, WORD
    
    private String zipFilePath;
    
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "template_id")
    private Template template;
    
    public AdmitCardSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public String getZipFilePath() { return zipFilePath; }
    public void setZipFilePath(String zipFilePath) { this.zipFilePath = zipFilePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public static AdmitCardSessionBuilder builder() {
        return new AdmitCardSessionBuilder();
    }

    public static class AdmitCardSessionBuilder {
        private String sessionName;
        private String status;
        private String outputType;
        private Template template;

        public AdmitCardSessionBuilder sessionName(String sessionName) { this.sessionName = sessionName; return this; }
        public AdmitCardSessionBuilder status(String status) { this.status = status; return this; }
        public AdmitCardSessionBuilder outputType(String outputType) { this.outputType = outputType; return this; }
        public AdmitCardSessionBuilder template(Template template) { this.template = template; return this; }

        public AdmitCardSession build() {
            AdmitCardSession session = new AdmitCardSession();
            session.setSessionName(sessionName);
            session.setStatus(status);
            session.setOutputType(outputType);
            session.setTemplate(template);
            return session;
        }
    }
}
