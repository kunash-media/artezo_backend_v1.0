package com.artezo.security;

import com.artezo.entity.UserEntity;
import com.artezo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userDetailsServiceImpl")   // named bean — avoids conflict with AdminUserDetailsService
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Accepts email or phone as the identifier.
     * Spring Security calls this with whatever string was passed
     * as the "username" in UsernamePasswordAuthenticationToken.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        logger.debug("Loading user by identifier: {}", identifier);

        UserEntity user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> {
                    logger.warn("No user found for identifier: {}", identifier);
                    return new UsernameNotFoundException("User not found: " + identifier);
                });

        logger.debug("User loaded successfully: userId={}", user.getUserId());
        return new UserPrincipal(user);
    }
}