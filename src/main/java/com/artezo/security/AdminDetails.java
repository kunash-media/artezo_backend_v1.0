package com.artezo.security;

import com.artezo.entity.AdminEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper that adapts AdminEntity to Spring Security's UserDetails interface.
 */
public class AdminDetails implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(AdminDetails.class);

    private static final String DEFAULT_ROLE = "ADMIN";  // fallback if role is missing

    private final AdminEntity admin;

    public AdminDetails(AdminEntity admin) {
        if (admin == null) {
            throw new IllegalArgumentException("AdminEntity cannot be null");
        }
        this.admin = admin;
    }

    /**
     * Returns the authorities/roles granted to the admin.
     * Prefixes with "ROLE_" as required by Spring Security's hasRole() checks.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = admin.getAdminRole();

        // ADD THIS LINE temporarily
        System.out.println(">>> ROLE FROM DB: [" + role + "] for user: " + admin.getAdminMobileNumber());

        if (role == null || role.trim().isEmpty()) {
            role = DEFAULT_ROLE;
        }

        String authority = "ROLE_" + role.trim().toUpperCase();

        // ADD THIS LINE temporarily
        System.out.println(">>> GRANTED AUTHORITY: [" + authority + "]");

        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return admin.getAdminPassword();
    }

    /**
     * Returns the identifier used for login (mobile number).
     */
    @Override
    public String getUsername() {
        return admin.getAdminMobileNumber();
    }

    // ── CHANGED: all status methods now read from AdminEntity fields ──
    // Make sure your AdminEntity has these boolean fields.
    // If a field doesn't exist yet, add it with a default value of true in your entity/DB.

    @Override
    public boolean isAccountNonExpired() {
        // Return true if AdminEntity has no account expiry tracking yet
        // Future: return admin.getIsAccountNonExpired() != null && admin.getIsAccountNonExpired();
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // CHANGED: reads from entity — lets you lock an admin without deleting them
        // Requires a boolean field `adminIsLocked` (or similar) in AdminEntity
        // Default to NOT locked (true = not locked) if field is null
        Boolean isLocked = admin.getAdminIsLocked();
        if (isLocked == null) return true;      // treat null as not locked
        boolean nonLocked = !isLocked;
        if (!nonLocked) {
            logger.warn("Admin {} is locked", admin.getAdminMobileNumber());
        }
        return nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // CHANGED: reads from entity — disabled admins cannot log in
        // Requires a boolean field `adminIsActive` in AdminEntity
        Boolean isActive = admin.getAdminIsActive();
        if (isActive == null) return true;      // treat null as active
        if (!isActive) {
            logger.warn("Admin {} is disabled (isActive = false)", admin.getAdminMobileNumber());
        }
        return isActive;
    }

    // ─────────────────────────────────────────────────────────────
    //  Convenience getters (unchanged)
    // ─────────────────────────────────────────────────────────────

    public AdminEntity getAdminEntity() {
        return admin;
    }

    public String getAdminId() {
        return admin.getAdminId();
    }

    public String getMobileNumber() {
        return admin.getAdminMobileNumber();
    }

    public String getRole() {
        return admin.getAdminRole();
    }
}