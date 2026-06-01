package com.innowise.authservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ValidateResponse {

    private Long userId;
    private String role;
}