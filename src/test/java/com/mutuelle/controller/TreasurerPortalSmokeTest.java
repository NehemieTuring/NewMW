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
public class TreasurerPortalSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AdminService adminService;
    @MockBean private DashboardService dashboardService;
    @MockBean private ExpenseService expenseService;
    @MockBean private MemberService memberService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(2L).email("treasurer@test.com").type(RoleType.ADMIN).build();
        Administrator admin = Administrator.builder().id(2L).user(user).build();
        when(adminService.getAdminByEmail(anyString())).thenReturn(admin);
    }

    @Test
    @DisplayName("Treasurer - Smoke Test - Finances")
    @WithMockUser(authorities = "ROLE_TRESORIER")
    void testTreasurerFinance() throws Exception {
        mockMvc.perform(get("/treasurer/reports/daily")).andExpect(status().isOk());
        mockMvc.perform(get("/treasurer/expenses")).andExpect(status().isOk());
        mockMvc.perform(get("/treasurer/dashboard/cashboxes")).andExpect(status().isOk());
    }
}
