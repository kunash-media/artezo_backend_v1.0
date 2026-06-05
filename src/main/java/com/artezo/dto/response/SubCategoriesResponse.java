package com.artezo.dto.response;

import org.apache.poi.ss.formula.functions.T;

import java.util.List;

public class SubCategoriesResponse {

    private boolean success;
    private String message;
    private List<String>  data;

    public SubCategoriesResponse(boolean success, String message, List<String>  data) {
        this.success = success;
        this.message = message;
        this.data = data;
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

    public List<String>  getData() {
        return data;
    }

    public void setData(List<String>  data) {
        this.data = data;
    }
}
