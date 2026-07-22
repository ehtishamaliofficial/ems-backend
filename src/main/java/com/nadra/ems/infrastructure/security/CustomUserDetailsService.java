package com.nadra.ems.infrastructure.security;

import com.nadra.ems.domain.model.Role;
import com.nadra.ems.domain.model.User;
import com.nadra.ems.domain.port.out.RoleRepository;
import com.nadra.ems.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads user details from the database for Spring Security authentication.
 * Bridges the domain's {@link UserRepository} port to Spring Security's {@link UserDetailsService}.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        List<Role> roles = roleRepository.findRolesByUserId(user.getId());

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),                           // enabled
                true,                                       // accountNonExpired
                true,                                       // credentialsNonExpired
                !user.isAccountLocked(),                   // accountNonLocked
                authorities
        );
    }

    /**
     * Loads user details by userId — used for scoped tokens (2FA setup/verify)
     * which only carry the userId, not the username.
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user details for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", userId);
                    return new UsernameNotFoundException("User not found: id=" + userId);
                });

        List<Role> roles = roleRepository.findRolesByUserId(user.getId());

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                !user.isAccountLocked(),
                authorities
        );
    }
}
