package com.mutuelle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private Long id;
    private String token;
    private String email;
    private String username;
    private String role;
    private String subRole;
}
