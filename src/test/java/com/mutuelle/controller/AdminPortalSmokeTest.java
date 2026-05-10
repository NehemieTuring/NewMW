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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class AdminPortalSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MemberService memberService;
    @MockBean private AdminService adminService;
    @MockBean private SolidarityService solidarityService;
    @MockBean private SavingService savingService;
    @MockBean private BorrowingService borrowingService;
    @MockBean private HelpService helpService;
    @MockBean private ExerciseService exerciseService;
    @MockBean private SessionService sessionService;
    @MockBean private DashboardService dashboardService;
    @MockBean private ChatService chatService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email("admin@test.com").type(RoleType.ADMIN).build();
        Administrator admin = Administrator.builder().id(1L).user(user).build();
        when(adminService.getAdminByEmail(anyString())).thenReturn(admin);
    }

    @Test
    @DisplayName("Admin - Smoke Test - Gestion Générale")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminManagement() throws Exception {
        mockMvc.perform(get("/admin/members")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/exercises")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/transactions")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin - Smoke Test - Profil et Chat")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminProfile() throws Exception {
        mockMvc.perform(get("/admin/profile")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/chat/conversations")).andExpect(status().isOk());
    }
}
