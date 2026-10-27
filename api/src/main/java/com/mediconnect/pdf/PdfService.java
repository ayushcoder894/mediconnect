package com.mediconnect.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mediconnect.appointment.Appointment;
import com.mediconnect.lab.LabBooking;
import com.mediconnect.lab.LabResult;
import com.mediconnect.prescription.Prescription;
import com.mediconnect.prescription.PrescriptionItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Generates the appointment slip (with check-in QR), lab report and prescription PDFs. */
@Service
public class PdfService {

    private static final Color BRAND = new Color(13, 148, 136);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LIGHT = new Color(240, 253, 250);
    private static final Color HIGH = new Color(220, 38, 38);
    private static final Color LOW = new Color(217, 119, 6);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE, d MMM yyyy, h:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
    private static final Font VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    public byte[] appointmentSlip(Appointment a) {
        return render(doc -> {
            header(doc, "Appointment Slip", "Ref #MC-" + a.getId());
            var d = a.getDoctor();
            PdfPTable info = twoColumns();
            field(info, "Patient", a.getPatient().getUser().getFullName());
            field(info, "Doctor", d.getUser().getFullName() + " (" + d.getSpecialization() + ")");
            field(info, "Date & time", a.getSlotStart().format(DATE_TIME));
            field(info, "Hospital", d.getHospital() == null ? "-" : d.getHospital().getName() + ", " + d.getHospital().getCity());
            field(info, "Consultation fee", "Rs. " + a.getFee());
            field(info, "Status", a.getStatus().name().replace('_', ' '));
            doc.add(info);

            Paragraph scan = new Paragraph("Show this QR code at the front desk to check in", BODY);
            scan.setAlignment(Element.ALIGN_CENTER);
            scan.setSpacingBefore(18);
            doc.add(scan);
            Image qr = Image.getInstance(qrPng(a.getCheckInCode()));
            qr.scaleAbsolute(170, 170);
            qr.setAlignment(Image.ALIGN_CENTER);
            doc.add(qr);
            Paragraph code = new Paragraph(a.getCheckInCode(), SMALL);
            code.setAlignment(Element.ALIGN_CENTER);
            doc.add(code);
            footer(doc, "Please arrive 10 minutes early. To cancel or reschedule, use the MediConnect app.");
        });
    }

