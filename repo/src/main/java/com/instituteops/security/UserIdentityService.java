package com.instituteops.security;

import com.instituteops.security.repo.UserRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserIdentityService {

    private final UserRepository userRepository;

    public UserIdentityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Long> resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof InstituteUserPrincipal instituteUserPrincipal) {
            return Optional.ofNullable(instituteUserPrincipal.getUserId());
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || username.startsWith("api:")) {
            return Optional.empty();
        }
        return userRepository.findIdByUsername(username);
    }
}
