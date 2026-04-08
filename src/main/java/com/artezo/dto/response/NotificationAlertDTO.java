package com.artezo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationAlertDTO {

    private List<LowStockAlert> lowStock;
    private List<NewUserAlert> recentUsers;
    private List<ContactEnquiryAlert> contactEnquiries;
    private int totalUnread;

    // ── Constructors, getters, setters ──────────────────────────

    public NotificationAlertDTO() {}

    public NotificationAlertDTO(List<LowStockAlert> lowStock,
                                List<NewUserAlert> recentUsers,
                                List<ContactEnquiryAlert> contactEnquiries) {
        this.lowStock = lowStock;
        this.recentUsers = recentUsers;
        this.contactEnquiries = contactEnquiries;
        // Only count items not yet visited
        this.totalUnread = (int) (
                lowStock.stream().filter(i -> !i.isVisited()).count() +
                        recentUsers.stream().filter(u -> !u.isVisited()).count() +
                        contactEnquiries.stream().filter(c -> !c.isVisited()).count()
        );
    }

    public List<LowStockAlert> getLowStock() { return lowStock; }
    public List<NewUserAlert> getRecentUsers() { return recentUsers; }
    public List<ContactEnquiryAlert> getContactEnquiries() { return contactEnquiries; }
    public int getTotalUnread() { return totalUnread; }

    // ── Nested DTOs ──────────────────────────────────────────────

    public static class LowStockAlert {
        private String sku;
        private String productName;
        private Integer availableStock;
        private Integer lowStockThreshold;
        private String fingerprint;
        private boolean visited;

        public LowStockAlert(String sku, String productName, Integer availableStock,
                             Integer threshold, boolean visited) {
            this.sku = sku;
            this.productName = productName;
            this.availableStock = availableStock;
            this.lowStockThreshold = threshold;
            this.fingerprint = "stock-" + sku;
            this.visited = visited;
        }

        public String getSku()                { return sku; }
        public String getProductName()        { return productName; }
        public Integer getAvailableStock()    { return availableStock; }
        public Integer getLowStockThreshold() { return lowStockThreshold; }
        public String getFingerprint()        { return fingerprint; }
        public boolean isVisited()            { return visited; }
    }

    public static class NewUserAlert {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private LocalDateTime createdAt;
        private String fingerprint;
        private boolean visited;

        public NewUserAlert(Long userId, String firstName, String lastName,
                            String email, LocalDateTime createdAt, boolean visited) {
            this.userId = userId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.createdAt = createdAt;
            this.fingerprint = "user-" + userId;
            this.visited = visited;
        }

        public Long getUserId()             { return userId; }
        public String getFirstName()        { return firstName; }
        public String getLastName()         { return lastName; }
        public String getEmail()            { return email; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getFingerprint()      { return fingerprint; }
        public boolean isVisited()          { return visited; }
    }

    public static class ContactEnquiryAlert {
        private Long formId;
        private String name;
        private String email;
        private String messagePreview;
        private LocalDateTime createdAt;
        private String fingerprint;
        private boolean visited;

        public ContactEnquiryAlert(Long formId, String name, String email,
                                   String message, LocalDateTime createdAt, boolean visited) {
            this.formId = formId;
            this.name = name;
            this.email = email;
            this.messagePreview = message != null && message.length() > 60
                    ? message.substring(0, 60) + "…" : message;
            this.createdAt = createdAt;
            this.fingerprint = "contact-" + formId;
            this.visited = visited;
        }

        public Long getFormId()             { return formId; }
        public String getName()             { return name; }
        public String getEmail()            { return email; }
        public String getMessagePreview()   { return messagePreview; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getFingerprint()      { return fingerprint; }
        public boolean isVisited()          { return visited; }
    }
}