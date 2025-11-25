package com.example.financeapp.auth.service;

public class GoogleUserInfo {
    private String email;
    private String name;
    private String picture;

    public GoogleUserInfo() {
    }

    public GoogleUserInfo(String email, String name, String picture) {
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public static GoogleUserInfoBuilder builder() {
        return new GoogleUserInfoBuilder();
    }

    public static class GoogleUserInfoBuilder {
        private String email;
        private String name;
        private String picture;

        public GoogleUserInfoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public GoogleUserInfoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GoogleUserInfoBuilder picture(String picture) {
            this.picture = picture;
            return this;
        }

        public GoogleUserInfo build() {
            return new GoogleUserInfo(email, name, picture);
        }
    }
}

