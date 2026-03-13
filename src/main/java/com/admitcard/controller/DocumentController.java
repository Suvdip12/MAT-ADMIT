package com.admitcard.controller;

import com.admitcard.dto.ApiResponseDTO;
import com.admitcard.dto.BulkRequestDTO;
import com.admitcard.model.Template;
import com.admitcard.service.DocumentService;
import com.admitcard.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final TemplateService templateService;

    public DocumentController(DocumentService documentService, TemplateService templateService) {
        this.documentService = documentService;
        this.templateService = templateService;
    }

    @Value("${file.storage.location}")
    private String fileStorageLocation;

    @PostMapping("/generate-single")
    public ResponseEntity<ApiResponseDTO> generateSingle(@RequestParam("templateId") Long templateId,
                                                       @RequestParam("type") String type,
                                                       @RequestBody Map<String, String> data) {
        try {
            Template template = templateService.getTemplateById(templateId);
            String processedHtml = documentService.processTemplate(template.getHtmlContent(), data);
            
            String filename = "Document_" + UUID.randomUUID().toString() + (type.equalsIgnoreCase("PDF") ? ".pdf" : ".docx");
            String filePath = fileStorageLocation + File.separator + filename;
            
            new File(fileStorageLocation).mkdirs();
            
            if (type.equalsIgnoreCase("PDF")) {
                documentService.generatePdf(processedHtml, filePath);
            } else {
                documentService.generateWord(processedHtml, filePath);
            }
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Document generated successfully", filename));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponseDTO(false, "Generation failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/generate-bulk")
    public ResponseEntity<ApiResponseDTO> generateBulk(@RequestBody BulkRequestDTO bulkRequest) {
        String sessionId = UUID.randomUUID().toString();
        File sessionDir = new File(fileStorageLocation + File.separator + sessionId);
        sessionDir.mkdirs();
        
        try {
            Template template = templateService.getTemplateById(bulkRequest.getTemplateId());
            String type = bulkRequest.getOutputType();
            
            int count = 0;
            for (Map<String, String> data : bulkRequest.getStudentDataList()) {
                String processedHtml = documentService.processTemplate(template.getHtmlContent(), data);
                String identifier = data.getOrDefault("roll", data.getOrDefault("name", "doc_" + count));
                String filename = identifier.replaceAll("[^a-zA-Z0-9]", "_") + (type.equalsIgnoreCase("PDF") ? ".pdf" : ".docx");
                String filePath = sessionDir.getAbsolutePath() + File.separator + filename;
                
                if (type.equalsIgnoreCase("PDF")) {
                    documentService.generatePdf(processedHtml, filePath);
                } else {
                    documentService.generateWord(processedHtml, filePath);
                }
                count++;
            }
            
            // Create Zip
            String zipFilename = "Bulk_" + sessionId + ".zip";
            String zipPath = fileStorageLocation + File.separator + zipFilename;
            zipDirectory(sessionDir, zipPath);
            
            // Cleanup session dir
            FileUtils.deleteDirectory(sessionDir);
            
            return ResponseEntity.ok(new ApiResponseDTO(true, "Bulk generation completed", zipFilename));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponseDTO(false, "Bulk generation failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) throws IOException {
        Path path = Path.of(fileStorageLocation, filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        
        byte[] data = Files.readAllBytes(path);
        MediaType mediaType = filename.endsWith(".pdf") ? MediaType.APPLICATION_PDF : 
                              filename.endsWith(".zip") ? MediaType.valueOf("application/zip") :
                              MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }

    private void zipDirectory(File folder, String zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        zos.write(bytes, 0, bytes.length);
                        zos.closeEntry();
                    }
                }
            }
        }
    }
}
