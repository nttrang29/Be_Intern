package com.example.financeapp.log.service;

import com.example.financeapp.log.entity.LoginLog;
import com.example.financeapp.log.model.LoginAuditEvent;
import com.example.financeapp.log.model.LoginLogStatus;
import com.example.financeapp.log.repository.LoginLogRepository;
import com.example.financeapp.log.util.UserAgentParser;
import com.example.financeapp.log.util.UserAgentParser.DeviceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class LoginLogService {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogService.class);

    private final LoginLogRepository loginLogRepository;
    private final ObjectMapper objectMapper;

    public LoginLogService(LoginLogRepository loginLogRepository, ObjectMapper objectMapper) {
        this.loginLogRepository = loginLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lưu 1 bản ghi login log mới với đầy đủ thông tin phụ.
     */
    public void recordEvent(LoginAuditEvent event) {
        if (event == null) {
            return;
        }

        LoginLog log = new LoginLog();
        log.setUserId(event.userId());
        log.setIpAddress(event.ipAddress());
        log.setUserAgent(event.userAgent());
        log.setLocation(event.location());
        log.setFailureReason(event.failureReason());
        log.setLoginTime(Instant.now());

        DeviceInfo deviceInfo = UserAgentParser.parse(event.userAgent());
        log.setBrowser(deviceInfo.browser());
        log.setOperatingSystem(deviceInfo.operatingSystem());
        log.setDeviceSummary(deviceInfo.summary());

        LoginLogStatus incomingStatus = event.status() != null
                ? event.status()
                : LoginLogStatus.SUCCESS;

        boolean suspicious = shouldFlagSuspicious(event.userId(), incomingStatus, event.ipAddress());
        log.setSuspicious(suspicious);
        log.setStatus(suspicious ? LoginLogStatus.SUSPICIOUS : incomingStatus);

        log.setMetadata(serializeMetadata(buildMetadata(event)));

        loginLogRepository.save(log);
    }

    public List<LoginLog> getLogsByUser(Long userId) {
        return loginLogRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    public Page<LoginLog> getLogsByUser(Long userId, Pageable pageable) {
        Pageable safePageable = pageable != null
                ? pageable
                : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "loginTime"));
        return loginLogRepository.findByUserId(userId, safePageable);
    }

    public List<LoginLog> getRecentLogsByUser(Long userId, int limit) {
        int safeLimit = limit <= 0 ? 10 : limit;
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "loginTime"));
        return loginLogRepository.findByUserId(userId, pageable).getContent();
    }

    public Page<LoginLog> searchLogs(Long userId, String ipAddress, LoginLogStatus status, Pageable pageable) {
        Specification<LoginLog> specification = Specification.where(null);

        if (userId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }

        if (StringUtils.hasText(ipAddress)) {
            String keyword = "%" + ipAddress.trim().toLowerCase(Locale.ENGLISH) + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("ipAddress")), keyword));
        }

        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return loginLogRepository.findAll(specification, pageable);
    }

    private boolean shouldFlagSuspicious(Long userId, LoginLogStatus incomingStatus, String ipAddress) {
        if (userId == null || !StringUtils.hasText(ipAddress)) {
            return incomingStatus == LoginLogStatus.SUSPICIOUS;
        }

        if (incomingStatus == LoginLogStatus.SUSPICIOUS) {
            return true;
        }

        Optional<LoginLog> lastLog = loginLogRepository.findTopByUserIdOrderByLoginTimeDesc(userId);
        return lastLog
                .map(log -> StringUtils.hasText(log.getIpAddress())
                        && !log.getIpAddress().equalsIgnoreCase(ipAddress))
                .orElse(false);
    }

    private Map<String, Object> buildMetadata(LoginAuditEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        if (StringUtils.hasText(event.userAgent())) {
            metadata.put("rawUserAgent", event.userAgent());
        }
        if (StringUtils.hasText(event.email())) {
            metadata.put("email", event.email());
        }
        if (StringUtils.hasText(event.location())) {
            metadata.put("location", event.location());
        }
        if (StringUtils.hasText(event.failureReason())) {
            metadata.put("failureReason", event.failureReason());
        }
        if (!CollectionUtils.isEmpty(event.metadata())) {
            metadata.putAll(event.metadata());
        }
        return metadata;
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.warn("Không thể serialize metadata login log", e);
            return metadata.toString();
        }
    }
}

