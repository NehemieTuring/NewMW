package com.mutuelle.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @JsonProperty("identifier")
    @JsonAlias({"email", "username"})
    @NotBlank(message = "L'identifiant est obligatoire")
    private String identifier;

    @JsonProperty("email")
    private String email;

    @JsonProperty("username")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
