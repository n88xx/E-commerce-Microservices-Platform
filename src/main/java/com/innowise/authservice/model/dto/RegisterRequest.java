package com.innowise.authservice.model.dto;

import com.innowise.authservice.model.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String login;

    @NotBlank
    @Size
    private String password;

    @NotNull
    private Role role;

    @NotNull
    private Long userId;

}
