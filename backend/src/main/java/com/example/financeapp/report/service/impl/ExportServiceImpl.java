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
import com.example.financeapp.wallet.entity.WalletTransfer;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.repository.WalletTransferRepository;
import com.example.financeapp.fund.entity.FundTransaction;
import com.example.financeapp.fund.repository.FundTransactionRepository;
import com.example.financeapp.fund.entity.FundTransactionType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Autowired
    private WalletTransferRepository walletTransferRepository;

    @Autowired
    private FundTransactionRepository fundTransactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Normalize Vietnamese text by removing diacritics (accents)
     * This is needed because Helvetica font doesn't support Vietnamese characters
     */
    private String normalizeVietnameseText(String text) {
        if (text == null) return "";

        // Vietnamese diacritics mapping
        text = text.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        text = text.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        text = text.replaceAll("[ìíịỉĩ]", "i");
        text = text.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        text = text.replaceAll("[ùúụủũưừứựửữ]", "u");
        text = text.replaceAll("[ỳýỵỷỹ]", "y");
        text = text.replaceAll("[đ]", "d");
        text = text.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        text = text.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        text = text.replaceAll("[ÌÍỊỈĨ]", "I");
        text = text.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        text = text.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        text = text.replaceAll("[ỲÝỴỶỸ]", "Y");
        text = text.replaceAll("[Đ]", "D");

        return text;
    }

    /**
     * Tính toán startDate và endDate từ range (day/week/month/year)
     */
    private void calculateDateRange(ExportRequest request) {
        if (request.getRange() == null || request.getRange().isEmpty()) {
            return; // Sử dụng startDate/endDate đã có
        }

        LocalDate now = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = null;

        switch (request.getRange().toLowerCase()) {
            case "day":
                startDate = now;
                endDate = now;
                break;
            case "week":
                // Tuần hiện tại (Thứ 2 đến Chủ nhật)
                int dayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
                startDate = now.minusDays(dayOfWeek - 1);
                endDate = startDate.plusDays(6);
                break;
            case "month":
                // Tháng hiện tại
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
                break;
            case "year":
                // Năm hiện tại
                startDate = now.withDayOfYear(1);
                endDate = now.withDayOfYear(now.lengthOfYear());
                break;
        }

        if (startDate != null && endDate != null) {
            request.setStartDate(startDate);
            request.setEndDate(endDate);
        }
    }

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
        // Tính toán date range từ range string nếu có
        calculateDateRange(request);

        // Lấy danh sách giao dịch từ database
        List<Transaction> transactions;
        if (request.getWalletId() != null) {
            // Lấy transactions của một ví cụ thể (bao gồm cả đã xóa)
            transactions = transactionRepository.findDetailedByWalletIdIncludingDeleted(request.getWalletId());
        } else {
            // Lấy tất cả transactions của user (bao gồm cả đã xóa)
            transactions = transactionRepository.findAllByUser_UserIdOrderByTransactionDateDescIncludingDeleted(userId);
        }

        // Lọc theo date range
        final LocalDate startDate = request.getStartDate();
        final LocalDate endDate = request.getEndDate();
        if (startDate != null || endDate != null) {
            transactions = transactions.stream()
                    .filter(t -> {
                        if (t.getTransactionDate() == null) return false;
                        LocalDate txDate = t.getTransactionDate().toLocalDate();
                        if (startDate != null && txDate.isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && txDate.isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Lấy transfers nếu có walletId
        List<WalletTransfer> transfers = null;
        Wallet wallet = null;
        if (request.getWalletId() != null) {
            // Lấy transfers bao gồm cả đã xóa
            transfers = walletTransferRepository.findByWalletIdIncludingDeleted(request.getWalletId());
            wallet = walletRepository.findById(request.getWalletId()).orElse(null);

            // Lọc transfers theo date range và status COMPLETED
            if (transfers != null) {
                final LocalDate finalStartDate = startDate;
                final LocalDate finalEndDate = endDate;
                transfers = transfers.stream()
                        .filter(t -> {
                            // Chỉ lấy giao dịch COMPLETED
                            if (t.getStatus() != WalletTransfer.TransferStatus.COMPLETED) {
                                return false;
                            }
                            if (t.getTransferDate() == null) return false;
                            LocalDate txDate = t.getTransferDate().toLocalDate();
                            if (finalStartDate != null && txDate.isBefore(finalStartDate)) {
                                return false;
                            }
                            if (finalEndDate != null && txDate.isAfter(finalEndDate)) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }
        }

        // Lấy fund transactions
        List<FundTransaction> fundTransactions = null;
        if (request.getWalletId() != null) {
            fundTransactions = fundTransactionRepository.findByWalletId(request.getWalletId());
        } else {
            fundTransactions = fundTransactionRepository.findByUserId(userId);
        }

        // Lọc fund transactions theo date range
        if (fundTransactions != null && (startDate != null || endDate != null)) {
            final LocalDate finalStartDate = startDate;
            final LocalDate finalEndDate = endDate;
            fundTransactions = fundTransactions.stream()
                    .filter(t -> {
                        if (t.getCreatedAt() == null) return false;
                        LocalDate txDate = t.getCreatedAt().toLocalDate();
                        if (finalStartDate != null && txDate.isBefore(finalStartDate)) {
                            return false;
                        }
                        if (finalEndDate != null && txDate.isAfter(finalEndDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        if (request.getFormat() == ExportRequest.ExportFormat.EXCEL) {
            return generateTransactionsExcel(transactions, userId);
        } else {
            return generateTransactionsPDF(transactions, transfers, fundTransactions, wallet, request);
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

    /**
     * Load TTF font from system or use fallback
     */
    private org.apache.pdfbox.pdmodel.font.PDFont loadVietnameseFont(PDDocument document) {
        try {
            // Try to load Arial from Windows system fonts
            String[] fontPaths = {
                    "C:/Windows/Fonts/arial.ttf",
                    "C:/Windows/Fonts/ARIAL.TTF",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/System/Library/Fonts/Helvetica.ttc"
            };

            for (String fontPath : fontPaths) {
                try {
                    java.io.File fontFile = new java.io.File(fontPath);
                    if (fontFile.exists()) {
                        return PDType0Font.load(document, fontFile);
                    }
                } catch (Exception e) {
                    // Continue to next font
                }
            }

            // Fallback: Use Helvetica (will need to normalize text)
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        } catch (Exception e) {
            // Fallback to Helvetica
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
    }

    private Resource generateTransactionsPDF(List<Transaction> transactions, List<WalletTransfer> transfers, List<FundTransaction> fundTransactions, Wallet wallet, ExportRequest request) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sử dụng khổ ngang (Landscape) để có nhiều không gian hơn
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);

            // Load font hỗ trợ Unicode
            org.apache.pdfbox.pdmodel.font.PDFont fontRegular = loadVietnameseFont(document);
            org.apache.pdfbox.pdmodel.font.PDFont fontBold = loadVietnameseFont(document);
            boolean useUnicodeFont = !(fontRegular instanceof PDType1Font);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 50;
                float yPosition = 550; // A4 width is 595, so start a bit lower
                float lineHeight = 20;
                float smallLineHeight = 14;

                // Table column widths (Tổng width ~750 cho Landscape)
                float col1Width = 60;   // STT
                float col2Width = 100;  // Thời gian
                float col3Width = 100;  // Loại
                float colMemberWidth = 150; // Thành viên (Tăng rộng)
                float col4Width = 180;  // Mô tả (Giảm bớt để nhường chỗ cho cột thành viên)
                float col5Width = 110;  // Số tiền
                // Bỏ cột tiền tệ

                float tableWidth = col1Width + col2Width + col3Width + colMemberWidth + col4Width + col5Width;
                float tableStartX = margin;
                float tableEndX = tableStartX + tableWidth;

                // Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset(margin, yPosition);
                String title = useUnicodeFont ? "BÁO CÁO TÀI CHÍNH" : normalizeVietnameseText("BÁO CÁO TÀI CHÍNH");
                contentStream.showText(title);
                contentStream.endText();

                yPosition -= 35;

                // Report Info
                contentStream.setFont(fontRegular, 10);

                // Wallet name
                if (wallet != null) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    String walletText = useUnicodeFont ? "Ví: " + wallet.getWalletName() : normalizeVietnameseText("Vi: " + wallet.getWalletName());
                    contentStream.showText(walletText);
                    contentStream.endText();
                    yPosition -= smallLineHeight;
                }

                // Range
                if (request.getRange() != null && !request.getRange().isEmpty()) {
                    String rangeLabel = switch (request.getRange().toLowerCase()) {
                        case "day" -> useUnicodeFont ? "Ngày" : "Ngay";
                        case "week" -> useUnicodeFont ? "Tuần" : "Tuan";
                        case "month" -> useUnicodeFont ? "Tháng" : "Thang";
                        case "year" -> useUnicodeFont ? "Năm" : "Nam";
                        default -> request.getRange();
                    };
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    String rangeText = useUnicodeFont ? "Kỳ báo cáo: " + rangeLabel : normalizeVietnameseText("Ky bao cao: " + rangeLabel);
                    contentStream.showText(rangeText);
                    contentStream.endText();
                    yPosition -= smallLineHeight;
                }

                // Date range
                if (request.getStartDate() != null && request.getEndDate() != null) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    String dateText = useUnicodeFont
                            ? "Từ ngày: " + request.getStartDate().format(DATE_FORMATTER) + " đến " + request.getEndDate().format(DATE_FORMATTER)
                            : normalizeVietnameseText("Tu ngay: " + request.getStartDate().format(DATE_FORMATTER) + " den " + request.getEndDate().format(DATE_FORMATTER));
                    contentStream.showText(dateText);
                    contentStream.endText();
                    yPosition -= smallLineHeight;
                }

                // Export date
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                String exportText = useUnicodeFont
                        ? "Ngày xuất: " + LocalDate.now().format(DATE_FORMATTER)
                        : normalizeVietnameseText("Ngay xuat: " + LocalDate.now().format(DATE_FORMATTER));
                contentStream.showText(exportText);
                contentStream.endText();
                yPosition -= smallLineHeight;

                // Total transactions
                int totalCount = transactions.size() +
                        (transfers != null ? (int)transfers.stream().filter(t -> {
                            String note = t.getNote() != null ? t.getNote().toLowerCase() : "";
                            return !note.contains("nạp vào quỹ") &&
                                    !note.contains("rút tiền từ quỹ") &&
                                    !note.contains("fund deposit") &&
                                    !note.contains("fund withdraw");
                        }).count() : 0) +
                        (fundTransactions != null ? fundTransactions.size() : 0);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                String totalText = useUnicodeFont
                        ? "Tổng số giao dịch: " + totalCount
                        : normalizeVietnameseText("Tong so giao dich: " + totalCount);
                contentStream.showText(totalText);
                contentStream.endText();
                yPosition -= 25;

                // Draw table border
                float tableTopY = yPosition + 5;
                float tableBottomY = yPosition - 15;

                // Table header background
                contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                contentStream.addRect(tableStartX, tableBottomY, tableWidth, 25);
                contentStream.fill();
                contentStream.setNonStrokingColor(0, 0, 0);

                // Table header text
                contentStream.setFont(fontBold, 10);
                float headerY = tableBottomY + 8;
                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + 5, headerY);
                contentStream.showText(useUnicodeFont ? "STT" : "STT");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + col1Width + 5, headerY);
                contentStream.showText(useUnicodeFont ? "Thời gian" : "Thoi gian");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + 5, headerY);
                contentStream.showText(useUnicodeFont ? "Loại" : "Loai");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + 5, headerY);
                contentStream.showText(useUnicodeFont ? "Thành viên" : "Thanh vien");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + colMemberWidth + 5, headerY);
                contentStream.showText(useUnicodeFont ? "Mô tả" : "Mo ta");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + colMemberWidth + col4Width + 5, headerY);
                contentStream.showText(useUnicodeFont ? "Số tiền" : "So tien");
                contentStream.endText();

                // Draw table borders
                contentStream.setStrokingColor(0, 0, 0);
                contentStream.setLineWidth(0.5f);

                // Outer border
                contentStream.addRect(tableStartX, tableBottomY, tableWidth, tableTopY - tableBottomY);
                contentStream.stroke();

                // Vertical lines
                float x = tableStartX + col1Width;
                contentStream.moveTo(x, tableBottomY);
                contentStream.lineTo(x, tableTopY);
                x += col2Width;
                contentStream.moveTo(x, tableBottomY);
                contentStream.lineTo(x, tableTopY);
                x += col3Width;
                contentStream.moveTo(x, tableBottomY);
                contentStream.lineTo(x, tableTopY);
                x += colMemberWidth;
                contentStream.moveTo(x, tableBottomY);
                contentStream.lineTo(x, tableTopY);
                x += col4Width;
                contentStream.moveTo(x, tableBottomY);
                contentStream.lineTo(x, tableTopY);
                // Removed vertical line for currency column
                contentStream.stroke();

                // Header bottom line
                contentStream.moveTo(tableStartX, tableBottomY + 25);
                contentStream.lineTo(tableEndX, tableBottomY + 25);
                contentStream.stroke();

                yPosition = tableBottomY;

                // Data rows
                contentStream.setFont(fontRegular, 9);
                int stt = 1;
                BigDecimal totalIncome = BigDecimal.ZERO;
                BigDecimal totalExpense = BigDecimal.ZERO;
                BigDecimal totalReceivedFromOther = BigDecimal.ZERO;
                BigDecimal totalTransferredToOther = BigDecimal.ZERO;

                // Combine transactions and transfers, sort by date
                List<Object> allItems = new ArrayList<>();
                allItems.addAll(transactions);
                if (transfers != null) {
                    // Filter out transfers related to fund transactions to avoid duplication
                    List<WalletTransfer> filteredTransfers = transfers.stream()
                            .filter(t -> {
                                String note = t.getNote() != null ? t.getNote().toLowerCase() : "";
                                return !note.contains("nạp vào quỹ") &&
                                        !note.contains("rút tiền từ quỹ") &&
                                        !note.contains("fund deposit") &&
                                        !note.contains("fund withdraw");
                            })
                            .collect(Collectors.toList());
                    allItems.addAll(filteredTransfers);
                }
                if (fundTransactions != null) {
                    allItems.addAll(fundTransactions);
                }

                // Sort by date (descending)
                allItems.sort((a, b) -> {
                    LocalDateTime dateA = null;
                    if (a instanceof Transaction) dateA = ((Transaction) a).getTransactionDate();
                    else if (a instanceof WalletTransfer) dateA = ((WalletTransfer) a).getTransferDate();
                    else if (a instanceof FundTransaction) dateA = ((FundTransaction) a).getCreatedAt();

                    LocalDateTime dateB = null;
                    if (b instanceof Transaction) dateB = ((Transaction) b).getTransactionDate();
                    else if (b instanceof WalletTransfer) dateB = ((WalletTransfer) b).getTransferDate();
                    else if (b instanceof FundTransaction) dateB = ((FundTransaction) b).getCreatedAt();

                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA); // Descending
                });

                for (Object item : allItems) {
                    if (yPosition < 80) {
                        contentStream.close();
                        page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        fontRegular = loadVietnameseFont(document);
                        fontBold = loadVietnameseFont(document);
                        useUnicodeFont = !(fontRegular instanceof PDType1Font);
                        contentStream.setFont(fontRegular, 9);
                        yPosition = 500; // Reset yPosition for new page (Landscape)
                        tableTopY = yPosition;
                        tableBottomY = yPosition - 15;
                    }

                    String dateTimeStr = "";
                    String typeStr = "";
                    String description = "";
                    String amountStr = "";
                    String currency = "";
                    String memberName = "";
                    boolean isDeleted = false;
                    boolean isEdited = false;

                    if (item instanceof Transaction) {
                        Transaction t = (Transaction) item;
                        isDeleted = Boolean.TRUE.equals(t.getIsDeleted());
                        isEdited = Boolean.TRUE.equals(t.getIsEdited());

                        // Fix timezone: Transaction date is stored in UTC, convert to GMT+7 for display
                        dateTimeStr = t.getTransactionDate() != null
                                ? t.getTransactionDate().plusHours(7).format(DATETIME_FORMATTER)
                                : "";
                        typeStr = t.getTransactionType() != null ? t.getTransactionType().getTypeName() : "";
                        description = t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : "-";
                        amountStr = formatCurrency(t.getAmount());
                        currency = t.getWallet() != null ? t.getWallet().getCurrencyCode() : "VND";
                        memberName = t.getUser() != null ? t.getUser().getEmail() : "";

                        // Check transaction type
                        if ("Thu nhập".equals(typeStr)) {
                            if (!isDeleted) totalIncome = totalIncome.add(t.getAmount());
                            amountStr = "+" + amountStr;
                        } else {
                            if (!isDeleted) totalExpense = totalExpense.add(t.getAmount());
                            amountStr = "-" + amountStr;
                        }

                        // Normalize only if not using Unicode font
                        if (!useUnicodeFont) {
                            typeStr = normalizeVietnameseText(typeStr);
                            description = normalizeVietnameseText(description);
                            memberName = normalizeVietnameseText(memberName);
                        }
                    } else if (item instanceof WalletTransfer) {
                        WalletTransfer t = (WalletTransfer) item;
                        isDeleted = Boolean.TRUE.equals(t.getIsDeleted());
                        isEdited = Boolean.TRUE.equals(t.getIsEdited());

                        dateTimeStr = t.getTransferDate() != null
                                ? t.getTransferDate().format(DATETIME_FORMATTER)
                                : "";

                        String fromWalletName = t.getFromWallet() != null ? t.getFromWallet().getWalletName() : "";
                        String toWalletName = t.getToWallet() != null ? t.getToWallet().getWalletName() : "";
                        memberName = t.getUser() != null ? t.getUser().getEmail() : "";

                        boolean isIncoming = false;
                        if (wallet != null) {
                            if (t.getToWallet() != null && t.getToWallet().getWalletId().equals(wallet.getWalletId())) {
                                isIncoming = true;
                            }
                        }

                        if (isIncoming) {
                            typeStr = useUnicodeFont ? "Nhận từ ví khác" : normalizeVietnameseText("Nhan tu vi khac");
                            description = (useUnicodeFont ? "Từ: " : "Tu: ") + fromWalletName;
                            if (!isDeleted) totalReceivedFromOther = totalReceivedFromOther.add(t.getAmount());
                            amountStr = "+" + formatCurrency(t.getAmount());
                        } else {
                            typeStr = useUnicodeFont ? "Chuyển sang ví khác" : normalizeVietnameseText("Chuyen sang vi khac");
                            description = (useUnicodeFont ? "Đến: " : "Den: ") + toWalletName;
                            if (!isDeleted) totalTransferredToOther = totalTransferredToOther.add(t.getAmount());
                            amountStr = "-" + formatCurrency(t.getAmount());
                        }

                        if (!useUnicodeFont) {
                            fromWalletName = normalizeVietnameseText(fromWalletName);
                            toWalletName = normalizeVietnameseText(toWalletName);
                            description = normalizeVietnameseText(description);
                            memberName = normalizeVietnameseText(memberName);
                        }

                        currency = t.getCurrencyCode() != null ? t.getCurrencyCode() : "VND";
                    } else if (item instanceof FundTransaction) {
                        FundTransaction t = (FundTransaction) item;
                        // FundTransaction doesn't have soft delete flag exposed here easily, assume active

                        dateTimeStr = t.getCreatedAt() != null
                                ? t.getCreatedAt().format(DATETIME_FORMATTER)
                                : "";

                        String fundName = t.getFund() != null ? t.getFund().getFundName() : "Quỹ";
                        memberName = t.getPerformedBy() != null ? t.getPerformedBy().getEmail() : "";

                        boolean isIncoming = false;
                        if (wallet != null && t.getFund() != null && t.getFund().getSourceWallet() != null) {
                            // Withdraw: Fund -> Wallet (Incoming for Wallet)
                            if (t.getType() == FundTransactionType.WITHDRAW) {
                                if (t.getFund().getSourceWallet().getWalletId().equals(wallet.getWalletId())) {
                                    isIncoming = true;
                                }
                            }
                            // Deposit: Wallet -> Fund (Outgoing for Wallet)
                            else if (t.getType() == FundTransactionType.DEPOSIT || t.getType() == FundTransactionType.AUTO_DEPOSIT || t.getType() == FundTransactionType.AUTO_DEPOSIT_RECOVERY) {
                                if (t.getFund().getSourceWallet().getWalletId().equals(wallet.getWalletId())) {
                                    isIncoming = false;
                                }
                            }
                        }

                        if (isIncoming) {
                            typeStr = useUnicodeFont ? "Nhận từ ví khác" : normalizeVietnameseText("Nhan tu vi khac");
                            description = (useUnicodeFont ? "Từ: " : "Tu: ") + fundName + (useUnicodeFont ? " - Ví Quỹ" : " - Vi Quy");
                            totalReceivedFromOther = totalReceivedFromOther.add(t.getAmount());
                            amountStr = "+" + formatCurrency(t.getAmount());
                        } else {
                            typeStr = useUnicodeFont ? "Chuyển sang ví khác" : normalizeVietnameseText("Chuyen sang vi khac");
                            description = (useUnicodeFont ? "Đến: " : "Den: ") + fundName + (useUnicodeFont ? " - Ví Quỹ" : " - Vi Quy");
                            totalTransferredToOther = totalTransferredToOther.add(t.getAmount());
                            amountStr = "-" + formatCurrency(t.getAmount());
                        }

                        if (!useUnicodeFont) {
                            fundName = normalizeVietnameseText(fundName);
                            description = normalizeVietnameseText(description);
                            memberName = normalizeVietnameseText(memberName);
                        }

                        currency = t.getFund() != null && t.getFund().getTargetWallet() != null ? t.getFund().getTargetWallet().getCurrencyCode() : "VND";
                    }

                    // Truncate long text
                    if (dateTimeStr.length() > 16) dateTimeStr = dateTimeStr.substring(0, 16);
                    if (typeStr.length() > 25) typeStr = typeStr.substring(0, 25) + "...";
                    if (memberName.length() > 30) memberName = memberName.substring(0, 30) + "...";
                    if (description.length() > 40) description = description.substring(0, 40) + "..."; // Giảm giới hạn ký tự cho mô tả vì đã thêm cột thành viên

                    // Draw row border
                    yPosition -= lineHeight;
                    contentStream.moveTo(tableStartX, yPosition);
                    contentStream.lineTo(tableEndX, yPosition);
                    contentStream.stroke();

                    // Write row data
                    float rowY = yPosition + 5;

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + 5, rowY);
                    String sttStr = String.valueOf(stt++);
                    if (isDeleted) {
                        sttStr += useUnicodeFont ? " (đã xoá)" : " (da xoa)";
                    } else if (isEdited) {
                        sttStr += useUnicodeFont ? " (đã sửa)" : " (da sua)";
                    }
                    contentStream.showText(sttStr);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + col1Width + 5, rowY);
                    contentStream.showText(dateTimeStr);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + 5, rowY);
                    contentStream.showText(typeStr);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + 5, rowY);
                    contentStream.showText(memberName);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + colMemberWidth + 5, rowY);
                    contentStream.showText(description);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + col1Width + col2Width + col3Width + colMemberWidth + col4Width + 5, rowY);
                    contentStream.showText(amountStr);
                    contentStream.endText();

                    // Removed currency column data
                }

                // Summary
                yPosition -= 15;
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(550, yPosition);
                contentStream.stroke();
                yPosition -= 15;

                contentStream.setFont(fontBold, 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useUnicodeFont ? "TỔNG KẾT:" : "TONG KET:");
                contentStream.endText();

                yPosition -= 20;
                contentStream.setFont(fontRegular, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useUnicodeFont ? "Thu vào: " + formatCurrency(totalIncome) : normalizeVietnameseText("Thu vao: " + formatCurrency(totalIncome)));
                contentStream.endText();

                yPosition -= 18;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useUnicodeFont ? "Chi ra: " + formatCurrency(totalExpense) : normalizeVietnameseText("Chi ra: " + formatCurrency(totalExpense)));
                contentStream.endText();

                if (totalReceivedFromOther.compareTo(BigDecimal.ZERO) > 0) {
                    yPosition -= 18;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(useUnicodeFont ? "Nhận từ ví khác: " + formatCurrency(totalReceivedFromOther) : normalizeVietnameseText("Nhan tu vi khac: " + formatCurrency(totalReceivedFromOther)));
                    contentStream.endText();
                }

                if (totalTransferredToOther.compareTo(BigDecimal.ZERO) > 0) {
                    yPosition -= 18;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(useUnicodeFont ? "Chuyển sang ví khác: " + formatCurrency(totalTransferredToOther) : normalizeVietnameseText("Chuyen sang vi khac: " + formatCurrency(totalTransferredToOther)));
                    contentStream.endText();
                }

                yPosition -= 18;
                BigDecimal balance = totalIncome.subtract(totalExpense).add(totalReceivedFromOther).subtract(totalTransferredToOther);
                contentStream.setFont(fontBold, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useUnicodeFont ? "Còn lại: " + formatCurrency(balance) : normalizeVietnameseText("Con lai: " + formatCurrency(balance)));
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

