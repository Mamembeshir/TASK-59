package com.instituteops.security;

import com.instituteops.security.domain.UserEntity;
import com.instituteops.security.repo.UserRepository;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class InstituteUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public InstituteUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new InstituteUserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getPasswordHash(),
            user.isActive(),
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()))
                .collect(Collectors.toSet())
        );
    }
}
