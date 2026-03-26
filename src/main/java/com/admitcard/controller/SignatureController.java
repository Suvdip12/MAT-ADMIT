package com.admitcard.controller;

import com.admitcard.dto.SignatureVerificationResult;
import com.admitcard.service.SignatureVerificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SignatureController {

    private final SignatureVerificationService verificationService;

    public SignatureController(SignatureVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify-signature")
    public ResponseEntity<Object> verifySignature(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            SignatureVerificationResult result = verificationService.verifySignatures(is, pwdBytes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(resolveStatus(e)).body(resolveErrorMessage(e));
        }
    }

    @PostMapping(value = "/verify-signature/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> verifySignaturePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            SignatureVerificationResult result = verificationService.verifySignatures(is, pwdBytes);
            byte[] pdfBytes = verificationService.generateVerificationReportPdf(result);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "verification_report.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return buildBinaryErrorResponse(e);
        }
    }

    @PostMapping(value = "/verify-and-stamp", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> verifyAndStamp(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        try (InputStream is = file.getInputStream()) {
            byte[] pwdBytes = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
            byte[] stampedPdf = verificationService.verifyAndStampPdf(is, pwdBytes);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "verified_" + file.getOriginalFilename());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stampedPdf);
        } catch (Exception e) {
            e.printStackTrace();
            return buildBinaryErrorResponse(e);
        }
    }

    private ResponseEntity<byte[]> buildBinaryErrorResponse(Exception exception) {
        String message = resolveErrorMessage(exception);
        HttpStatus status = resolveStatus(exception);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return ResponseEntity.status(status)
                .headers(headers)
                .body(message.getBytes(StandardCharsets.UTF_8));
    }

    private HttpStatus resolveStatus(Exception exception) {
        return isPasswordRelatedError(exception) ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveErrorMessage(Exception exception) {
        if (isPasswordRelatedError(exception)) {
            return "This PDF is password-protected. Please provide the correct password and try again.";
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Verification failed due to an unexpected server error.";
        }
        return "Verification failed: " + message;
    }

    private boolean isPasswordRelatedError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("bad user password")
                        || normalized.contains("bad owner password")
                        || normalized.contains("invalid password")
                        || normalized.contains("owner password")
                        || normalized.contains("password is required")
                        || (normalized.contains("password") && normalized.contains("encrypted"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
