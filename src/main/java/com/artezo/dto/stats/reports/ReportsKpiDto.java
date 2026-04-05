package com.artezo.dto.stats.reports;

public class ReportsKpiDto {

    private String  label;          // "Total Revenue", "Total Orders", "Avg. Order Value"
    private String  displayValue;   // "₹1,28,430.00" or "1,250" — fully formatted
    private Double  rawValue;       // raw double for client-side arithmetic if needed
    private Double  deltaPercent;   // +12.5 or -3.2  (positive = up, negative = down)
    private boolean deltaUp;        // true if current >= previous
    private String  icon;           // FontAwesome class hint e.g. "fa-indian-rupee-sign"

    public ReportsKpiDto() {}

    public ReportsKpiDto(String label, String displayValue, Double rawValue,
                         Double deltaPercent, boolean deltaUp, String icon) {
        this.label        = label;
        this.displayValue = displayValue;
        this.rawValue     = rawValue;
        this.deltaPercent = deltaPercent;
        this.deltaUp      = deltaUp;
        this.icon         = icon;
    }

    public String  getLabel()        { return label; }
    public String  getDisplayValue() { return displayValue; }
    public Double  getRawValue()     { return rawValue; }
    public Double  getDeltaPercent() { return deltaPercent; }
    public boolean isDeltaUp()       { return deltaUp; }
    public String  getIcon()         { return icon; }

    public void setLabel(String label)               { this.label = label; }
    public void setDisplayValue(String displayValue) { this.displayValue = displayValue; }
    public void setRawValue(Double rawValue)         { this.rawValue = rawValue; }
    public void setDeltaPercent(Double deltaPercent) { this.deltaPercent = deltaPercent; }
    public void setDeltaUp(boolean deltaUp)          { this.deltaUp = deltaUp; }
    public void setIcon(String icon)                 { this.icon = icon; }
}