package com.artezo.security;

import com.artezo.entity.AdminEntity;
import com.artezo.repository.AdminRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")   // explicit name for @Qualifier
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminEntity admin = adminRepository.findByAdminMobileNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Admin not found with mobile: " + username));
        return new AdminDetails(admin);
    }
}