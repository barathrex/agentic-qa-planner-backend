package com.qaassistant.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.qaassistant.dto.AcceptanceCriteriaDto;
import com.qaassistant.dto.QaPlanResponse;
import com.qaassistant.dto.TestCaseDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    public ByteArrayInputStream generatePdf(QaPlanResponse plan) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts & Colors
            Color primaryColor = new Color(37, 99, 235); // #2563EB
            Color darkTextColor = new Color(30, 41, 59); // #1E293B
            Color bgHeaderColor = new Color(241, 245, 249); // #F1F5F9

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, primaryColor);
            Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkTextColor);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkTextColor);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkTextColor);

            // Title Header
            Paragraph title = new Paragraph("Agentic QA Plan Document", titleFont);
            title.setSpacingAfter(5);
            document.add(title);

            Paragraph planTitle = new Paragraph(plan.getTitle() != null ? plan.getTitle() : "QA Test Plan", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, darkTextColor));
            planTitle.setSpacingAfter(15);
            document.add(planTitle);

            // Metadata Table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(20);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String createdDate = plan.getCreatedDate() != null ? plan.getCreatedDate().format(formatter) : "N/A";

            addMetaCell(metaTable, "Developer Name:", plan.getDeveloperName() != null ? plan.getDeveloperName() : "Barath", boldFont, bodyFont);
            addMetaCell(metaTable, "Created Date:", createdDate, boldFont, bodyFont);
            addMetaCell(metaTable, "Current Version:", "v" + (plan.getCurrentVersion() != null ? plan.getCurrentVersion() : 1), boldFont, bodyFont);
            addMetaCell(metaTable, "Coverage Score:", (plan.getCoveragePercentage() != null ? plan.getCoveragePercentage().intValue() : 100) + "%", boldFont, bodyFont);

            document.add(metaTable);

            // Description
            if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
                addSection(document, "Description", plan.getDescription(), sectionHeaderFont, bodyFont);
            }

            // Requirement
            addSection(document, "Requirement / User Story", plan.getRequirement(), sectionHeaderFont, bodyFont);

            // Implementation Summary
            addSection(document, "Implementation Summary", plan.getImplementationSummary(), sectionHeaderFont, bodyFont);

            // Acceptance Criteria & Coverage
            document.add(new Paragraph("Acceptance Criteria & Coverage Report", sectionHeaderFont));
            document.add(new Paragraph(" ", bodyFont));

            int totalAc = plan.getAcceptanceCriteria() != null ? plan.getAcceptanceCriteria().size() : 0;
            long coveredAc = plan.getAcceptanceCriteria() != null ? plan.getAcceptanceCriteria().stream().filter(AcceptanceCriteriaDto::isCovered).count() : 0;
            document.add(new Paragraph("Covered: " + coveredAc + " / " + totalAc + " | Coverage: " + (plan.getCoveragePercentage() != null ? plan.getCoveragePercentage().intValue() : 100) + "%", boldFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable acTable = new PdfPTable(3);
            acTable.setWidthPercentage(100);
            acTable.setWidths(new float[]{1, 6, 2});
            acTable.setSpacingAfter(20);

            addTableHeader(acTable, "Index", boldFont, bgHeaderColor);
            addTableHeader(acTable, "Acceptance Criterion", boldFont, bgHeaderColor);
            addTableHeader(acTable, "Status", boldFont, bgHeaderColor);

            if (plan.getAcceptanceCriteria() != null) {
                for (AcceptanceCriteriaDto ac : plan.getAcceptanceCriteria()) {
                    acTable.addCell(new PdfPCell(new Phrase("AC" + ac.getCriteriaIndex(), bodyFont)));
                    acTable.addCell(new PdfPCell(new Phrase(ac.getDescription(), bodyFont)));
                    String status = ac.isCovered() ? "✓ Covered" : "✗ Uncovered";
                    Font statusFont = ac.isCovered() ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(16, 185, 129)) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(239, 68, 68));
                    acTable.addCell(new PdfPCell(new Phrase(status, statusFont)));
                }
            }
            document.add(acTable);

            // Retrieved QA Guidance (RAG)
            if (plan.getRetrievedGuidance() != null && !plan.getRetrievedGuidance().isBlank()) {
                addSection(document, "Retrieved QA Guidance (RAG)", plan.getRetrievedGuidance(), sectionHeaderFont, bodyFont);
            }

            // User Flows
            if (plan.getUserFlows() != null && !plan.getUserFlows().isEmpty()) {
                document.add(new Paragraph("Main User Flows", sectionHeaderFont));
                com.lowagie.text.List flowList = new com.lowagie.text.List(com.lowagie.text.List.ORDERED);
                for (String flow : plan.getUserFlows()) {
                    flowList.add(new ListItem(flow, bodyFont));
                }
                document.add(flowList);
                document.add(new Paragraph(" ", bodyFont));
            }

            // Test Cases Table
            document.add(new Paragraph("Proposed Test Cases", sectionHeaderFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable tcTable = new PdfPTable(5);
            tcTable.setWidthPercentage(100);
            tcTable.setWidths(new float[]{2, 4, 3, 2, 2});
            tcTable.setSpacingAfter(20);

            addTableHeader(tcTable, "Test ID", boldFont, bgHeaderColor);
            addTableHeader(tcTable, "Title", boldFont, bgHeaderColor);
            addTableHeader(tcTable, "Category", boldFont, bgHeaderColor);
            addTableHeader(tcTable, "Priority", boldFont, bgHeaderColor);
            addTableHeader(tcTable, "Mapped AC", boldFont, bgHeaderColor);

            if (plan.getTestCases() != null) {
                for (TestCaseDto tc : plan.getTestCases()) {
                    tcTable.addCell(new PdfPCell(new Phrase(tc.getTestId(), boldFont)));
                    tcTable.addCell(new PdfPCell(new Phrase(tc.getTitle(), bodyFont)));
                    tcTable.addCell(new PdfPCell(new Phrase(tc.getCategory() != null ? tc.getCategory().name() : "MANUAL", bodyFont)));
                    tcTable.addCell(new PdfPCell(new Phrase(tc.getPriority() != null ? tc.getPriority().name() : "MEDIUM", bodyFont)));

                    String mapped = tc.getMappedCriteriaIndices() != null ? tc.getMappedCriteriaIndices().stream().map(i -> "AC" + i).reduce((a, b) -> a + ", " + b).orElse("None") : "None";
                    tcTable.addCell(new PdfPCell(new Phrase(mapped, bodyFont)));
                }
            }
            document.add(tcTable);

            // Assumptions
            if (plan.getAssumptions() != null && !plan.getAssumptions().isEmpty()) {
                document.add(new Paragraph("Assumptions", sectionHeaderFont));
                com.lowagie.text.List asmList = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
                for (String asm : plan.getAssumptions()) {
                    asmList.add(new ListItem(asm, bodyFont));
                }
                document.add(asmList);
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addSection(Document document, String title, String content, Font headerFont, Font bodyFont) throws DocumentException {
        document.add(new Paragraph(title, headerFont));
        Paragraph p = new Paragraph(content, bodyFont);
        p.setSpacingAfter(15);
        document.add(p);
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value, valueFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(bgColor);
        header.setPadding(6);
        header.setPhrase(new Phrase(text, font));
        table.addCell(header);
    }
}
