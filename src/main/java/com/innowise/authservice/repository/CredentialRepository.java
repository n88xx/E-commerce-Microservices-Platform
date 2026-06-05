package com.innowise.authservice.repository;

import com.innowise.authservice.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByLogin(String login);

    Optional<Credential> findByUserId(Long userId);

    boolean existsByLogin(String login);
    boolean existsByUserId(Long userId);
}
