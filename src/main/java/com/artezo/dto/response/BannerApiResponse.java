package com.artezo.dto.response;

public class BannerApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // Default constructor
    public BannerApiResponse() {}

    // Parameterized constructor
    public BannerApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Static factory methods
    public static <T> BannerApiResponse<T> success(T data) {
        return new BannerApiResponse<>(true, "Success", data);
    }

    public static <T> BannerApiResponse<T> success(String message, T data) {
        return new BannerApiResponse<>(true, message, data);
    }

    public static <T> BannerApiResponse<T> error(String message) {
        return new BannerApiResponse<>(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}