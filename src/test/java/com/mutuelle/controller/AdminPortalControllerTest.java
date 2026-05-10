package com.mutuelle.controller;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.User;
import com.mutuelle.enums.RoleType;
import com.mutuelle.service.*;
import com.mutuelle.repository.UserRepository;
import com.mutuelle.config.JwtTokenProvider;
import com.mutuelle.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPortalController.class)
public class AdminPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean
    private SolidarityService solidarityService;

    @MockBean
    private SavingService savingService;

    @MockBean
    private BorrowingService borrowingService;

    @MockBean
    private HelpService helpService;

    @MockBean
    private ExerciseService exerciseService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RefuelingService refuelingService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private ChatService chatService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AgapeService agapeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Test mise à jour de l'avatar admin")
    @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
    void shouldUpdateAdminAvatar() throws Exception {
        User user = User.builder().id(1L).email("admin@example.com").type(RoleType.ADMIN).build();
        Administrator admin = Administrator.builder().id(1L).user(user).build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "admin-avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        when(adminService.getAdminByEmail("admin@example.com")).thenReturn(admin);
        when(fileStorageService.storeFile(any())).thenReturn("admin-avatar-name.jpg");
        when(adminService.updateAvatar(anyLong(), anyString())).thenReturn(admin);

        mockMvc.perform(multipart("/admin/profile/avatar")
                        .file(file)
                        .with(csrf())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
    }
}
