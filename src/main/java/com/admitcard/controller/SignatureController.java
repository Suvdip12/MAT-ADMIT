package com.admitcard.controller;

import com.admitcard.dto.ApiResponseDTO;
import com.admitcard.dto.SignatureVerificationResult;
import com.admitcard.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SignatureController {

    private final SignatureVerificationService verificationService;

    public SignatureController(SignatureVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify-signature")
    public ResponseEntity<Object> verifySignature(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            SignatureVerificationResult result = verificationService.verifySignatures(is);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Verification failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "/verify-signature/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> verifySignaturePdf(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            SignatureVerificationResult result = verificationService.verifySignatures(is);
            byte[] pdfBytes = verificationService.generateVerificationReportPdf(result);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "verification_report.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/verify-and-stamp", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> verifyAndStamp(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] stampedPdf = verificationService.verifyAndStampPdf(is);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "verified_" + file.getOriginalFilename());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stampedPdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
