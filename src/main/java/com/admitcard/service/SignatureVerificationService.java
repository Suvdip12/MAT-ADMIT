package com.admitcard.service;

import com.admitcard.dto.SignatureVerificationResult;
import com.admitcard.dto.VerificationResponseDTO;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

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
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
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
    private static final float ADOBE_VALID_TICK_CENTER_X = 57f;
    private static final float ADOBE_VALID_TICK_CENTER_Y = 31f;
    private static final float ADOBE_VALID_TICK_SIZE = 34f;
    private static final float ADOBE_VALID_TICK_IMAGE_SCALE = 1.84f;
    // tick_valid.png includes transparent padding, so we anchor by visible-pixel center instead of raw image center.
    private static final float ADOBE_VALID_TICK_VISIBLE_CENTER_X = 0.533f;
    private static final float ADOBE_VALID_TICK_VISIBLE_CENTER_Y = 0.544f;
    private static final float ADOBE_INVALID_ICON_X = 75f;
    private static final float ADOBE_INVALID_ICON_Y = 7f;
    private static final float ADOBE_INVALID_ICON_SIZE = 43f;
    private static final String ADOBE_INVALID_ICON_TEXT = "?";

    private final PdfSigningService pdfSigningService;
    private volatile ImageData tickImageData;

    public SignatureVerificationService(PdfSigningService pdfSigningService) {
        this.pdfSigningService = pdfSigningService;
    }

    public SignatureVerificationResult verifySignatures(InputStream pdfInputStream) throws Exception {
        return verifySignatures(pdfInputStream, null);
    }

    public boolean isPasswordRequired(InputStream pdfInputStream) throws Exception {
        try (PdfReader reader = new PdfReader(pdfInputStream);
             PdfDocument ignored = new PdfDocument(reader)) {
            return false;
        } catch (Exception exception) {
            if (isPasswordRelatedError(exception)) {
                return true;
            }
            throw exception;
        }
    }

    public SignatureVerificationResult verifySignatures(InputStream pdfInputStream, byte[] password) throws Exception {
        ensureBouncyCastleProvider();
        SignatureVerificationResult result = new SignatureVerificationResult();
        List<SignatureVerificationResult.SignatureInfo> signatureInfos = new ArrayList<>();
        
        try (PdfReader reader = buildPdfReader(pdfInputStream, password);
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
                    PdfPKCS7 pkcs7 = readSignatureDataWithFallback(signUtil, name);
                    
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
                    info.algorithm = pkcs7.getDigestAlgorithmName();
                    
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
        return verifyAndStampPdf(pdfInputStream, null);
    }

    public byte[] verifyAndStampPdf(InputStream pdfInputStream, byte[] password) throws Exception {
        byte[] inputBytes = pdfInputStream.readAllBytes();
        SignatureVerificationResult verificationResult =
                verifySignatures(new java.io.ByteArrayInputStream(inputBytes), password);

        if (!verificationResult.hasSignatures || verificationResult.signatures.isEmpty()) {
            throw new IllegalStateException("No signature found in this PDF. Stamped PDF is available only for digitally signed documents.");
        }

        Map<String, SignatureVerificationResult.SignatureInfo> signatureInfoByName = new HashMap<>();
        for (SignatureVerificationResult.SignatureInfo signatureInfo : verificationResult.signatures) {
            signatureInfoByName.put(signatureInfo.signatureName, signatureInfo);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfReader sourceReader = buildPdfReader(new java.io.ByteArrayInputStream(inputBytes), password);
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
        float sourceX = rect.getLeft();
        float sourceY = rect.getBottom();
        float sourceWidth = rect.getWidth();
        float sourceHeight = rect.getHeight();

        float maxBadgeScale = 3.0f;
        float width = Math.min(sourceWidth, ADOBE_BADGE_WIDTH * maxBadgeScale);
        float height = Math.min(sourceHeight, ADOBE_BADGE_HEIGHT * maxBadgeScale);
        float x = sourceX + ((sourceWidth - width) / 2f);
        float y = sourceY + ((sourceHeight - height) / 2f);
        boolean isValid = signatureInfo.isValid;

        canvas.saveState();

        float hPadding = width * 0.04f;
        float vPadding = height * 0.04f;
        Rectangle clearingRect = new Rectangle(x - hPadding, y - vPadding, width + hPadding * 2, height + vPadding * 2);

        canvas.setFillColor(new DeviceRgb(255, 255, 255));
        canvas.rectangle(clearingRect);
        canvas.fill();

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont iconFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        float invalidIconX = x + width * (ADOBE_INVALID_ICON_X / ADOBE_BADGE_WIDTH);
        float invalidIconY = y + height * (ADOBE_INVALID_ICON_Y / ADOBE_BADGE_HEIGHT);
        float invalidIconSize = scaleByAdobeReference(width, height, ADOBE_INVALID_ICON_SIZE, ADOBE_INVALID_ICON_SIZE);

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
                    iconFont,
                    invalidIconX,
                    invalidIconY,
                    invalidIconSize
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

        List<String> detailLines = new ArrayList<>();
        detailLines.add("Digitally Signed.");

        String displayName = extractDisplayName(signatureInfo.signerName);
        if (hasDisplayValue(displayName) && !"Unknown".equalsIgnoreCase(displayName)) {
            detailLines.add("Name: " + displayName);
        }
        if (hasDisplayValue(signatureInfo.signingDate)) {
            detailLines.add("Date: " + signatureInfo.signingDate.trim());
        }
        if (hasDisplayValue(signatureInfo.reason)) {
            detailLines.add("Reason: " + signatureInfo.reason.trim());
        }
        if (hasDisplayValue(signatureInfo.location)) {
            detailLines.add("Location: " + signatureInfo.location.trim());
        }

        for (int i = 0; i < detailLines.size(); i++) {
            drawBadgeLine(canvas, font, metaSize, x + leftInset, y + metaBaseline - leading * i, detailLines.get(i));
        }

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

    private boolean hasDisplayValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.isBlank() && !"N/A".equalsIgnoreCase(normalized);
    }

    /**
     * Draws the tick image directly for valid signatures —
     * using the user-provided PNG for a perfect look.
     *
     * @param canvas  target PDF canvas
     * @param cx      horizontal centre of the tick
     * @param cy      vertical centre of the tick (PDF y-up)
     * @param size    half-size / scale factor in points
     */
    private void drawAdobeTick(PdfCanvas canvas, float cx, float cy, float size) {
        try {
            ImageData imageData = getTickImageData();
            float imgWidth = size * ADOBE_VALID_TICK_IMAGE_SCALE;
            float imgHeight = size * ADOBE_VALID_TICK_IMAGE_SCALE;

            // Anchor against the visible glyph center, not the full PNG bounds.
            float x = cx - (imgWidth * ADOBE_VALID_TICK_VISIBLE_CENTER_X);
            float y = cy - (imgHeight * ADOBE_VALID_TICK_VISIBLE_CENTER_Y);

            // iText 8 PdfCanvas uses transformation matrix for scaling
            canvas.addImageWithTransformationMatrix(imageData, imgWidth, 0, 0, imgHeight, x, y, false);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to a simple green circle if image loading fails
            canvas.saveState();
            canvas.setFillColor(new DeviceRgb(30, 190, 70));
            canvas.circle(cx, cy, size * 0.5f);
            canvas.fill();
            canvas.restoreState();
        }
    }

    private ImageData getTickImageData() throws Exception {
        ImageData cached = tickImageData;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (tickImageData == null) {
                try (InputStream tickStream = getClass().getResourceAsStream("/images/tick_valid.png")) {
                    if (tickStream == null) {
                        throw new IllegalStateException("Missing /images/tick_valid.png");
                    }
                    tickImageData = ImageDataFactory.create(StreamUtil.inputStreamToArray(tickStream));
                }
            }
            return tickImageData;
        }
    }

    private void drawAdobeQuestionMark(PdfCanvas canvas, PdfFont boldFont, float cx, float cy, float size) {
        float shadowOffset = Math.max(1.2f, size * 0.04f);
        canvas.saveState();
        canvas.setFillColor(new DeviceRgb(0, 0, 0));
        canvas.beginText().setFontAndSize(boldFont, size).moveText(cx + shadowOffset, cy - shadowOffset).showText(ADOBE_INVALID_ICON_TEXT).endText();
        canvas.setFillColor(new DeviceRgb(255, 220, 0));
        canvas.beginText().setFontAndSize(boldFont, size).moveText(cx, cy).showText(ADOBE_INVALID_ICON_TEXT).endText();
        canvas.restoreState();
    }

    public VerificationResponseDTO verifySingleSignature(InputStream is) throws Exception {
        return verifySingleSignature(is, null);
    }

    public VerificationResponseDTO verifySingleSignature(InputStream is, byte[] password) throws Exception {
        SignatureVerificationResult result = verifySignatures(is, password);
        
        if (!result.hasSignatures || result.signatures.isEmpty()) {
            return new VerificationResponseDTO(
                    false,
                    "No signature found",
                    null,
                    null,
                    null,
                    "Document does not contain any digital signatures."
            );
        }

        SignatureVerificationResult.SignatureInfo sig = selectBestSignature(result.signatures);
        
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
                normalizeOptional(sig.signingDate),
                normalizeOptional(sig.reason),
                normalizeOptional(sig.location),
                sig.message != null ? sig.message : (sig.isValid ? "Signature Valid" : "Signature Not Verified")
        );
    }

    private String normalizeOptional(String value) {
        if (!hasDisplayValue(value)) {
            return null;
        }
        return value.trim();
    }

    private SignatureVerificationResult.SignatureInfo selectBestSignature(
            List<SignatureVerificationResult.SignatureInfo> signatures) {
        for (int i = signatures.size() - 1; i >= 0; i--) {
            SignatureVerificationResult.SignatureInfo info = signatures.get(i);
            if (info.isValid) {
                return info;
            }
        }
        return signatures.get(0);
    }

    private PdfPKCS7 readSignatureDataWithFallback(SignatureUtil signUtil, String name) throws Exception {
        // iText 8 uses its own BouncyCastle abstraction layer (bouncy-castle-adapter).
        // Passing a raw BC provider name conflicts with that abstraction and causes
        // runtime failures. The no-arg overload is correct for iText 8.
        return signUtil.readSignatureData(name);
    }

    private void ensureBouncyCastleProvider() {
        // iText 8's bouncy-castle-adapter handles BC registration internally.
        // No manual provider registration needed.
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

    /**
     * Builds a PdfReader, optionally with a password for encrypted/password-protected PDFs.
     * When password is null or empty, opens the PDF without a password (existing behaviour).
     */
    private PdfReader buildPdfReader(InputStream inputStream, byte[] password) throws java.io.IOException {
        if (password != null && password.length > 0) {
            ReaderProperties props = new ReaderProperties().setPassword(password);
            return new PdfReader(inputStream, props);
        }
        return new PdfReader(inputStream);
    }
}
