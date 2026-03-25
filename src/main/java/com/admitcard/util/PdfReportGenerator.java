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

        // ── Header ──────────────────────────────────────────────────────────
        String title = sig.isValid ? "Signature Valid" : "Signature Not Verified";
        DeviceRgb headerColor = sig.isValid
                ? new DeviceRgb(30, 190, 70)
                : new DeviceRgb(220, 38, 38);

        document.add(new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // ── Big Icon (drawn directly on canvas) ─────────────────────────────
        float centerX = PageSize.A4.getWidth() / 2;
        float centerY = PageSize.A4.getHeight() - 150;
        PdfCanvas canvas = new PdfCanvas(pdf.getPage(1));

        if (sig.isValid) {
            drawBigTick(canvas, centerX, centerY, 60);
        } else {
            drawBigQuestionMark(canvas, centerX, centerY, 60);
        }


        // Spacer so layout text flows below the icon area
        document.add(new Paragraph("\n\n\n\n\n\n"));

        // ── Sub-title ────────────────────────────────────────────────────────
        document.add(new Paragraph("Digitally Signed")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // ── Details Table ────────────────────────────────────────────────────
        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }));
        table.setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Name", sig.signerName != null ? sig.signerName : "Unknown", boldFont, font);
        addTableRow(table, "Date", sig.signingDate != null ? sig.signingDate : "N/A", boldFont, font);
        addTableRow(table, "Reason", sig.reason != null ? sig.reason : "N/A", boldFont, font);
        addTableRow(table, "Location", sig.location != null ? sig.location : "N/A", boldFont, font);
        addTableRow(table, "CA", sig.certificateAuthority != null ? sig.certificateAuthority : "Unknown", boldFont,
                font);
        addTableRow(table, "Algorithm", sig.algorithm != null ? sig.algorithm : "N/A", boldFont, font);
        addTableRow(table, "Status", sig.message, boldFont, font);

        document.add(table);

        // ── Invalid footer note ──────────────────────────────────────────────
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void addTableRow(Table table, String label, String value,
            PdfFont boldFont, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)).setPadding(5));
    }

    /**
     * Draws a chunky green checkmark with a solid black 3-D extrusion shadow
     * offset toward the bottom-right — matching the reference image exactly.
     *
     * PDF coordinate system: y increases upward, so "down" = negative y.
     *
     * @param canvas target canvas
     * @param x      horizontal centre of the icon
     * @param y      vertical centre of the icon
     * @param s      scale factor (half-size in points)
     */
    private void drawBigTick(PdfCanvas canvas, float x, float y, float s) {
        canvas.saveState();

        DeviceRgb green = new DeviceRgb(30, 190, 70);
        DeviceRgb black = new DeviceRgb(0, 0, 0);

        /*
         * Tick polygon — 6 vertices that form a chunky checkmark:
         *
         * 5 ──────────────────── 3
         * \ / \
         * \ / 2
         * 0 /
         * \ 4
         * 1
         *
         * 0 = left outer top
         * 1 = bottom outer tip (the deepest point of the "V")
         * 2 = right outer top
         * 3 = right inner top
         * 4 = bottom inner tip
         * 5 = left inner top
         */
        float[][] p = {
                { x - s * 0.65f, y + s * 0.04f }, // 0 left outer top
                { x - s * 0.30f, y - s * 0.56f }, // 1 bottom outer tip
                { x + s * 0.57f, y + s * 0.39f }, // 2 right outer top
                { x + s * 0.37f, y + s * 0.56f }, // 3 right inner top
                { x - s * 0.27f, y - s * 0.24f }, // 4 bottom inner tip
                { x - s * 0.47f, y + s * 0.22f }  // 5 left inner top
        };


        // ── 1. Solid black 3-D extrusion shadow (bottom-right) ──────────────
        // Each iteration paints a filled + stroked copy of the tick shifted
        // (+d, -d) — right and down in PDF space. Stacking them fills the
        // gap between the green shape and its shadow edge completely.
        float shadowDepth = s * 0.10f;

        canvas.setFillColor(black);
        canvas.setStrokeColor(black);
        canvas.setLineWidth(1.0f);

        for (float d = 1f; d <= shadowDepth; d += 1.0f) {
            canvas.moveTo(p[0][0] + d, p[0][1] - d);
            for (int i = 1; i < p.length; i++) {
                canvas.lineTo(p[i][0] + d, p[i][1] - d);
            }
            canvas.closePathFillStroke();
        }

        // ── 2. Green tick with thick black outline on top ───────────────────
        canvas.setFillColor(green);
        canvas.setStrokeColor(black);
        canvas.setLineWidth(3.0f);
        canvas.setLineJoinStyle(0); // 0 = miter — sharp corners

        canvas.moveTo(p[0][0], p[0][1]);
        for (int i = 1; i < p.length; i++) {
            canvas.lineTo(p[i][0], p[i][1]);
        }
        canvas.closePathFillStroke();

        canvas.restoreState();
    }

    /**
     * Draws a chunky yellow question mark with a solid black 3-D extrusion shadow.
     */
    private void drawBigQuestionMark(PdfCanvas canvas, float x, float y, float s) {
        canvas.saveState();

        DeviceRgb yellow = new DeviceRgb(245, 158, 11);
        DeviceRgb black = new DeviceRgb(0, 0, 0);

        // ── 1. Question Mark Shape (approximate with points for extrusion) ──
        // (Curve and Dot)
        float dotX = x;
        float dotY = y - s * 0.45f;
        float dotR = s * 0.15f;

        // Curve points
        float[][] q = {
            { x - s * 0.4f, y + s * 0.2f },
            { x - s * 0.4f, y + s * 0.6f },
            { x + s * 0.4f, y + s * 0.6f },
            { x + s * 0.4f, y + s * 0.2f },
            { x + s * 0.1f, y - s * 0.1f }
        };

        float shadowDepth = s * 0.10f;

        // ── 2. Shadow (Extrusion) ───────────────────────────────────────────
        canvas.setFillColor(black);
        canvas.setStrokeColor(black);
        
        for (float d = 1f; d <= shadowDepth; d += 1.0f) {
            // Shadow curve
            canvas.setLineWidth(s * 0.25f);
            canvas.moveTo(q[0][0] + d, q[0][1] - d);
            canvas.curveTo(q[1][0] + d, q[1][1] - d, q[2][0] + d, q[2][1] - d, q[3][0] + d, q[3][1] - d);
            canvas.lineTo(q[4][0] + d, q[4][1] - d);
            canvas.stroke();
            
            // Shadow dot
            canvas.circle(dotX + d, dotY - d, dotR);
            canvas.fill();
        }

        // ── 3. Main Yellow Shape ────────────────────────────────────────────
        // Yellow curve
        canvas.setStrokeColor(yellow);
        canvas.setLineWidth(s * 0.22f);
        canvas.setLineCapStyle(1); // round
        canvas.moveTo(q[0][0], q[0][1]);
        canvas.curveTo(q[1][0], q[1][1], q[2][0], q[2][1], q[3][0], q[3][1]);
        canvas.lineTo(q[4][0], q[4][1]);
        canvas.stroke();

        // Yellow dot
        canvas.setFillColor(yellow);
        canvas.circle(dotX, dotY, dotR);
        canvas.fill();

        // ── 4. Black Outlines ───────────────────────────────────────────────
        canvas.setStrokeColor(black);
        canvas.setLineWidth(2.0f);
        
        // Outline for dot
        canvas.circle(dotX, dotY, dotR);
        canvas.stroke();
        
        // (Curve outline is harder with simple stroke, but let's add a thin one)
        canvas.setLineWidth(1.0f);
        canvas.moveTo(q[0][0], q[0][1]);
        canvas.curveTo(q[1][0], q[1][1], q[2][0], q[2][1], q[3][0], q[3][1]);
        canvas.lineTo(q[4][0], q[4][1]);
        canvas.stroke();

        canvas.restoreState();
    }
}