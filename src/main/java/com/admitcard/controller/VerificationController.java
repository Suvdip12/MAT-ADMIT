package com.admitcard.controller;

import com.admitcard.dto.VerificationResponseDTO;
import com.admitcard.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {

    private final com.admitcard.service.SignatureVerificationService verificationService;
    private final com.admitcard.util.PdfReportGenerator reportGenerator;

    public VerificationController(com.admitcard.service.SignatureVerificationService verificationService, 
                                com.admitcard.util.PdfReportGenerator reportGenerator) {
        this.verificationService = verificationService;
        this.reportGenerator = reportGenerator;
    }

    @PostMapping("/verify-pdf")
    public org.springframework.http.ResponseEntity<byte[]> verifyPdf(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream()) {
            com.admitcard.dto.SignatureVerificationResult result = verificationService.verifySignatures(is);
            
            com.admitcard.dto.SignatureVerificationResult.SignatureInfo sig;
            if (!result.hasSignatures || result.signatures.isEmpty()) {
                sig = new com.admitcard.dto.SignatureVerificationResult.SignatureInfo();
                sig.isValid = false;
                sig.signerName = "No Signature Found";
                sig.message = "Document does not contain any digital signatures.";
            } else {
                sig = result.signatures.get(0);
            }
            
            byte[] pdfBytes = reportGenerator.generateVerificationReport(sig);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "verification_report.pdf");
            
            return org.springframework.http.ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/verify-stamp", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public org.springframework.http.ResponseEntity<byte[]> verifyAndStamp(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream()) {
            byte[] stampedPdf = verificationService.verifyAndStampPdf(is);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "stamped_" + file.getOriginalFilename());
            
            return org.springframework.http.ResponseEntity.ok()
                    .headers(headers)
                    .body(stampedPdf);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponseDTO> verify(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            VerificationResponseDTO result = verificationService.verifySingleSignature(is);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
