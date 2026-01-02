package com.example.financeapp.user.dto;

public class TwoFactorStatusResponse {
    private boolean enabled;
    private boolean hasSecret; // Đã có secret chưa (đã setup lần đầu chưa)

    public TwoFactorStatusResponse(boolean enabled, boolean hasSecret) {
        this.enabled = enabled;
        this.hasSecret = hasSecret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHasSecret() {
        return hasSecret;
    }

    public void setHasSecret(boolean hasSecret) {
        this.hasSecret = hasSecret;
    }
}

