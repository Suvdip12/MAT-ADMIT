package com.admitcard.controller;

import com.admitcard.dto.VerificationResponseDTO;
import com.admitcard.service.SignatureVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {

    private final SignatureVerificationService verificationService;
    private final com.admitcard.util.PdfReportGenerator reportGenerator;

    public VerificationController(SignatureVerificationService verificationService,
            com.admitcard.util.PdfReportGenerator reportGenerator) {
        this.verificationService = verificationService;
        this.reportGenerator = reportGenerator;
    }

    /**
     * POST /api/verify-pdf
     * Verifies signatures and returns a styled PDF report.
     * @param password optional, required only for password-protected PDFs
     */
    @PostMapping("/verify-pdf")
    public ResponseEntity<byte[]> verifyPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            com.admitcard.dto.SignatureVerificationResult result = verificationService.verifySignatures(is, pwdBytes);

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

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/verify-stamp
     * Verifies signatures and returns the original PDF with a stamped badge.
     * @param password optional, required only for password-protected PDFs
     */
    @PostMapping(value = "/verify-stamp", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> verifyAndStamp(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            byte[] stampedPdf = verificationService.verifyAndStampPdf(is, pwdBytes);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "stamped_" + file.getOriginalFilename());

            return ResponseEntity.ok().headers(headers).body(stampedPdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/verify
     * Returns a JSON verification result for the first/best signature found.
     * @param password optional, required only for password-protected PDFs
     */
    @PostMapping("/verify")
    public ResponseEntity<VerificationResponseDTO> verify(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            VerificationResponseDTO result = verificationService.verifySingleSignature(is, pwdBytes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            VerificationResponseDTO errorResponse = new VerificationResponseDTO(
                    false,
                    "Verification Error",
                    "N/A",
                    "N/A",
                    "N/A",
                    "Verification failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
