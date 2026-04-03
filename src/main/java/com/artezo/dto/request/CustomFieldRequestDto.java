package com.artezo.dto.request;

import java.util.List;

public class CustomFieldRequestDto {

    private Integer fieldId;
    private String fieldName;
    private String fieldInputType;
    private String note;
    private List<String> dropdownOptions;   // only sent when fieldInputType = "dropdown"

    // === Getters & Setters ===
    public Integer getFieldId() { return fieldId; }
    public void setFieldId(Integer fieldId) { this.fieldId = fieldId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldInputType() { return fieldInputType; }
    public void setFieldInputType(String fieldInputType) { this.fieldInputType = fieldInputType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<String> getDropdownOptions() { return dropdownOptions; }
    public void setDropdownOptions(List<String> dropdownOptions) { this.dropdownOptions = dropdownOptions; }
}