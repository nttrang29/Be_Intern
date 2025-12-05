package com.example.financeapp.log.util;

import java.util.Locale;

/**
 * Helper đơn giản để trích xuất thông tin trình duyệt / hệ điều hành từ user-agent.
 */
public final class UserAgentParser {

    private static final String UNKNOWN = "Unknown";

    private UserAgentParser() {
    }

    public static DeviceInfo parse(String userAgent) {
        if (userAgent == null) {
            return new DeviceInfo(UNKNOWN, UNKNOWN, UNKNOWN);
        }

        String ua = userAgent.toLowerCase(Locale.ENGLISH);
        String browser = detectBrowser(ua);
        String os = detectOs(ua);
        String summary = buildSummary(browser, os);

        return new DeviceInfo(browser, os, summary);
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("edg")) {
            return "Edge";
        }
        if (ua.contains("chrome")) {
            return "Chrome";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        }
        if (ua.contains("opera") || ua.contains("opr")) {
            return "Opera";
        }
        if (ua.contains("msie") || ua.contains("trident")) {
            return "Internet Explorer";
        }
        return UNKNOWN;
    }

    private static String detectOs(String ua) {
        if (ua.contains("windows nt")) {
            return "Windows";
        }
        if (ua.contains("mac os x")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return UNKNOWN;
    }

    private static String buildSummary(String browser, String os) {
        if (UNKNOWN.equals(browser) && UNKNOWN.equals(os)) {
            return UNKNOWN;
        }
        if (UNKNOWN.equals(os)) {
            return browser;
        }
        if (UNKNOWN.equals(browser)) {
            return os;
        }
        return browser + " • " + os;
    }

    public record DeviceInfo(String browser, String operatingSystem, String summary) {
    }
}
