package com.example.financeapp.chat.service.impl;

import com.example.financeapp.chat.dto.ChatRequest;
import com.example.financeapp.chat.dto.ChatResponse;
import com.example.financeapp.chat.entity.ChatMessage;
import com.example.financeapp.chat.repository.ChatMessageRepository;
import com.example.financeapp.chat.service.ChatService;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.service.TransactionService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import com.example.financeapp.wallet.dto.response.SharedWalletDTO;
import com.example.financeapp.wallet.service.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation của ChatService sử dụng Google Gemini API
 * Model: gemini-1.5-flash
 * API Version: v1beta
 * URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
 */
@Service
public class ChatServiceImpl implements ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    
    // Model mới: gemini-2.5-flash (tài khoản đã nâng cấp lên dòng 2.5 và 3.0)
    // Không còn hỗ trợ dòng 1.5 cũ nữa
    private static final String[] GEMINI_MODELS = {
        "gemini-2.5-flash",       // Ưu tiên 1: Model mới, nhanh, rẻ
        "gemini-2.5-pro",         // Ưu tiên 2: Model mới, chất lượng cao
        "gemini-3.0-flash",       // Ưu tiên 3: Model mới nhất (nếu khả dụng)
        "gemini-3.0-pro"          // Ưu tiên 4: Model mới nhất, chất lượng cao nhất (nếu khả dụng)
    };
    private static final String[] GEMINI_API_VERSIONS = {
        "v1beta",                 // gemini-2.5-flash với v1beta
        "v1beta",                 // gemini-2.5-pro với v1beta
        "v1beta",                 // gemini-3.0-flash với v1beta
        "v1beta"                  // gemini-3.0-pro với v1beta
    };
    
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;
    
    /**
     * Tạo System Prompt với context data của user
     */
    private String buildSystemPrompt(String userContext) {
        return """
            Vai trò: Bạn là Trợ lí tài chính cá nhân, hỗ trợ người dùng quản lý tài chính một cách thông minh và hiệu quả.
            
            Nhiệm vụ: 
            - Trả lời các câu hỏi về tài chính dựa trên dữ liệu thực tế của người dùng
            - Đưa ra lời khuyên tài chính phù hợp và thực tế
            - Hướng dẫn sử dụng các tính năng của ứng dụng
            - Giúp người dùng hiểu rõ tình hình tài chính của mình
            
            """ + userContext + """
            
            Các chức năng chính của ứng dụng MyWallet:
            
            1. **Ví (Wallet)**:
               - Tạo ví cá nhân, ví nhóm (chia sẻ với người khác)
               - Nạp tiền vào ví, rút tiền từ ví
               - Chuyển tiền giữa các ví
               - Quản lý ví nhóm: mời thành viên, phân quyền (OWNER, MEMBER, VIEWER), xóa thành viên
               - Gộp ví (merge wallets): gộp nhiều ví thành một
               - Đặt ví mặc định
               - Hỗ trợ nhiều loại tiền tệ (VND, USD, ...)
            
            2. **Giao dịch (Transaction)**:
               - Tạo giao dịch thu nhập (income) và chi tiêu (expense)
               - Xem lịch sử giao dịch theo ví, theo thời gian
               - Chỉnh sửa, xóa giao dịch
               - Phân loại giao dịch theo danh mục (category)
               - Gắn ghi chú (note) cho giao dịch
            
            3. **Ngân sách (Budget)**:
               - Đặt hạn mức chi tiêu theo danh mục và theo thời gian (tuần, tháng, năm)
               - Theo dõi ngân sách: xem đã chi bao nhiêu, còn lại bao nhiêu
               - Cảnh báo khi vượt quá ngân sách
               - Xem báo cáo ngân sách theo thời gian
            
            4. **Báo cáo (Report/Dashboard)**:
               - Tổng quan tài chính: tổng thu, tổng chi, số dư hiện tại
               - Biểu đồ tròn (donut chart): phân tích chi tiêu theo danh mục
               - Biểu đồ đường (line chart): xu hướng thu chi theo thời gian
               - Biểu đồ cột (bar chart): so sánh thu chi theo thời gian
               - Xem báo cáo theo tuần, tháng, năm
               - Xuất báo cáo ra file (Excel, PDF)
            
            5. **Danh mục (Category)**:
               - Quản lý danh mục chi tiêu và thu nhập
               - Tạo, chỉnh sửa, xóa danh mục
               - Gán icon và màu sắc cho danh mục
               - Phân loại giao dịch theo danh mục
            
            6. **Quỹ tiết kiệm (Fund)**:
               - Tạo quỹ tiết kiệm với mục tiêu số tiền và thời hạn
               - Tự động trích tiền từ ví vào quỹ tiết kiệm
               - Theo dõi tiến độ tiết kiệm
               - Quỹ cá nhân và quỹ nhóm (chia sẻ với người khác)
               - Hoàn thành quỹ khi đạt mục tiêu
            
            7. **Lịch sử hoạt động (Activity History)**:
               - Xem log tất cả các hoạt động trong ứng dụng
               - Theo dõi các thay đổi: tạo ví, tạo giao dịch, cập nhật ngân sách, ...
            
            8. **Thông báo (Notification)**:
               - Nhận thông báo về giao dịch mới, ngân sách vượt quá, ...
               - Thông báo về ví nhóm: mời tham gia, bị xóa khỏi ví, ...
            
            Hướng dẫn trả lời:
            - Trả lời ngắn gọn, thân thiện, dùng Tiếng Việt
            - Nếu người dùng hỏi về số dư, thu chi, giao dịch, hãy lấy từ dữ liệu ngữ cảnh trên để trả lời
            - Đừng nhắc đến việc "dựa trên dữ liệu được cung cấp", hãy trả lời tự nhiên như bạn tự biết
            - Nếu không có dữ liệu, hãy thừa nhận và hướng dẫn người dùng sử dụng ứng dụng
            - Luôn giữ câu trả lời ngắn gọn (tối đa 200 từ), dễ hiểu và thân thiện
            """;
    }
    
    public ChatServiceImpl(
            RestTemplate restTemplate, 
            ObjectMapper objectMapper,
            WalletService walletService,
            TransactionService transactionService,
            UserRepository userRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
    }
    
    /**
     * Lấy API URL từ cấu hình với model và version cụ thể
     */
    private String getApiUrl(String model, String version) {
        return GEMINI_API_BASE_URL + "/" + version + "/models/" + model + ":generateContent?key=" + geminiApiKey;
    }
    
    /**
     * List các models khả dụng (để debug)
     */
    private void listAvailableModels() {
        try {
            String listUrl = GEMINI_API_BASE_URL + "/v1beta/models?key=" + geminiApiKey;
            logger.info("Listing available models from: {}", listUrl.replaceAll("key=[^&]*", "key=***"));
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                listUrl, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            String responseBody = response.getBody();
            if (responseBody != null) {
                logger.info("Available models: {}", responseBody);
            }
        } catch (Exception e) {
            logger.warn("Could not list models: {}", e.getMessage());
        }
    }
    
    /**
     * Lấy context data của user (tên, số dư, thu/chi, giao dịch gần nhất)
     */
    private String getUserContextData(Long userId) {
        try {
            StringBuilder context = new StringBuilder();
            
            // 1. Lấy thông tin user
            User user = userRepository.findById(userId).orElse(null);
            String userName = user != null ? user.getFullName() : "Người dùng";
            context.append("Dữ liệu ngữ cảnh của người dùng hiện tại:\n");
            context.append("- Tên: ").append(userName).append("\n");
            
            // 2. Lấy tổng số dư từ tất cả ví
            List<SharedWalletDTO> wallets = walletService.getAllAccessibleWallets(userId);
            BigDecimal totalBalance = BigDecimal.ZERO;
            Map<String, BigDecimal> balanceByCurrency = new HashMap<>();
            
            for (SharedWalletDTO wallet : wallets) {
                BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
                String currency = wallet.getCurrencyCode() != null ? wallet.getCurrencyCode() : "VND";
                totalBalance = totalBalance.add(balance);
                balanceByCurrency.put(currency, 
                    balanceByCurrency.getOrDefault(currency, BigDecimal.ZERO).add(balance));
            }
            
            context.append("- Tổng số dư: ");
            if (balanceByCurrency.isEmpty()) {
                context.append("0 VND\n");
            } else {
                List<String> balanceStrings = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : balanceByCurrency.entrySet()) {
                    balanceStrings.add(entry.getValue().stripTrailingZeros().toPlainString() + " " + entry.getKey());
                }
                context.append(String.join(", ", balanceStrings)).append("\n");
            }
            
            // 3. Lấy tổng thu/chi trong tháng này
            LocalDate now = LocalDate.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
            
            List<Transaction> allTransactions = transactionService.getAllTransactions(userId);
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;
            
            for (Transaction tx : allTransactions) {
                if (tx.getTransactionDate() == null) continue;
                LocalDateTime txDate = tx.getTransactionDate();
                if (txDate.isBefore(startOfMonth) || txDate.isAfter(endOfMonth)) continue;
                
                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                String typeName = tx.getTransactionType() != null ? tx.getTransactionType().getTypeName() : "";
                
                if ("Thu nhập".equals(typeName)) {
                    totalIncome = totalIncome.add(amount);
                } else if ("Chi tiêu".equals(typeName)) {
                    totalExpense = totalExpense.add(amount);
                }
            }
            
            context.append("- Thu nhập tháng này: ")
                   .append(totalIncome.stripTrailingZeros().toPlainString()).append(" VND\n");
            context.append("- Chi tiêu tháng này: ")
                   .append(totalExpense.stripTrailingZeros().toPlainString()).append(" VND\n");
            
            // 4. Lấy 5 giao dịch gần nhất
            List<Transaction> recentTransactions = allTransactions.stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getTransactionDate() != null ? a.getTransactionDate() : LocalDateTime.MIN;
                    LocalDateTime dateB = b.getTransactionDate() != null ? b.getTransactionDate() : LocalDateTime.MIN;
                    return dateB.compareTo(dateA); // Sắp xếp giảm dần (mới nhất trước)
                })
                .limit(5)
                .toList();
            
            context.append("- Giao dịch gần đây:\n");
            if (recentTransactions.isEmpty()) {
                context.append("  + Chưa có giao dịch nào\n");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (Transaction tx : recentTransactions) {
                    String typeName = tx.getTransactionType() != null ? tx.getTransactionType().getTypeName() : "Giao dịch";
                    BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                    String category = tx.getCategory() != null ? tx.getCategory().getCategoryName() : "Không có";
                    String note = tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : "";
                    String dateStr = tx.getTransactionDate() != null 
                        ? tx.getTransactionDate().format(formatter) 
                        : "N/A";
                    
                    context.append("  + ").append(typeName)
                           .append(": ").append(amount.stripTrailingZeros().toPlainString())
                           .append(" VND - ").append(category);
                    if (!note.isEmpty()) {
                        context.append(" (").append(note).append(")");
                    }
                    context.append(" - ").append(dateStr).append("\n");
                }
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error getting user context data for user {}: {}", userId, e.getMessage());
            return "Dữ liệu ngữ cảnh của người dùng hiện tại:\n" +
                   "- Tên: Người dùng\n" +
                   "- Tổng số dư: Đang tải...\n" +
                   "- Thu nhập tháng này: Đang tải...\n" +
                   "- Chi tiêu tháng này: Đang tải...\n" +
                   "- Giao dịch gần đây: Đang tải...\n";
        }
    }
    
    /**
     * Tạo Header Content-Type: application/json
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    /**
     * Tạo Body JSON đúng chuẩn DTO với conversation history
     * Format: { "contents": [{ "role": "user"/"model", "parts": [{"text": "..."}] }] }
     * 
     * Lưu ý: System prompt chỉ được thêm vào đầu conversation (khi history rỗng),
     * không thêm vào mỗi lần gọi để tránh reset context.
     */
    private Map<String, Object> createRequestBody(String systemPrompt, List<ChatRequest.ChatMessage> history, String currentMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // Tạo contents array
        List<Map<String, Object>> contents = new ArrayList<>();
        
        // Thêm system prompt vào đầu conversation CHỈ KHI history rỗng (lần đầu tiên)
        // Nếu đã có history, nghĩa là đây không phải lần đầu, không cần thêm system prompt nữa
        boolean isFirstMessage = (history == null || history.isEmpty());
        
        if (isFirstMessage && systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemContent = new HashMap<>();
            systemContent.put("role", "user");
            List<Map<String, String>> systemParts = new ArrayList<>();
            Map<String, String> systemPart = new HashMap<>();
            systemPart.put("text", systemPrompt);
            systemParts.add(systemPart);
            systemContent.put("parts", systemParts);
            contents.add(systemContent);
            
            // Thêm response từ model để đánh dấu system prompt đã được xử lý
            Map<String, Object> modelResponse = new HashMap<>();
            modelResponse.put("role", "model");
            List<Map<String, String>> modelParts = new ArrayList<>();
            Map<String, String> modelPart = new HashMap<>();
            modelPart.put("text", "Đã hiểu. Tôi sẵn sàng hỗ trợ bạn.");
            modelParts.add(modelPart);
            modelResponse.put("parts", modelParts);
            contents.add(modelResponse);
        }
        
        // Thêm conversation history nếu có (quan trọng: giữ nguyên context)
        if (history != null && !history.isEmpty()) {
            for (ChatRequest.ChatMessage msg : history) {
                Map<String, Object> historyContent = new HashMap<>();
                // Đảm bảo role đúng format: "user" hoặc "model"
                String role = msg.getRole();
                if (!"user".equals(role) && !"model".equals(role)) {
                    // Nếu role không đúng, tự động convert
                    role = "user".equals(role.toLowerCase()) ? "user" : "model";
                }
                historyContent.put("role", role);
                
                List<Map<String, String>> parts = new ArrayList<>();
                Map<String, String> part = new HashMap<>();
                part.put("text", msg.getContent() != null ? msg.getContent() : "");
                parts.add(part);
                historyContent.put("parts", parts);
                contents.add(historyContent);
            }
        }
        
        // Thêm message hiện tại của người dùng
        Map<String, Object> currentContent = new HashMap<>();
        currentContent.put("role", "user");
        List<Map<String, String>> currentParts = new ArrayList<>();
        Map<String, String> currentPart = new HashMap<>();
        currentPart.put("text", currentMessage != null ? currentMessage : "");
        currentParts.add(currentPart);
        currentContent.put("parts", currentParts);
        contents.add(currentContent);
        
        requestBody.put("contents", contents);
        
        // Log để debug (chỉ log khi cần)
        if (logger.isDebugEnabled()) {
            logger.debug("Request body contents count: {}, isFirstMessage: {}", contents.size(), isFirstMessage);
        }
        
        // Cấu hình generation
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 4096); // Tăng từ 1024 lên 4096 để hỗ trợ câu trả lời dài
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    /**
     * Gửi POST request đến Gemini API với một model cụ thể
     */
    private String callGeminiApi(String model, String version, String systemPrompt, List<ChatRequest.ChatMessage> history, String currentMessage) throws Exception {
        // Lấy URL từ cấu hình
        String apiUrl = getApiUrl(model, version);
        logger.info("Trying Gemini API with model {} ({}): {}", model, version, apiUrl.replaceAll("key=[^&]*", "key=***"));
        
        // Tạo Header
        HttpHeaders headers = createHeaders();
        
        // Tạo Body JSON đúng chuẩn với history
        Map<String, Object> requestBody = createRequestBody(systemPrompt, history, currentMessage);
        
        // Tạo HttpEntity với headers và body
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Gửi POST request
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        
        // Parse response
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new RuntimeException("Empty response from API");
        }
        
        logger.debug("Gemini API response: {}", responseBody);
        
        // Parse JSON response
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        
        // Kiểm tra lỗi
        if (jsonNode.has("error")) {
            JsonNode errorNode = jsonNode.get("error");
            String errorMessage = errorNode.has("message") 
                ? errorNode.get("message").asText() 
                : errorNode.toString();
            throw new RuntimeException("Gemini API error: " + errorMessage);
        }
        
        // Lấy text response từ cấu trúc nested JSON
        // Format: { "candidates": [{ "content": { "parts": [{"text": "..."}] } }] }
        JsonNode candidates = jsonNode.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("No candidates in response");
        }
        
        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.get("content");
        if (content == null) {
            throw new RuntimeException("No content in candidate");
        }
        
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.size() == 0) {
            throw new RuntimeException("No parts in content");
        }
        
        String aiResponse = parts.get(0).get("text").asText();
        logger.info("Successfully received response from Gemini API using model: {}", model);
        
        return aiResponse;
    }
    
    /**
     * Gửi POST request đến Gemini API và nhận response
     * Thử các model theo thứ tự fallback nếu model trước không khả dụng
     * @param systemPrompt System prompt với context data
     * @param history Conversation history
     * @param currentMessage Câu hỏi hiện tại của người dùng
     * @return Câu trả lời từ AI
     */
    public String getChatResponse(String systemPrompt, List<ChatRequest.ChatMessage> history, String currentMessage) {
        // Kiểm tra API key
        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.equals("YOUR_NEW_GEMINI_API_KEY_HERE")) {
            logger.error("Gemini API key chưa được cấu hình hoặc chưa được thay thế");
            throw new RuntimeException("API key chưa được cấu hình. Vui lòng cập nhật API key trong application.properties. Xem hướng dẫn tại: GEMINI_API_KEY_SETUP.md");
        }
        
        // List models khả dụng (để debug - chỉ log một lần)
        // Comment out nếu không cần debug
        // listAvailableModels();
        
        // Thử các model theo thứ tự fallback
        Exception lastException = null;
        for (int i = 0; i < GEMINI_MODELS.length; i++) {
            String model = GEMINI_MODELS[i];
            String version = GEMINI_API_VERSIONS[i];
            
            try {
                return callGeminiApi(model, version, systemPrompt, history, currentMessage);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                int statusCode = e.getStatusCode().value();
                String errorBody = e.getResponseBodyAsString();
                lastException = e;
                
                // Nếu là lỗi 404 (model không tồn tại), thử model tiếp theo
                if (statusCode == 404) {
                    logger.warn("Model {} không khả dụng trong {}, thử model tiếp theo...", model, version);
                    continue;
                }
                
                // Nếu là lỗi khác (400, 401, 403, 500...), log và throw
                logger.error("Gemini API HTTP error {} với model {}: {}", statusCode, model, errorBody);
                if (statusCode >= 400 && statusCode < 500) {
                    throw new RuntimeException("Client error " + statusCode + ": " + errorBody, e);
                } else if (statusCode >= 500) {
                    throw new RuntimeException("Server error " + statusCode + ": " + errorBody, e);
                } else {
                    throw new RuntimeException("HTTP error " + statusCode + ": " + errorBody, e);
                }
            } catch (Exception e) {
                lastException = e;
                // Nếu là lỗi về model không tồn tại, thử model tiếp theo
                if (e.getMessage() != null && e.getMessage().contains("not found")) {
                    logger.warn("Model {} không khả dụng, thử model tiếp theo...", model);
                    continue;
                }
                // Nếu là lỗi khác, throw ngay
                logger.error("Exception calling Gemini API with model {}: {}", model, e.getMessage());
                throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
            }
        }
        
        // Nếu tất cả model đều thất bại
        String errorMessage = "Không thể kết nối đến Gemini API. Tất cả các model đều không khả dụng.";
        if (lastException != null) {
            String lastError = lastException.getMessage();
            if (lastError != null && lastError.contains("404")) {
                errorMessage += "\n\nCó thể do:\n" +
                    "1. API key chưa được enable 'Generative Language API' trong Google Cloud Console\n" +
                    "2. API key không có quyền truy cập các model Gemini\n" +
                    "3. Model names không đúng (đã thử: " + String.join(", ", GEMINI_MODELS) + ")\n\n" +
                    "Vui lòng kiểm tra:\n" +
                    "- Vào Google Cloud Console: https://console.cloud.google.com/\n" +
                    "- Enable 'Generative Language API'\n" +
                    "- Kiểm tra API key có quyền truy cập\n" +
                    "- Hoặc tạo API key mới tại: https://makersuite.google.com/app/apikey";
            }
            logger.error("Tất cả các model đều không khả dụng. Lỗi cuối cùng: {}", lastError);
        } else {
            logger.error("Tất cả các model đều không khả dụng. Không có exception details.");
        }
        throw new RuntimeException(errorMessage, lastException);
    }
    
    @Override
    public ChatResponse chat(Long userId, ChatRequest request) {
        try {
            // Bước 1: Lấy user từ database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Bước 2: Lấy context data của user
            String userContext = getUserContextData(userId);
            
            // Bước 3: Tạo System Prompt với context data
            String systemPrompt = buildSystemPrompt(userContext);
            
            // Bước 4: Lấy message hiện tại và history
            String currentMessage = request.getMessage();
            List<ChatRequest.ChatMessage> history = request.getHistory();
            
            // Bước 5: Lưu tin nhắn của user vào database
            ChatMessage userMessage = new ChatMessage(user, ChatMessage.MessageRole.USER, currentMessage);
            chatMessageRepository.save(userMessage);
            logger.debug("Saved user message to database for user: {}", userId);
            
            // Bước 6: Gọi hàm getChatResponse để lấy câu trả lời từ AI
            // System prompt sẽ được thêm vào đầu conversation (chỉ một lần)
            String aiResponse = getChatResponse(systemPrompt, history, currentMessage);
            
            // Bước 7: Lưu tin nhắn của AI vào database
            ChatMessage aiMessage = new ChatMessage(user, ChatMessage.MessageRole.MODEL, aiResponse);
            chatMessageRepository.save(aiMessage);
            logger.debug("Saved AI message to database for user: {}", userId);
            
            return new ChatResponse(aiResponse, true, null);
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API for user {}: {}", userId, e.getMessage(), e);
            return new ChatResponse(
                "Xin lỗi, đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.",
                false,
                e.getMessage()
            );
        }
    }
    
    /**
     * Lấy lịch sử chat của user từ database
     * @param userId ID của user
     * @return Danh sách tin nhắn (dạng ChatRequest.ChatMessage để frontend dùng)
     */
    public List<ChatRequest.ChatMessage> getChatHistory(Long userId) {
        try {
            List<ChatMessage> dbMessages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId);
            
            // Convert từ ChatMessage entity sang ChatRequest.ChatMessage DTO
            List<ChatRequest.ChatMessage> history = new ArrayList<>();
            for (ChatMessage msg : dbMessages) {
                ChatRequest.ChatMessage chatMsg = new ChatRequest.ChatMessage();
                chatMsg.setRole(msg.getRole() == ChatMessage.MessageRole.USER ? "user" : "model");
                chatMsg.setContent(msg.getContent());
                history.add(chatMsg);
            }
            
            logger.debug("Loaded {} chat messages from database for user: {}", history.size(), userId);
            return history;
            
        } catch (Exception e) {
            logger.error("Error loading chat history for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>(); // Trả về list rỗng nếu có lỗi
        }
    }
    
    /**
     * Xóa toàn bộ lịch sử chat của user
     * @param userId ID của user
     */
    public void clearChatHistory(Long userId) {
        try {
            chatMessageRepository.deleteByUser_UserId(userId);
            logger.info("Cleared chat history for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error clearing chat history for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Không thể xóa lịch sử chat: " + e.getMessage());
        }
    }
}

