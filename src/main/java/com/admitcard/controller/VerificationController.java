package com.admitcard.controller;

import com.admitcard.dto.VerificationResponseDTO;
import com.admitcard.service.SignatureVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
     * POST /api/pdf-password-required
     * Checks whether the uploaded PDF requires a password to open.
     */
    @PostMapping("/pdf-password-required")
    public ResponseEntity<Map<String, Object>> checkPasswordRequirement(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            boolean passwordRequired = verificationService.isPasswordRequired(is);
            return ResponseEntity.ok(Map.of("passwordRequired", passwordRequired));
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("passwordRequired", false);
            response.put("message", resolveErrorMessage(e));
            return ResponseEntity.status(resolveStatus(e)).body(response);
        }
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
            return buildBinaryErrorResponse(e);
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
            return buildBinaryErrorResponse(e);
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
            HttpStatus status = resolveStatus(e);
            VerificationResponseDTO errorResponse = new VerificationResponseDTO(
                    false,
                    "Verification Error",
                    "N/A",
                    "N/A",
                    "N/A",
                    resolveErrorMessage(e));
            return ResponseEntity.status(status).body(errorResponse);
        }
    }

    private ResponseEntity<byte[]> buildBinaryErrorResponse(Exception exception) {
        String message = resolveErrorMessage(exception);
        HttpStatus status = resolveStatus(exception);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

        return ResponseEntity.status(status)
                .headers(headers)
                .body(message.getBytes(StandardCharsets.UTF_8));
    }

    private HttpStatus resolveStatus(Exception exception) {
        if (isPasswordRelatedError(exception) || isNoSignatureError(exception)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveErrorMessage(Exception exception) {
        if (isPasswordRelatedError(exception)) {
            return "This PDF is password-protected. Please provide the correct password and try again.";
        }
        if (isNoSignatureError(exception)) {
            return "No signature found in this PDF. Stamped PDF can be generated only for digitally signed documents.";
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Verification failed due to an unexpected server error.";
        }
        return "Verification failed: " + message;
    }

    private boolean isNoSignatureError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("no signature found")
                        || normalized.contains("does not contain any digital signatures")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
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
