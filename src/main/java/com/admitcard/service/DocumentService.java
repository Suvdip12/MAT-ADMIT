package com.admitcard.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class DocumentService {

    private final PdfSigningService pdfSigningService;

    public DocumentService(PdfSigningService pdfSigningService) {
        this.pdfSigningService = pdfSigningService;
    }

    public void generatePdf(String htmlContent, String outputPath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            ConverterProperties converterProperties = new ConverterProperties();
            HtmlConverter.convertToPdf(htmlContent, pdf, converterProperties);
        }

        try {
            pdfSigningService.signPdfFileInPlace(outputPath);
        } catch (Exception exception) {
            throw new IOException("PDF generated, but digital signing failed: " + exception.getMessage(), exception);
        }
    }

    public void generateWord(String htmlContent, String outputPath) throws IOException {
        // Simple HTML-to-Word conversion logic (plain text / basic formatting)
        // For a more robust solution, we'd use a specialized library, but for now, 
        // we'll extract text or use a simple POI approach.
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputPath)) {
            
            // This is a naive implementation. Real HTML-to-Word is complex.
            // We'll strip tags and add paragraphs for now, as a placeholder for better logic.
            String plainText = htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(plainText);
            
            document.write(out);
        }
    }

    public String processTemplate(String templateHtml, Map<String, String> placeholders) {
        String processed = templateHtml;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            processed = processed.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return processed;
    }
}
