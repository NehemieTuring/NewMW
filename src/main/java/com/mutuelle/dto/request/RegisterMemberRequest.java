package com.mutuelle.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterMemberRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String firstName;

    @NotBlank
    private String email;

    private String tel;
    private String address;

    @NotBlank
    private String password;

    private String registrationNumber;

    private String username;

    private LocalDate inscriptionDate = LocalDate.now();
    private Long adminId;
}
