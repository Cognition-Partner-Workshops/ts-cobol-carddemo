package com.aws.carddemo.authorization;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aws.carddemo.user.AppUser;
import com.aws.carddemo.user.AppUserRepository;

@Service
public class CardDemoUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CardDemoUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        String role = "A".equals(appUser.getUserType()) ? "ROLE_ADMIN" : "ROLE_USER";

        return new User(
                appUser.getUserId(),
                appUser.getPassword(),
                appUser.getEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
