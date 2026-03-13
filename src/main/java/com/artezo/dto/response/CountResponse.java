package com.artezo.dto.response;


import lombok.Builder;


@Builder
public class CountResponse {
    private Long userId;
    private int count;

//    public CountResponse(){}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}