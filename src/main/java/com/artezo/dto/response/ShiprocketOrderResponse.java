package com.artezo.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiprocketOrderResponse {

    @JsonProperty("order_id")
    private Long orderId;               // SR numeric order id — store in shiprocketOrderId

    @JsonProperty("shipment_id")
    private Long shipmentId;            // SR shipment id — store in shiprocketShipmentId

    @JsonProperty("status")
    private String status;              // "NEW"

    @JsonProperty("status_code")
    private Integer statusCode;         // 1 = success

    @JsonProperty("awb_code")
    private String awbCode;             // may be empty initially — filled via webhook

    @JsonProperty("courier_company_id")
    private Integer courierCompanyId;

    @JsonProperty("courier_name")
    private String courierName;

    // getters and setters ...


    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getAwbCode() {
        return awbCode;
    }

    public void setAwbCode(String awbCode) {
        this.awbCode = awbCode;
    }

    public Integer getCourierCompanyId() {
        return courierCompanyId;
    }

    public void setCourierCompanyId(Integer courierCompanyId) {
        this.courierCompanyId = courierCompanyId;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }
}