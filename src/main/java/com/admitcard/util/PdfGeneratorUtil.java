package com.admitcard.util;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import java.io.FileOutputStream;
import java.io.IOException;

public class PdfGeneratorUtil {

    public static void generatePdfFromHtml(String htmlContent, String outputPath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            
            ConverterProperties converterProperties = new ConverterProperties();
            
            HtmlConverter.convertToPdf(htmlContent, pdf, converterProperties);
        }
    }
}
