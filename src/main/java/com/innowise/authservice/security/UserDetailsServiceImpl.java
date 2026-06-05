package com.innowise.authservice.security;

import com.innowise.authservice.entity.Credential;
import com.innowise.authservice.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;


import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CredentialRepository credentialRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Credential credential = credentialRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(credential.getLogin())
                .password(credential.getPasswordHash())
                .authorities("ROLE_" + credential.getRole().name())
                .disabled(!credential.getActive())
                .build();
    }
}
