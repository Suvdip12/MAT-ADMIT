package com.admitcard.service;

import com.admitcard.dto.StudentDataDTO;
import com.admitcard.model.Template;
import com.admitcard.repository.TemplateRepository;
import com.admitcard.util.PdfGeneratorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdmitCardService {

    private final TemplateRepository templateRepository;
    private final PdfSigningService pdfSigningService;

    @Value("${file.storage.location}")
    private String fileStorageLocation;

    public AdmitCardService(TemplateRepository templateRepository, PdfSigningService pdfSigningService) {
        this.templateRepository = templateRepository;
        this.pdfSigningService = pdfSigningService;
    }

    public String generateAdmitCard(StudentDataDTO studentData) throws IOException {
        // 1. Load HTML Template from Database (Fallback to first template if "Official" not found)
        Template template = templateRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase("Official Admit Card"))
                .findFirst()
                .orElseGet(() -> {
                    return templateRepository.findAll().stream().findFirst().orElse(null);
                });

        if (template == null) {
            throw new IOException("No templates found in database.");
        }
        
        String htmlContent = template.getHtmlContent();

        // 2. Replace Placeholders
        htmlContent = htmlContent.replace("{{name}}", studentData.getName() != null ? studentData.getName() : "");
        htmlContent = htmlContent.replace("{{roll}}", studentData.getRoll() != null ? studentData.getRoll() : "");
        htmlContent = htmlContent.replace("{{reg}}", studentData.getReg() != null ? studentData.getReg() : "");
        htmlContent = htmlContent.replace("{{course}}", studentData.getCourse() != null ? studentData.getCourse() : "");
        htmlContent = htmlContent.replace("{{exam_name}}", studentData.getExamName() != null ? studentData.getExamName() : "");
        htmlContent = htmlContent.replace("{{college_name}}", studentData.getCollegeName() != null ? studentData.getCollegeName() : "");
        htmlContent = htmlContent.replace("{{subject}}", studentData.getSubject() != null ? studentData.getSubject() : "");
        htmlContent = htmlContent.replace("{{date}}", studentData.getDate() != null ? studentData.getDate() : "");
        htmlContent = htmlContent.replace("{{time}}", studentData.getTime() != null ? studentData.getTime() : "");
        htmlContent = htmlContent.replace("{{room}}", studentData.getRoom() != null ? studentData.getRoom() : "");

        // 3. Prepare Output Directory
        File directory = new File(fileStorageLocation);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 4. Generate Unique Filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = studentData.getRoll() != null ? studentData.getRoll().replaceAll("[^a-zA-Z0-9]", "_") : "unknown";
        String filename = "AdmitCard_" + safeName + "_" + timestamp + ".pdf";
        String filePath = fileStorageLocation + File.separator + filename;

        // 5. Generate PDF
        PdfGeneratorUtil.generatePdfFromHtml(htmlContent, filePath);
        try {
            pdfSigningService.signPdfFileInPlace(filePath);
        } catch (Exception exception) {
            throw new IOException("Admit card generated, but digital signing failed: " + exception.getMessage(), exception);
        }

        return filePath;
    }
}
