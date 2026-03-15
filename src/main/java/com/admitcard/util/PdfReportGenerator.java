package com.admitcard.util;

import com.admitcard.dto.SignatureVerificationResult;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfReportGenerator {

    public byte[] generateVerificationReport(SignatureVerificationResult.SignatureInfo sig) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Header
        String title = sig.isValid ? "Signature Valid" : "Signature Not Verified";
        DeviceRgb color = sig.isValid ? new DeviceRgb(0, 165, 80) : new DeviceRgb(220, 38, 38);

        Paragraph header = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(header);

        // Big Icon
        float centerX = PageSize.A4.getWidth() / 2;
        float centerY = PageSize.A4.getHeight() - 150;
        PdfCanvas canvas = new PdfCanvas(pdf.getPage(1));
        
        if (sig.isValid) {
            drawBigTick(canvas, centerX, centerY, 60);
        } else {
            drawBigCross(canvas, centerX, centerY, 60);
        }

        document.add(new Paragraph("\n\n\n\n\n\n")); // Spacer for the icon area

        // Subtitle
        document.add(new Paragraph("Digitally Signed")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Details Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        table.setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Name", sig.signerName != null ? sig.signerName : "Unknown", boldFont, font);
        addTableRow(table, "Date", sig.signingDate != null ? sig.signingDate : "N/A", boldFont, font);
        addTableRow(table, "Reason", sig.reason != null ? sig.reason : "N/A", boldFont, font);
        addTableRow(table, "Location", sig.location != null ? sig.location : "N/A", boldFont, font);
        addTableRow(table, "CA", sig.certificateAuthority != null ? sig.certificateAuthority : "Unknown", boldFont, font);
        addTableRow(table, "Algorithm", sig.algorithm != null ? sig.algorithm : "N/A", boldFont, font);
        addTableRow(table, "Status", sig.message, boldFont, font);

        document.add(table);

        if (!sig.isValid) {
            document.add(new Paragraph("\nDocument signature could not be verified.")
                    .setFont(font)
                    .setFontSize(12)
                    .setFontColor(new DeviceRgb(220, 38, 38))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));
        }

        document.close();
        return baos.toByteArray();
    }

    private void addTableRow(Table table, String label, String value, PdfFont boldFont, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)).setPadding(5));
    }

    private void drawBigTick(PdfCanvas canvas, float x, float y, float s) {
        canvas.saveState();
        canvas.setFillColor(new DeviceRgb(0, 165, 80));
        canvas.moveTo(x - s * 0.45f, y - s * 0.05f);
        canvas.lineTo(x - s * 0.15f, y - s * 0.35f);
        canvas.lineTo(x + s * 0.55f, y + s * 0.35f);
        canvas.lineTo(x + s * 0.45f, y + s * 0.45f);
        canvas.lineTo(x - s * 0.15f, y - s * 0.15f);
        canvas.lineTo(x - s * 0.35f, y + s * 0.05f);
        canvas.closePathFillStroke();
        canvas.restoreState();
    }

    private void drawBigCross(PdfCanvas canvas, float x, float y, float s) {
        canvas.saveState();
        canvas.setFillColor(new DeviceRgb(220, 38, 38));
        canvas.setLineWidth(10);
        canvas.setStrokeColor(new DeviceRgb(220, 38, 38));
        canvas.moveTo(x - s * 0.35f, y - s * 0.35f);
        canvas.lineTo(x + s * 0.35f, y + s * 0.35f);
        canvas.stroke();
        canvas.moveTo(x + s * 0.35f, y - s * 0.35f);
        canvas.lineTo(x - s * 0.35f, y + s * 0.35f);
        canvas.stroke();
        canvas.restoreState();
    }
}
