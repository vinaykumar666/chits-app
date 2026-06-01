package com.ygc.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.ygc.model.ChitMembership;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

@Service
public class PdfCertificateService {

    public String generateMemberCertificate(ChitMembership membership) throws Exception {
        String dir = "uploads/certificates/";
        new File(dir).mkdirs();
        String filename = dir + "certificate_" + membership.getId() + "_" +
                membership.getUser().getId() + ".pdf";

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.DARK_GRAY);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph title = new Paragraph("YGC Internal - Chit Membership Certificate", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph tagline = new Paragraph("Save Rupee, Rupee Will Save You In Future", normalFont);
        tagline.setAlignment(Element.ALIGN_CENTER);
        document.add(tagline);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("─".repeat(80), normalFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Member Details", headerFont));
        document.add(new Paragraph("Name: " + membership.getUser().getFullName(), normalFont));
        document.add(new Paragraph("Email: " + membership.getUser().getEmail(), normalFont));
        document.add(new Paragraph("Phone: " + membership.getUser().getPhone(), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Chit Details", headerFont));
        document.add(new Paragraph("Chit Name: " + membership.getChit().getName(), normalFont));
        document.add(new Paragraph("Monthly Amount: ₹" + membership.getChit().getMonthlyAmount(), normalFont));
        document.add(new Paragraph("Total Value: ₹" + membership.getChit().getTotalChitValue(), normalFont));
        document.add(new Paragraph("Start Date: " + membership.getChit().getStartDate(), normalFont));
        document.add(new Paragraph("Duration: " + membership.getChit().getDurationMonths() + " months", normalFont));
        document.add(new Paragraph("Joined Date: " + membership.getJoinedAt().toLocalDate(), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("─".repeat(80), normalFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Terms & Conditions Accepted: Yes", normalFont));
        document.add(new Paragraph("Digital Signature: Accepted on " + LocalDate.now(), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("This is a digitally generated certificate.", normalFont));
        document.add(new Paragraph("Generated on: " + LocalDate.now(), normalFont));

        document.close();
        return filename;
    }
}