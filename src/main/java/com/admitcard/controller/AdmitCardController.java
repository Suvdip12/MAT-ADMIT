package com.admitcard.controller;

import com.admitcard.dto.ApiResponseDTO;
import com.admitcard.dto.StudentDataDTO;
import com.admitcard.service.AdmitCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admitcard")
@CrossOrigin(origins = "*") // Allow requests from any frontend
public class AdmitCardController {

    @Autowired
    private AdmitCardService admitCardService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDTO> generateAdmitCard(@RequestBody StudentDataDTO studentData) {
        try {
            String filePath = admitCardService.generateAdmitCard(studentData);
            String message = "Admit Card generated successfully!";
            // In a real app, this would be a downloadable URL. 
            // For this local example, we assume the user can access the local file system or we return the path.
            // We'll return the path relative to the project root for simplicity in this context.
            
            return ResponseEntity.ok(new ApiResponseDTO(true, message, filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponseDTO(false, "Failed to generate Admit Card: " + e.getMessage(), null));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadAdmitCard(@RequestParam("file") String filePath) {
        try {
            // Normalize the file path
            Path path = Paths.get(filePath).normalize();
            File file = path.toFile();
            
            // Check if file exists
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // Create resource from file
            Resource resource = new FileSystemResource(file);
            
            // Set headers for PDF viewing in browser
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", file.getName());
            headers.setContentLength(file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
