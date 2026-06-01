package com.ygc.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.ygc.model.Auction;
import com.ygc.model.ChitMembership;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfCertificateService {
    private final LoggingUtil loggingUtil;

    public String generateCertificate(Auction auction, ChitMembership membership) {
        loggingUtil.transactionStart("generateCertificate", "PdfCertificateService");
        try {
            String filename = "certificate_" + auction.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String filepath = "uploads/certificates/" + filename;
            new File("uploads/certificates").mkdirs();

            PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setMargins(40, 40, 40, 40);

            // Header
            Paragraph header = new Paragraph("YGC INTERNAL")
                    .setFontSize(28)
                    .setBold()
                    .setFontColor(new DeviceRgb(51, 102, 204))
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(header);

            Paragraph subtitle = new Paragraph("Chit Fund Certificate of Achievement")
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            doc.add(subtitle);

            // Border decoration
            Table borderTable = new Table(1);
            borderTable.setWidth(500);
            borderTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            Cell borderCell = new Cell();
            borderCell.setBorder(new SolidBorder(new DeviceRgb(51, 102, 204), 3));
            borderCell.setPadding(30);

            // Certificate content
            Paragraph content = new Paragraph("This is to certify that")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            borderCell.add(content);

            Paragraph name = new Paragraph(membership.getUser().getFullName().toUpperCase())
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15);
            borderCell.add(name);

            Paragraph achievement = new Paragraph("\nhas successfully participated and won the auction for\n")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            borderCell.add(achievement);

            Paragraph chitName = new Paragraph(membership.getChit().getName())
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(51, 102, 204));
            borderCell.add(chitName);

            Paragraph details = new Paragraph("\nWinning Bid Amount: ₹" + auction.getWinningBidAmount() +
                    "\nMonth: " + auction.getMonthNumber() +
                    "\nDate: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            borderCell.add(details);

            borderTable.addCell(borderCell);
            doc.add(borderTable);

            // Footer
            Paragraph footer = new Paragraph("\n\n✓ Official Certificate\nSave Rupee, Rupee Will Save You")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY);
            doc.add(footer);

            doc.close();
            loggingUtil.info("Certificate generated: " + filepath, "PdfCertificateService");
            return filepath;
        } catch (Exception e) {
            loggingUtil.error("Error generating certificate", "PdfCertificateService", e);
            throw new RuntimeException(e);
        }
    }
}

