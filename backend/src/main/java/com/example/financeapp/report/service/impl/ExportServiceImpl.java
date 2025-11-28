package com.example.financeapp.report.service.impl;

import com.example.financeapp.budget.dto.BudgetResponse;
import com.example.financeapp.budget.service.BudgetService;
import com.example.financeapp.report.dto.ExportRequest;
import com.example.financeapp.report.service.ExportService;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.repository.WalletRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public Resource exportReport(Long userId, ExportRequest request) {
        switch (request.getReportType()) {
            case TRANSACTIONS:
                return exportTransactions(userId, request);
            case BUDGETS:
                return exportBudgets(userId, request);
            case SUMMARY:
                return exportSummary(userId, request);
            default:
                return exportTransactions(userId, request);
        }
    }

    @Override
    public Resource exportTransactions(Long userId, ExportRequest request) {
        // Lấy danh sách giao dịch
        List<Transaction> transactions = transactionRepository.findByUser_UserIdOrderByTransactionDateDesc(userId);
        
        // Lọc theo điều kiện
        if (request.getStartDate() != null || request.getEndDate() != null || request.getWalletId() != null) {
            transactions = transactions.stream()
                    .filter(t -> {
                        if (request.getStartDate() != null && 
                            t.getTransactionDate().toLocalDate().isBefore(request.getStartDate())) {
                            return false;
                        }
                        if (request.getEndDate() != null && 
                            t.getTransactionDate().toLocalDate().isAfter(request.getEndDate())) {
                            return false;
                        }
                        if (request.getWalletId() != null && 
                            !t.getWallet().getWalletId().equals(request.getWalletId())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        if (request.getFormat() == ExportRequest.ExportFormat.EXCEL) {
            return generateTransactionsExcel(transactions, userId);
        } else {
            return generateTransactionsPDF(transactions, userId);
        }
    }

    @Override
    public Resource exportBudgets(Long userId, ExportRequest request) {
        List<BudgetResponse> budgets = budgetService.getAllBudgets(userId);
        
        if (request.getFormat() == ExportRequest.ExportFormat.EXCEL) {
            return generateBudgetsExcel(budgets, userId);
        } else {
            return generateBudgetsPDF(budgets, userId);
        }
    }

    @Override
    public Resource exportSummary(Long userId, ExportRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        List<Transaction> transactions = transactionRepository.findByUser_UserIdOrderByTransactionDateDesc(userId);
        List<BudgetResponse> budgets = budgetService.getAllBudgets(userId);
        List<Wallet> wallets = walletRepository.findByUser_UserId(userId);
        
        // Lọc transactions theo điều kiện
        if (request.getStartDate() != null || request.getEndDate() != null || request.getWalletId() != null) {
            transactions = transactions.stream()
                    .filter(t -> {
                        if (request.getStartDate() != null && 
                            t.getTransactionDate().toLocalDate().isBefore(request.getStartDate())) {
                            return false;
                        }
                        if (request.getEndDate() != null && 
                            t.getTransactionDate().toLocalDate().isAfter(request.getEndDate())) {
                            return false;
                        }
                        if (request.getWalletId() != null && 
                            !t.getWallet().getWalletId().equals(request.getWalletId())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        if (request.getFormat() == ExportRequest.ExportFormat.EXCEL) {
            return generateSummaryExcel(user, transactions, budgets, wallets, request);
        } else {
            return generateSummaryPDF(user, transactions, budgets, wallets, request);
        }
    }

    // ============ EXCEL GENERATION ============

    private Resource generateTransactionsExcel(List<Transaction> transactions, Long userId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Báo cáo giao dịch");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Tạo style cho data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // Tạo style cho số tiền
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0"));
            
            int rowNum = 0;
            
            // Header
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"STT", "Ngày giờ", "Loại", "Danh mục", "Ví", "Số tiền", "Ghi chú"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;
            int stt = 1;
            
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(t.getTransactionDate().format(DATETIME_FORMATTER));
                row.createCell(2).setCellValue(t.getTransactionType().getTypeName());
                row.createCell(3).setCellValue(t.getCategory().getCategoryName());
                row.createCell(4).setCellValue(t.getWallet().getWalletName());
                
                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(t.getAmount().doubleValue());
                amountCell.setCellStyle(currencyStyle);
                
                row.createCell(6).setCellValue(t.getNote() != null ? t.getNote() : "");
                
                // Tính tổng
                if ("Thu nhập".equals(t.getTransactionType().getTypeName())) {
                    totalIncome = totalIncome.add(t.getAmount());
                } else {
                    totalExpense = totalExpense.add(t.getAmount());
                }
                
                // Apply data style
                for (int i = 0; i < 7; i++) {
                    if (i != 5) { // Skip amount cell (already styled)
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }
            
            // Tổng kết
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("TỔNG KẾT");
            summaryRow.getCell(0).setCellStyle(headerStyle);
            
            Row incomeRow = sheet.createRow(rowNum++);
            incomeRow.createCell(0).setCellValue("Tổng thu nhập:");
            incomeRow.createCell(5).setCellValue(totalIncome.doubleValue());
            incomeRow.getCell(5).setCellStyle(currencyStyle);
            
            Row expenseRow = sheet.createRow(rowNum++);
            expenseRow.createCell(0).setCellValue("Tổng chi tiêu:");
            expenseRow.createCell(5).setCellValue(totalExpense.doubleValue());
            expenseRow.getCell(5).setCellStyle(currencyStyle);
            
            Row balanceRow = sheet.createRow(rowNum++);
            balanceRow.createCell(0).setCellValue("Số dư:");
            BigDecimal balance = totalIncome.subtract(totalExpense);
            balanceRow.createCell(5).setCellValue(balance.doubleValue());
            balanceRow.getCell(5).setCellStyle(currencyStyle);
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    private Resource generateBudgetsExcel(List<BudgetResponse> budgets, Long userId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Báo cáo ngân sách");
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0"));
            
            int rowNum = 0;
            
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"STT", "Tên ngân sách", "Ví", "Hạn mức", "Đã chi", "Còn lại", "Tỷ lệ %", "Trạng thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int stt = 1;
            for (BudgetResponse budget : budgets) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(budget.getCategoryName());
                row.createCell(2).setCellValue(budget.getWalletName() != null ? budget.getWalletName() : "Tất cả ví");
                
                Cell limitCell = row.createCell(3);
                limitCell.setCellValue(budget.getAmountLimit().doubleValue());
                limitCell.setCellStyle(currencyStyle);
                
                Cell spentCell = row.createCell(4);
                spentCell.setCellValue(budget.getSpentAmount().doubleValue());
                spentCell.setCellStyle(currencyStyle);
                
                Cell remainingCell = row.createCell(5);
                remainingCell.setCellValue(budget.getRemainingAmount().doubleValue());
                remainingCell.setCellStyle(currencyStyle);
                
                row.createCell(6).setCellValue(budget.getUsagePercentage());
                row.createCell(7).setCellValue(budget.getBudgetStatus());
                
                for (int i = 0; i < 8; i++) {
                    if (i != 3 && i != 4 && i != 5) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    private Resource generateSummaryExcel(User user, List<Transaction> transactions, 
                                         List<BudgetResponse> budgets, List<Wallet> wallets,
                                         ExportRequest request) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Sheet 1: Tổng quan
            Sheet summarySheet = workbook.createSheet("Tổng quan");
            createSummarySheet(summarySheet, workbook, user, transactions, budgets, wallets, request);
            
            // Sheet 2: Giao dịch
            Sheet transactionSheet = workbook.createSheet("Giao dịch");
            createTransactionSheet(transactionSheet, workbook, transactions);
            
            // Sheet 3: Ngân sách
            Sheet budgetSheet = workbook.createSheet("Ngân sách");
            createBudgetSheet(budgetSheet, workbook, budgets);
            
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    private void createSummarySheet(Sheet sheet, Workbook workbook, User user, 
                                   List<Transaction> transactions, List<BudgetResponse> budgets,
                                   List<Wallet> wallets, ExportRequest request) {
        CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.LIGHT_BLUE);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 0;
        
        // Tiêu đề
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO TỔNG HỢP TÀI CHÍNH");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        rowNum++;
        
        // Thông tin user
        Row userRow = sheet.createRow(rowNum++);
        userRow.createCell(0).setCellValue("Người dùng: " + user.getFullName());
        userRow.createCell(2).setCellValue("Email: " + user.getEmail());
        
        // Khoảng thời gian
        if (request.getStartDate() != null || request.getEndDate() != null) {
            Row dateRow = sheet.createRow(rowNum++);
            String dateRange = "Khoảng thời gian: ";
            if (request.getStartDate() != null) {
                dateRange += request.getStartDate().format(DATE_FORMATTER);
            }
            dateRange += " - ";
            if (request.getEndDate() != null) {
                dateRange += request.getEndDate().format(DATE_FORMATTER);
            } else {
                dateRange += "Hiện tại";
            }
            dateRow.createCell(0).setCellValue(dateRange);
        }
        
        rowNum++;
        
        // Tổng kết
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> "Thu nhập".equals(t.getTransactionType().getTypeName()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpense = transactions.stream()
                .filter(t -> "Chi tiêu".equals(t.getTransactionType().getTypeName()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal balance = totalIncome.subtract(totalExpense);
        
        Row summaryHeader = sheet.createRow(rowNum++);
        summaryHeader.createCell(0).setCellValue("TỔNG KẾT");
        summaryHeader.getCell(0).setCellStyle(headerStyle);
        
        Row incomeRow = sheet.createRow(rowNum++);
        incomeRow.createCell(0).setCellValue("Tổng thu nhập:");
        incomeRow.createCell(1).setCellValue(totalIncome.doubleValue());
        incomeRow.getCell(1).setCellStyle(currencyStyle);
        
        Row expenseRow = sheet.createRow(rowNum++);
        expenseRow.createCell(0).setCellValue("Tổng chi tiêu:");
        expenseRow.createCell(1).setCellValue(totalExpense.doubleValue());
        expenseRow.getCell(1).setCellStyle(currencyStyle);
        
        Row balanceRow = sheet.createRow(rowNum++);
        balanceRow.createCell(0).setCellValue("Số dư:");
        balanceRow.createCell(1).setCellValue(balance.doubleValue());
        balanceRow.getCell(1).setCellStyle(currencyStyle);
        
        rowNum++;
        
        // Thống kê
        Row statsHeader = sheet.createRow(rowNum++);
        statsHeader.createCell(0).setCellValue("THỐNG KÊ");
        statsHeader.getCell(0).setCellStyle(headerStyle);
        
        Row walletRow = sheet.createRow(rowNum++);
        walletRow.createCell(0).setCellValue("Số ví:");
        walletRow.createCell(1).setCellValue(wallets.size());
        
        Row transactionRow = sheet.createRow(rowNum++);
        transactionRow.createCell(0).setCellValue("Số giao dịch:");
        transactionRow.createCell(1).setCellValue(transactions.size());
        
        Row budgetRow = sheet.createRow(rowNum++);
        budgetRow.createCell(0).setCellValue("Số ngân sách:");
        budgetRow.createCell(1).setCellValue(budgets.size());
        
        // Auto-size
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createTransactionSheet(Sheet sheet, Workbook workbook, List<Transaction> transactions) {
        CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.LIGHT_BLUE);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"STT", "Ngày giờ", "Loại", "Danh mục", "Ví", "Số tiền", "Ghi chú"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int stt = 1;
        for (Transaction t : transactions) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(stt++);
            row.createCell(1).setCellValue(t.getTransactionDate().format(DATETIME_FORMATTER));
            row.createCell(2).setCellValue(t.getTransactionType().getTypeName());
            row.createCell(3).setCellValue(t.getCategory().getCategoryName());
            row.createCell(4).setCellValue(t.getWallet().getWalletName());
            
            Cell amountCell = row.createCell(5);
            amountCell.setCellValue(t.getAmount().doubleValue());
            amountCell.setCellStyle(currencyStyle);
            
            row.createCell(6).setCellValue(t.getNote() != null ? t.getNote() : "");
            
            for (int i = 0; i < 7; i++) {
                if (i != 5) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createBudgetSheet(Sheet sheet, Workbook workbook, List<BudgetResponse> budgets) {
        CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"STT", "Tên ngân sách", "Ví", "Hạn mức", "Đã chi", "Còn lại", "Tỷ lệ %", "Trạng thái"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int stt = 1;
        for (BudgetResponse budget : budgets) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(stt++);
            row.createCell(1).setCellValue(budget.getCategoryName());
            row.createCell(2).setCellValue(budget.getWalletName() != null ? budget.getWalletName() : "Tất cả ví");
            
            Cell limitCell = row.createCell(3);
            limitCell.setCellValue(budget.getAmountLimit().doubleValue());
            limitCell.setCellStyle(currencyStyle);
            
            Cell spentCell = row.createCell(4);
            spentCell.setCellValue(budget.getSpentAmount().doubleValue());
            spentCell.setCellStyle(currencyStyle);
            
            Cell remainingCell = row.createCell(5);
            remainingCell.setCellValue(budget.getRemainingAmount().doubleValue());
            remainingCell.setCellStyle(currencyStyle);
            
            row.createCell(6).setCellValue(budget.getUsagePercentage());
            row.createCell(7).setCellValue(budget.getBudgetStatus());
            
            for (int i = 0; i < 8; i++) {
                if (i != 3 && i != 4 && i != 5) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Helper methods for styles
    private CellStyle createHeaderStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    // ============ PDF GENERATION ============

    private Resource generateTransactionsPDF(List<Transaction> transactions, Long userId) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 50;
                float yPosition = 750;
                float lineHeight = 20;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("BÁO CÁO GIAO DỊCH");
                contentStream.endText();
                
                yPosition -= 40;
                
                // Header
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("STT | Ngày giờ | Loại | Danh mục | Ví | Số tiền | Ghi chú");
                contentStream.endText();
                
                yPosition -= 30;
                
                // Data
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                int stt = 1;
                BigDecimal totalIncome = BigDecimal.ZERO;
                BigDecimal totalExpense = BigDecimal.ZERO;
                
                for (Transaction t : transactions) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;
                    }
                    
                    String line = String.format("%d | %s | %s | %s | %s | %s | %s",
                            stt++,
                            t.getTransactionDate().format(DATETIME_FORMATTER),
                            t.getTransactionType().getTypeName(),
                            t.getCategory().getCategoryName(),
                            t.getWallet().getWalletName(),
                            formatCurrency(t.getAmount()),
                            t.getNote() != null ? t.getNote() : "");
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(line);
                    contentStream.endText();
                    
                    yPosition -= lineHeight;
                    
                    if ("Thu nhập".equals(t.getTransactionType().getTypeName())) {
                        totalIncome = totalIncome.add(t.getAmount());
                    } else {
                        totalExpense = totalExpense.add(t.getAmount());
                    }
                }
                
                // Summary
                yPosition -= 20;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("TỔNG KẾT:");
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Tổng thu nhập: " + formatCurrency(totalIncome));
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Tổng chi tiêu: " + formatCurrency(totalExpense));
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Số dư: " + formatCurrency(totalIncome.subtract(totalExpense)));
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file PDF: " + e.getMessage(), e);
        }
    }

    private Resource generateBudgetsPDF(List<BudgetResponse> budgets, Long userId) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 50;
                float yPosition = 750;
                
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("BÁO CÁO NGÂN SÁCH");
                contentStream.endText();
                
                yPosition -= 40;
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("STT | Tên ngân sách | Ví | Hạn mức | Đã chi | Còn lại | Tỷ lệ % | Trạng thái");
                contentStream.endText();
                
                yPosition -= 30;
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                int stt = 1;
                for (BudgetResponse budget : budgets) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 750;
                    }
                    
                    String line = String.format("%d | %s | %s | %s | %s | %s | %.1f%% | %s",
                            stt++,
                            budget.getCategoryName(),
                            budget.getWalletName() != null ? budget.getWalletName() : "Tất cả ví",
                            formatCurrency(budget.getAmountLimit()),
                            formatCurrency(budget.getSpentAmount()),
                            formatCurrency(budget.getRemainingAmount()),
                            budget.getUsagePercentage(),
                            budget.getBudgetStatus());
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(line);
                    contentStream.endText();
                    
                    yPosition -= 20;
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file PDF: " + e.getMessage(), e);
        }
    }

    private Resource generateSummaryPDF(User user, List<Transaction> transactions,
                                       List<BudgetResponse> budgets, List<Wallet> wallets,
                                       ExportRequest request) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 50;
                float yPosition = 750;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("BÁO CÁO TỔNG HỢP TÀI CHÍNH");
                contentStream.endText();
                
                yPosition -= 40;
                
                // User info
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Người dùng: " + user.getFullName());
                contentStream.endText();
                
                yPosition -= 25;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Email: " + user.getEmail());
                contentStream.endText();
                
                // Date range
                if (request.getStartDate() != null || request.getEndDate() != null) {
                    yPosition -= 25;
                    String dateRange = "Khoảng thời gian: ";
                    if (request.getStartDate() != null) {
                        dateRange += request.getStartDate().format(DATE_FORMATTER);
                    }
                    dateRange += " - ";
                    if (request.getEndDate() != null) {
                        dateRange += request.getEndDate().format(DATE_FORMATTER);
                    } else {
                        dateRange += "Hiện tại";
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(dateRange);
                    contentStream.endText();
                }
                
                yPosition -= 40;
                
                // Summary
                BigDecimal totalIncome = transactions.stream()
                        .filter(t -> "Thu nhập".equals(t.getTransactionType().getTypeName()))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalExpense = transactions.stream()
                        .filter(t -> "Chi tiêu".equals(t.getTransactionType().getTypeName()))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal balance = totalIncome.subtract(totalExpense);
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("TỔNG KẾT");
                contentStream.endText();
                
                yPosition -= 25;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Tổng thu nhập: " + formatCurrency(totalIncome));
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Tổng chi tiêu: " + formatCurrency(totalExpense));
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Số dư: " + formatCurrency(balance));
                contentStream.endText();
                
                yPosition -= 40;
                
                // Statistics
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("THỐNG KÊ");
                contentStream.endText();
                
                yPosition -= 25;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Số ví: " + wallets.size());
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Số giao dịch: " + transactions.size());
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Số ngân sách: " + budgets.size());
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(out);
            return new ByteArrayResource(out.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file PDF: " + e.getMessage(), e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VND", amount.doubleValue());
    }
}

