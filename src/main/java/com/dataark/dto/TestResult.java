package com.dataark.dto;

import java.time.LocalDateTime;

public class TestResult {
    private boolean success;
    private String message;
    private String detail;
    private LocalDateTime testedAt = LocalDateTime.now();

    public static TestResult ok(String message, String detail) {
        TestResult result = new TestResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setDetail(detail);
        return result;
    }

    public static TestResult fail(String message, String detail) {
        TestResult result = new TestResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setDetail(detail);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(LocalDateTime testedAt) {
        this.testedAt = testedAt;
    }
}