    public byte[] labReport(LabBooking b) {
        return render(doc -> {
            header(doc, "Laboratory Report", "Report #LAB-" + b.getId());
            PdfPTable info = twoColumns();
            var p = b.getPatient();
            field(info, "Patient", p.getUser().getFullName());
            field(info, "Age / Gender", (p.age() == null ? "-" : p.age() + " yrs") + " / " + (p.getGender() == null ? "-" : p.getGender()));
            field(info, "Test", b.getLabTest().getName());
            field(info, "Sample", b.getLabTest().getSampleType() == null ? "-" : b.getLabTest().getSampleType());
            field(info, "Collected", b.getScheduledAt().format(DATE_TIME));
            field(info, "Referred by", b.getOrderedBy() == null ? "Self" : b.getOrderedBy().getUser().getFullName());
            doc.add(info);

            PdfPTable table = new PdfPTable(new float[]{3.2f, 1.6f, 1.3f, 1.9f, 1.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(18);
            for (String h : new String[]{"Parameter", "Result", "Unit", "Reference range", "Flag"}) {
                table.addCell(headCell(h));
            }
            for (LabResult r : b.getResults()) {
                Font valueFont = r.getFlag() == null || "NORMAL".equals(r.getFlag()) ? BODY
                        : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, "HIGH".equals(r.getFlag()) ? HIGH : LOW);
                table.addCell(cell(r.getParameter(), BODY));
                table.addCell(cell(r.getValue(), valueFont));
                table.addCell(cell(nullToDash(r.getUnit()), BODY));
                table.addCell(cell(nullToDash(r.getReferenceRange()), BODY));
                table.addCell(cell(r.getFlag() == null ? "-" : r.getFlag(), valueFont));
            }
            doc.add(table);
            footer(doc, "Values outside the reference range are flagged. Please discuss your results with your doctor.");
        });
    }

    public byte[] prescription(Prescription rx) {
        return render(doc -> {
            var d = rx.getDoctor();
            header(doc, "Prescription", "Rx #" + rx.getId());
            PdfPTable info = twoColumns();
            field(info, "Doctor", d.getUser().getFullName() + ", " + nullToDash(d.getQualification()));
            field(info, "Specialization", d.getSpecialization());
            field(info, "Patient", rx.getPatient().getUser().getFullName());
            field(info, "Visit date", rx.getAppointment().getSlotStart().format(DATE_TIME));
            field(info, "Diagnosis", rx.getDiagnosis());
            field(info, "Follow-up", rx.getFollowUpDate() == null ? "As needed" : rx.getFollowUpDate().format(DATE));
            doc.add(info);

            PdfPTable table = new PdfPTable(new float[]{3f, 1.6f, 2f, 1.2f, 3f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(18);
            for (String h : new String[]{"Medicine", "Dosage", "Frequency", "Days", "Instructions"}) {
                table.addCell(headCell(h));
            }
            for (PrescriptionItem i : rx.getItems()) {
                table.addCell(cell(i.getMedicine(), VALUE));
                table.addCell(cell(i.getDosage(), BODY));
                table.addCell(cell(i.getFrequency(), BODY));
                table.addCell(cell(String.valueOf(i.getDurationDays()), BODY));
                table.addCell(cell(nullToDash(i.getInstructions()), BODY));
            }
            doc.add(table);
            if (rx.getNotes() != null && !rx.getNotes().isBlank()) {
                Paragraph notes = new Paragraph("Doctor's notes: " + rx.getNotes(), BODY);
                notes.setSpacingBefore(14);
                doc.add(notes);
            }
            Paragraph sign = new Paragraph(d.getUser().getFullName(), VALUE);
            sign.setAlignment(Element.ALIGN_RIGHT);
            sign.setSpacingBefore(40);
            doc.add(sign);
            Paragraph signLabel = new Paragraph("Digitally issued via MediConnect", SMALL);
            signLabel.setAlignment(Element.ALIGN_RIGHT);
            doc.add(signLabel);
            footer(doc, "Take medicines exactly as prescribed. Do not share your medicines with others.");
        });
    }

    // ---- building blocks ----

    @FunctionalInterface
    private interface Body {
        void write(Document doc) throws DocumentException, IOException;
    }

    private byte[] render(Body body) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            body.write(doc);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Could not generate PDF", e);
        }
    }

    private void header(Document doc, String title, String reference) throws DocumentException {
        PdfPTable bar = new PdfPTable(new float[]{3, 2});
        bar.setWidthPercentage(100);
        PdfPCell brand = new PdfPCell(new Phrase("MediConnect", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE)));
        brand.setBackgroundColor(BRAND);
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setPadding(12);
        PdfPCell tag = new PdfPCell(new Phrase("Your health, connected", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.WHITE)));
        tag.setBackgroundColor(BRAND);
        tag.setBorder(Rectangle.NO_BORDER);
        tag.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tag.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tag.setPadding(12);
        bar.addCell(brand);
        bar.addCell(tag);
        doc.add(bar);

        Paragraph t = new Paragraph(title, TITLE);
        t.setSpacingBefore(16);
        doc.add(t);
        Paragraph ref = new Paragraph(reference + "   |   Generated " + LocalDateTime.now().format(DATE_TIME), SMALL);
        ref.setSpacingAfter(12);
        doc.add(ref);
    }

    private PdfPTable twoColumns() {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        return t;
    }

    private void field(PdfPTable table, String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(LIGHT);
        c.setPadding(8);
        c.addElement(new Paragraph(label.toUpperCase(), LABEL));
        c.addElement(new Paragraph(value, VALUE));
        table.addCell(c);
    }

    private PdfPCell headCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, HEAD));
        c.setBackgroundColor(BRAND);
        c.setBorderColor(BRAND);
        c.setPadding(7);
        return c;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorderColor(new Color(226, 232, 240));
        c.setPadding(7);
        return c;
    }

    private void footer(Document doc, String note) throws DocumentException {
        Paragraph p = new Paragraph(note, SMALL);
        p.setSpacingBefore(28);
        doc.add(p);
        Paragraph disclaimer = new Paragraph("MediConnect demo platform. Not for clinical use.", SMALL);
        doc.add(disclaimer);
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static byte[] qrPng(String text) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 400, 400);
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Could not create QR code", e);
        }
    }
}
