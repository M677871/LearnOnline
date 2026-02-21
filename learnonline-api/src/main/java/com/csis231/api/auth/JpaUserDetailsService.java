package com.csis231.api.auth;

import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link UserDetailsService} implementation that loads users from the JPA
 * {@link UserRepository}.
 *
 * <p>It adapts the application's {@link User} entity to Spring Security's
 * {@link UserDetails} contract.</p>
 */

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Creates a new {@code JpaUserDetailsService} using the given repository.
     *
     * @param userRepository repository used to look up users by username
     */

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by username and maps it to a Spring Security
     * {@link UserDetails} instance.
     *
     * <p>The user's role is converted to a single
     * {@link SimpleGrantedAuthority} with the {@code ROLE_} prefix and the
     * active flag is mapped to the {@link UserDetails#isEnabled()} property.</p>
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated {@link UserDetails} instance
     * @throws UsernameNotFoundException if no user could be found
     */

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String roleName = "ROLE_" + u.getRole().name(); // must be prefixed with ROLE_
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(roleName)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!u.getIsActive())
                .build();
    }
}
