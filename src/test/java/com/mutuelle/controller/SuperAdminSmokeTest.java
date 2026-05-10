package com.mutuelle.controller;

import com.mutuelle.service.AdminService;
import com.mutuelle.service.DashboardService;
import com.mutuelle.config.JwtTokenProvider;
import com.mutuelle.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class SuperAdminSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AdminService adminService;
    @MockBean private DashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("SuperAdmin - Smoke Test - Gestion Critique")
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testSuperAdminAccess() throws Exception {
        mockMvc.perform(get("/admin/super/admins")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/super/dashboard")).andExpect(status().isOk());
    }
}
