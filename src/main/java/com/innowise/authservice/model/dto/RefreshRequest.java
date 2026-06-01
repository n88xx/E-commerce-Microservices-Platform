package com.innowise.authservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;
}