package com.admitcard.service;

import com.admitcard.dto.SignatureVerificationResult;
import com.admitcard.dto.VerificationResponseDTO;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignatureUtil;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SignatureVerificationService {

    private static final SimpleDateFormat BADGE_DATE_FORMAT =
            new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final float ADOBE_BADGE_WIDTH = 120f;
    private static final float ADOBE_BADGE_HEIGHT = 60f;
    private static final float ADOBE_LEFT_INSET = 2f;
    private static final float ADOBE_TITLE_FONT = 11.8f;
    private static final float ADOBE_TITLE_BASELINE = 46.4f;
    private static final float ADOBE_META_FONT = 7.6f;
    private static final float ADOBE_META_BASELINE = 32.4f;
    private static final float ADOBE_META_LEADING = 7.6f;
    private static final float ADOBE_VALID_TICK_CENTER_X = 84f;
    private static final float ADOBE_VALID_TICK_CENTER_Y = 32f;
    private static final float ADOBE_VALID_TICK_SIZE = 39f;
    private static final float ADOBE_INVALID_ICON_X = 75f;
    private static final float ADOBE_INVALID_ICON_Y = 7f;
    private static final float ADOBE_INVALID_ICON_SIZE = 43f;

    private final PdfSigningService pdfSigningService;

    public SignatureVerificationService(PdfSigningService pdfSigningService) {
        this.pdfSigningService = pdfSigningService;
    }

    public SignatureVerificationResult verifySignatures(InputStream pdfInputStream) throws Exception {
        SignatureVerificationResult result = new SignatureVerificationResult();
        List<SignatureVerificationResult.SignatureInfo> signatureInfos = new ArrayList<>();
        
        try (PdfReader reader = new PdfReader(pdfInputStream);
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            
            SignatureUtil signUtil = new SignatureUtil(pdfDoc);
            List<String> names = signUtil.getSignatureNames();
            
            if (names.isEmpty()) {
                result.hasSignatures = false;
                result.signatures = new ArrayList<>();
                return result;
            }
            
            result.hasSignatures = true;
            
            for (String name : names) {
                SignatureVerificationResult.SignatureInfo info = new SignatureVerificationResult.SignatureInfo();
                info.signatureName = name;
                
                try {
                    PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);
                    
                    // Verify integrity and authenticity
                    boolean isValid = pkcs7.verifySignatureIntegrityAndAuthenticity();
                    info.isValid = isValid;
                    
                    // Extract Signer Name with null check
                    if (pkcs7.getSigningCertificate() != null) {
                        info.signerName = pkcs7.getSigningCertificate().getSubjectDN().getName();
                    } else {
                        info.signerName = "Unknown (Certificate missing)";
                    }
                    
                    // Extract Date
                    Calendar cal = pkcs7.getSignDate();
                    if (cal != null) {
                        info.signingDate = BADGE_DATE_FORMAT.format(cal.getTime());
                    }
                    
                    info.message = isValid ? "Signature Valid" : "Signature Invalid (Integrity/Authenticity check failed)";
                    
                    // Detailed extraction
                    info.reason = pkcs7.getReason();
                    info.location = pkcs7.getLocation();
                    info.algorithm = pkcs7.getHashAlgorithm();
                    
                    if (pkcs7.getSigningCertificate() != null) {
                        info.certificateAuthority = pkcs7.getSigningCertificate().getIssuerDN().getName();
                        if (info.certificateAuthority.contains("CN=")) {
                             info.certificateAuthority = info.certificateAuthority.substring(info.certificateAuthority.indexOf("CN=") + 3);
                             if (info.certificateAuthority.contains(",")) info.certificateAuthority = info.certificateAuthority.substring(0, info.certificateAuthority.indexOf(","));
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    info.isValid = false;
                    info.message = "Verification Error: " + e.getMessage();
                    if (info.signerName == null) info.signerName = "Error";
                }
                
                signatureInfos.add(info);
            }
            
            result.signatures = signatureInfos;
        }
        
        return result;
    }

    public byte[] generateVerificationReportPdf(SignatureVerificationResult result) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; padding: 40px; color: #1f2937; line-height: 1.6; }");
        html.append(".header { border-bottom: 3px solid #6366f1; margin-bottom: 30px; padding-bottom: 20px; text-align: center; }");
        html.append(".title { font-size: 28px; font-weight: bold; color: #4338ca; margin: 0; }");
        html.append(".subtitle { color: #6b7280; font-size: 14px; margin-top: 5px; }");
        html.append(".signature-box { border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; margin-bottom: 20px; background-color: #f9fafb; }");
        html.append(".signature-name { font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #111827; }");
        html.append(".valid { color: #059669; font-weight: bold; background: #ecfdf5; padding: 4px 8px; border-radius: 4px; }");
        html.append(".invalid { color: #dc2626; font-weight: bold; background: #fef2f2; padding: 4px 8px; border-radius: 4px; }");
        html.append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; padding-top: 20px; }");
        html.append("</style></head><body>");
        
        html.append("<div class='header'>");
        html.append("<div class='title'>Digital Signature Verification Report</div>");
        html.append("<div class='subtitle'>Generated by AdmitPro Security Service</div>");
        html.append("</div>");
        
        if (!result.hasSignatures) {
            html.append("<div style='text-align: center; padding: 40px;'>");
            html.append("<p style='font-size: 18px; color: #d97706;'>No digital signatures found in this document.</p>");
            html.append("</div>");
        } else {
            for (SignatureVerificationResult.SignatureInfo sig : result.signatures) {
                html.append("<div class='signature-box'>");
                html.append("<div class='signature-name'>Signature: ").append(sig.signatureName).append("</div>");
                html.append("<p><strong>Signer identity:</strong> ").append(sig.signerName).append("</p>");
                html.append("<p><strong>Signing Date:</strong> ").append(sig.signingDate != null ? sig.signingDate : "N/A").append("</p>");
                html.append("<p><strong>Verification Status:</strong> <span class='").append(sig.isValid ? "valid" : "invalid").append("'>")
                    .append(sig.message).append("</span></p>");
                html.append("</div>");
            }
        }
        
        html.append("<div class='footer'>");
        html.append("<p>This is an automatically generated report confirming the status of digital signatures found within the uploaded document.</p>");
        html.append("<p>&copy; 2024 AdmitPro Generator. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</body></html>");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html.toString(), baos);
        return pdfSigningService.signPdfInvisiblyIfEnabled(baos.toByteArray());
    }

    public byte[] verifyAndStampPdf(InputStream pdfInputStream) throws Exception {
        byte[] inputBytes = pdfInputStream.readAllBytes();
        SignatureVerificationResult verificationResult =
                verifySignatures(new java.io.ByteArrayInputStream(inputBytes));

        Map<String, SignatureVerificationResult.SignatureInfo> signatureInfoByName = new HashMap<>();
        for (SignatureVerificationResult.SignatureInfo signatureInfo : verificationResult.signatures) {
            signatureInfoByName.put(signatureInfo.signatureName, signatureInfo);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfReader sourceReader = new PdfReader(new java.io.ByteArrayInputStream(inputBytes));
             PdfDocument sourcePdf = new PdfDocument(sourceReader);
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument stampedPdf = new PdfDocument(writer)) {

            SignatureUtil signUtil = new SignatureUtil(sourcePdf);
            List<String> names = signUtil.getSignatureNames();
            PdfAcroForm form = PdfAcroForm.getAcroForm(sourcePdf, false);

            copyPagesWithoutInteractiveSignatures(sourcePdf, stampedPdf);

            List<RectangleWithPage> stampedAreas = new ArrayList<>();

            for (int i = names.size() - 1; i >= 0; i--) {
                String name = names.get(i);

                PdfFormField field = form == null ? null : form.getField(name);
                if (field == null || field.getWidgets().isEmpty()) {
                    continue;
                }

                Rectangle rect = field.getWidgets().get(0).getRectangle().toRectangle();
                int pageNum = sourcePdf.getPageNumber(field.getWidgets().get(0).getPage());

                if (isAlreadyStamped(pageNum, rect, stampedAreas)) {
                    continue;
                }

                SignatureVerificationResult.SignatureInfo signatureInfo =
                        signatureInfoByName.getOrDefault(name, unreadableSignature(name));

                PdfCanvas canvas = new PdfCanvas(stampedPdf.getPage(pageNum));
                drawStatusBadge(canvas, rect, signatureInfo);
                stampedAreas.add(new RectangleWithPage(pageNum, rect));
            }
        }

        return pdfSigningService.signPdfInvisiblyIfEnabled(baos.toByteArray());
    }

    private void copyPagesWithoutInteractiveSignatures(PdfDocument sourcePdf, PdfDocument stampedPdf) throws java.io.IOException {
        for (int pageNumber = 1; pageNumber <= sourcePdf.getNumberOfPages(); pageNumber++) {
            PdfPage sourcePage = sourcePdf.getPage(pageNumber);
            PdfPage targetPage = stampedPdf.addNewPage(new PageSize(sourcePage.getPageSize()));
            PdfFormXObject pageCopy = sourcePage.copyAsFormXObject(stampedPdf);
            new PdfCanvas(targetPage).addXObjectAt(pageCopy, 0, 0);
        }
    }

    private SignatureVerificationResult.SignatureInfo unreadableSignature(String signatureName) {
        SignatureVerificationResult.SignatureInfo info = new SignatureVerificationResult.SignatureInfo();
        info.signatureName = signatureName;
        info.isValid = false;
        info.signerName = "Unknown";
        info.message = "Signature Not Verified";
        info.reason = "N/A";
        info.location = "N/A";
        return info;
    }

    private static class RectangleWithPage {
        int page;
        Rectangle rect;
        RectangleWithPage(int page, Rectangle rect) { this.page = page; this.rect = rect; }
    }

    private boolean isAlreadyStamped(int page, Rectangle rect, List<RectangleWithPage> stampedAreas) {
        for (RectangleWithPage stamped : stampedAreas) {
            if (stamped.page == page) {
                // Fuzzy check: if centers are close, it's the same signature field
                float dx = Math.abs(stamped.rect.getLeft() - rect.getLeft());
                float dy = Math.abs(stamped.rect.getBottom() - rect.getBottom());
                if (dx < 10 && dy < 10) return true;
            }
        }
        return false;
    }

    private void drawStatusBadge(PdfCanvas canvas, Rectangle rect, SignatureVerificationResult.SignatureInfo signatureInfo) throws Exception {
        float x = rect.getLeft();
        float y = rect.getBottom();
        float width = rect.getWidth();
        float height = rect.getHeight();
        boolean isValid = signatureInfo.isValid;

        canvas.saveState();

        float hPadding = width * 0.04f;
        float vPadding = height * 0.04f;
        Rectangle clearingRect = new Rectangle(x - hPadding, y - vPadding, width + hPadding * 2, height + vPadding * 2);

        canvas.setFillColor(new DeviceRgb(255, 255, 255));
        canvas.rectangle(clearingRect);
        canvas.fill();

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        if (isValid) {
            drawAdobeTick(
                    canvas,
                    x + width * (ADOBE_VALID_TICK_CENTER_X / ADOBE_BADGE_WIDTH),
                    y + height * (ADOBE_VALID_TICK_CENTER_Y / ADOBE_BADGE_HEIGHT),
                    scaleByAdobeReference(width, height, ADOBE_VALID_TICK_SIZE, ADOBE_VALID_TICK_SIZE)
            );
        } else {
            drawAdobeQuestionMark(
                    canvas,
                    x + width * (ADOBE_INVALID_ICON_X / ADOBE_BADGE_WIDTH),
                    y + height * (ADOBE_INVALID_ICON_Y / ADOBE_BADGE_HEIGHT),
                    scaleByAdobeReference(width, height, ADOBE_INVALID_ICON_SIZE, ADOBE_INVALID_ICON_SIZE)
            );
        }

        float leftInset = scaleByAdobeReference(width, height, ADOBE_LEFT_INSET, ADOBE_LEFT_INSET);
        float headerSize = scaleByAdobeReference(width, height, ADOBE_TITLE_FONT, ADOBE_TITLE_FONT);
        canvas.beginText()
            .setFontAndSize(font, headerSize)
            .setFillColor(new DeviceRgb(0, 0, 0))
            .moveText(x + leftInset, y + scaleByAdobeReference(width, height, ADOBE_TITLE_BASELINE, ADOBE_TITLE_BASELINE))
            .showText(isValid ? "Signature valid" : "Signature Not Verified")
            .endText();

        float metaSize = scaleByAdobeReference(width, height, ADOBE_META_FONT, ADOBE_META_FONT);
        float metaBaseline = scaleByAdobeReference(width, height, ADOBE_META_BASELINE, ADOBE_META_BASELINE);
        float leading = scaleByAdobeReference(width, height, ADOBE_META_LEADING, ADOBE_META_LEADING);

        drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline, "Digitally Signed.");
        drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline - leading, "Name: " + extractDisplayName(signatureInfo.signerName));
        drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline - leading * 2, "Date: " + safeDisplayValue(signatureInfo.signingDate));
        drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline - leading * 3, "Reason: " + safeDisplayValue(signatureInfo.reason));
        drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline - leading * 4, "Location: " + safeDisplayValue(signatureInfo.location));

        canvas.restoreState();
    }

    private float scaleByAdobeReference(float width, float height, float baseWidthValue, float baseHeightValue) {
        float widthScale = width / ADOBE_BADGE_WIDTH;
        float heightScale = height / ADOBE_BADGE_HEIGHT;
        return (baseWidthValue * widthScale + baseHeightValue * heightScale) / 2f;
    }

    private void drawBadgeLine(PdfCanvas canvas, PdfFont font, float fontSize, float x, float y, String text) {
        canvas.beginText()
            .setFontAndSize(font, fontSize)
            .setFillColor(new DeviceRgb(0, 0, 0))
            .moveText(x, y)
            .showText(text)
            .endText();
    }

    private String extractDisplayName(String signerName) {
        if (signerName == null || signerName.isBlank()) {
            return "Unknown";
        }

        if (signerName.contains("CN=")) {
            String commonName = signerName.substring(signerName.indexOf("CN=") + 3);
            if (commonName.contains(",")) {
                return commonName.substring(0, commonName.indexOf(","));
            }
            return commonName;
        }

        return signerName;
    }

    private String safeDisplayValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void drawAdobeTick(PdfCanvas canvas, float cx, float cy, float size) {
        canvas.saveState();
        canvas.setLineCapStyle(PdfCanvasConstants.LineCapStyle.PROJECTING_SQUARE);
        canvas.setLineJoinStyle(PdfCanvasConstants.LineJoinStyle.MITER);

        canvas.setStrokeColor(new DeviceRgb(0, 0, 0));
        canvas.setLineWidth(size * 0.32f);
        drawTickStroke(canvas, cx, cy, size);
        canvas.stroke();

        canvas.setStrokeColor(new DeviceRgb(0, 184, 84));
        canvas.setLineWidth(size * 0.22f);
        drawTickStroke(canvas, cx, cy, size);
        canvas.stroke();
        canvas.restoreState();
    }

    private void drawTickStroke(PdfCanvas canvas, float x, float y, float s) {
        canvas.moveTo(x - s * 0.38f, y - s * 0.03f);
        canvas.lineTo(x - s * 0.12f, y - s * 0.32f);
        canvas.lineTo(x + s * 0.34f, y + s * 0.32f);
    }

    private void drawAdobeQuestionMark(PdfCanvas canvas, float cx, float cy, float size) throws Exception {
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        float shadowOffset = Math.max(1.2f, size * 0.04f);
        canvas.saveState();
        canvas.setFillColor(new DeviceRgb(0, 0, 0));
        canvas.beginText().setFontAndSize(boldFont, size).moveText(cx + shadowOffset, cy - shadowOffset).showText("?").endText();
        canvas.setFillColor(new DeviceRgb(255, 220, 0));
        canvas.beginText().setFontAndSize(boldFont, size).moveText(cx, cy).showText("?").endText();
        canvas.restoreState();
    }

    public VerificationResponseDTO verifySingleSignature(InputStream is) throws Exception {
        SignatureVerificationResult result = verifySignatures(is);
        
        if (!result.hasSignatures || result.signatures.isEmpty()) {
            return new VerificationResponseDTO(
                    false,
                    "No signature found",
                    "N/A",
                    "N/A",
                    "N/A"
            );
        }

        SignatureVerificationResult.SignatureInfo sig = result.signatures.get(0);
        
        // Refine signer name for display
        String displayName = sig.signerName;
        if (displayName != null && displayName.contains("CN=")) {
            displayName = displayName.substring(displayName.indexOf("CN=") + 3);
            if (displayName.contains(",")) {
                displayName = displayName.substring(0, displayName.indexOf(","));
            }
        }

        return new VerificationResponseDTO(
                sig.isValid,
                displayName,
                sig.signingDate,
                sig.reason != null ? sig.reason : "N/A",
                sig.location != null ? sig.location : "N/A"
        );
    }
}
