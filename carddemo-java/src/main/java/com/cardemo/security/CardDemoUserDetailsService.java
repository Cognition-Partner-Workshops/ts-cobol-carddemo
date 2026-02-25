package com.cardemo.security;

import com.cardemo.entity.CardDemoUser;
import com.cardemo.repository.CardDemoUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security UserDetailsService implementation.
 * Migrated from USRSEC VSAM file read in COSGN00C (READ-USER-SEC-FILE paragraph).
 */
@Service
public class CardDemoUserDetailsService implements UserDetailsService {

    private final CardDemoUserRepository userRepository;

    public CardDemoUserDetailsService(CardDemoUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CardDemoUser user = userRepository.findById(username.toUpperCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(user.getUsrId(), user.getUsrPwd(), authorities);
    }
}
