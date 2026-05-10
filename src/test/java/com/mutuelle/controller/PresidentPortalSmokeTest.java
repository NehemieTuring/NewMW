package com.mutuelle.controller;

import com.mutuelle.entity.*;
import com.mutuelle.enums.*;
import com.mutuelle.service.*;
import com.mutuelle.config.JwtTokenProvider;
import com.mutuelle.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class PresidentPortalSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AdminService adminService;
    @MockBean private MemberService memberService;
    @MockBean private DashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(3L).email("president@test.com").type(RoleType.ADMIN).build();
        Administrator admin = Administrator.builder().id(3L).user(user).build();
        when(adminService.getAdminByEmail(anyString())).thenReturn(admin);
    }

    @Test
    @DisplayName("President - Smoke Test - Supervision")
    @WithMockUser(authorities = "ROLE_PRESIDENT")
    void testPresidentSupervision() throws Exception {
        mockMvc.perform(get("/president/members")).andExpect(status().isOk());
        mockMvc.perform(get("/president/dashboard/transactions")).andExpect(status().isOk());
    }
}
