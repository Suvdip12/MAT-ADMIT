package com.admitcard.service;

import com.admitcard.dto.StudentDataDTO;
import com.admitcard.util.PdfGeneratorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdmitCardService {

    @Value("${file.storage.location}")
    private String fileStorageLocation;

    public String generateAdmitCard(StudentDataDTO studentData) throws IOException {
        // 1. Load HTML Template
        ClassPathResource resource = new ClassPathResource("templates/admit-card-template.html");
        byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String htmlContent = new String(bdata, StandardCharsets.UTF_8);

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

        return filePath;
    }
}
