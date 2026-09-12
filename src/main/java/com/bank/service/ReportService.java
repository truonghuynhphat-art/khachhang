package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
// import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.Chunk;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ChartService chartService;

    public ReportService(TransactionRepository transactionRepository,
                          AccountRepository accountRepository,
                          ChartService chartService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.chartService = chartService;
    }

    /** Dữ liệu tổng hợp dùng chung cho cả Excel và PDF. */
    private static class ReportData {
        Account account; // null nếu báo cáo toàn hệ thống
        List<Transaction> transactions;
        TreeMap<LocalDate, Double> amountByDay = new TreeMap<>();
        TreeMap<LocalDate, Double> feeByDay = new TreeMap<>();
        TreeMap<LocalDate, Double> balanceByDay = new TreeMap<>(); // chỉ có khi account != null
    }

    private ReportData buildData(Long accountId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Invalid date range", HttpStatus.BAD_REQUEST);
        }

        ReportData data = new ReportData();

        if (accountId != null) {
            data.account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));
            data.transactions = transactionRepository
                    .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(accountId, from, to);
        } else {
            data.transactions = transactionRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to);
        }

        for (Transaction tx : data.transactions) {
            LocalDate day = tx.getCreatedAt().toLocalDate();
            data.amountByDay.merge(day, tx.getAmount().doubleValue(), Double::sum);
            data.feeByDay.merge(day, tx.getFee().doubleValue(), Double::sum);
        }

        // Dựng lại số dư theo ngày cho trường hợp 1 account cụ thể - CHỈ đúng nếu "to" là hiện tại,
        // vì tính bằng cách lùi ngược từ balance hiện tại trừ/cộng các giao dịch trong khoảng.
        if (data.account != null) {
            BigDecimal runningBalance = data.account.getBalance();
            List<Transaction> reversed = new ArrayList<>(data.transactions);
            Collections.reverse(reversed);
            for (Transaction tx : reversed) {
                LocalDate day = tx.getCreatedAt().toLocalDate();
                data.balanceByDay.putIfAbsent(day, runningBalance.doubleValue());
                runningBalance = isCredit(tx.getType())
                        ? runningBalance.subtract(tx.getAmount())
                        : runningBalance.add(tx.getAmount());
            }
        }

        return data;
    }

    private boolean isCredit(String type) {
        return "DEPOSIT".equals(type) || "TRANSFER_IN".equals(type);
    }

    public byte[] generateExcel(Long accountId, LocalDateTime from, LocalDateTime to) throws IOException {
        ReportData data = buildData(accountId, from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Account", "Type", "Amount", "Fee", "Location", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Transaction tx : data.transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId());
                row.createCell(1).setCellValue(tx.getAccount().getAccountNumber());
                row.createCell(2).setCellValue(tx.getType());
                row.createCell(3).setCellValue(tx.getAmount().doubleValue());
                row.createCell(4).setCellValue(tx.getFee().doubleValue());
                row.createCell(5).setCellValue(tx.getLocation());
                row.createCell(6).setCellValue(tx.getCreatedAt().toString());
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Sheet biểu đồ - chèn ảnh PNG do JFreeChart render
            Sheet chartSheet = workbook.createSheet("Chart");
            Map<String, TreeMap<LocalDate, Double>> series = new LinkedHashMap<>();
            series.put("Tong giao dich", data.amountByDay);
            series.put("Phi giao dich", data.feeByDay);
            if (data.account != null) {
                series.put("So du", data.balanceByDay);
            }
            byte[] chartImage = chartService.renderLineChart("Xu huong giao dich", series);

            int pictureIdx = workbook.addPicture(chartImage, Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = chartSheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(12);
            anchor.setRow2(25);
            drawing.createPicture(anchor, pictureIdx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generatePdf(Long accountId, LocalDateTime from, LocalDateTime to) throws Exception {
        ReportData data = buildData(accountId, from, to);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont =
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font normalFont =
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10);

        document.add(new Paragraph("Bao cao tai chinh", titleFont));
        document.add(new Paragraph(
                "Tu ngay: " + from + "   Den ngay: " + to
                        + (data.account != null ? "   Account: " + data.account.getAccountNumber() : "   (Toan he thong)"),
                normalFont));
        document.add(Chunk.NEWLINE);

        BigDecimal totalAmount = data.transactions.stream()
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFee = data.transactions.stream()
                .map(Transaction::getFee).reduce(BigDecimal.ZERO, BigDecimal::add);

        document.add(new Paragraph("Tong so giao dich: " + data.transactions.size(), normalFont));
        document.add(new Paragraph("Tong gia tri giao dich: " + totalAmount, normalFont));
        document.add(new Paragraph("Tong phi: " + totalFee, normalFont));
        document.add(Chunk.NEWLINE);

        // Biểu đồ
        Map<String, TreeMap<LocalDate, Double>> series = new LinkedHashMap<>();
        series.put("Tong giao dich", data.amountByDay);
        series.put("Phi giao dich", data.feeByDay);
        if (data.account != null) {
            series.put("So du", data.balanceByDay);
        }
        byte[] chartImage = chartService.renderLineChart("Xu huong giao dich", series);
        Image chart = Image.getInstance(chartImage);
        chart.scaleToFit(500, 260);
        document.add(chart);
        document.add(Chunk.NEWLINE);

        // Bảng chi tiết giao dịch
        Table table = new Table(6);
        table.setWidth(100);
        String[] headers = {"ID", "Account", "Type", "Amount", "Fee", "Created At"};
        for (String h : headers) {
            // table.addCell(new Cell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD))));
            table.addCell(new com.lowagie.text.Cell(new Phrase(h,
                    new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD))));
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Transaction tx : data.transactions) {
            table.addCell(new Phrase(String.valueOf(tx.getId()), normalFont));
            table.addCell(new Phrase(tx.getAccount().getAccountNumber(), normalFont));
            table.addCell(new Phrase(tx.getType(), normalFont));
            table.addCell(new Phrase(tx.getAmount().toString(), normalFont));
            table.addCell(new Phrase(tx.getFee().toString(), normalFont));
            table.addCell(new Phrase(tx.getCreatedAt().format(fmt), normalFont));
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }

    @org.springframework.scheduling.annotation.Async
    public java.util.concurrent.CompletableFuture<byte[]> generatePdfAsync(
            Long accountId, LocalDateTime from, LocalDateTime to) throws Exception {
        byte[] result = generatePdf(accountId, from, to);
        return java.util.concurrent.CompletableFuture.completedFuture(result);
    }
}

