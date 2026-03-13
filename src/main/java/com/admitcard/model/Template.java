package com.admitcard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "templates")
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String htmlContent;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public Template() {}

    public Template(Long id, String name, String htmlContent, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.htmlContent = htmlContent;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TemplateBuilder builder() {
        return new TemplateBuilder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Simple Builder Implementation
    public static class TemplateBuilder {
        private Long id;
        private String name;
        private String htmlContent;
        private String description;

        public TemplateBuilder id(Long id) { this.id = id; return this; }
        public TemplateBuilder name(String name) { this.name = name; return this; }
        public TemplateBuilder htmlContent(String htmlContent) { this.htmlContent = htmlContent; return this; }
        public TemplateBuilder description(String description) { this.description = description; return this; }

        public Template build() {
            Template template = new Template();
            template.setId(id);
            template.setName(name);
            template.setHtmlContent(htmlContent);
            template.setDescription(description);
            return template;
        }
    }
}
