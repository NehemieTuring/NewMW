package com.mutuelle.controller;

import com.mutuelle.entity.Member;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberPortalController.class)
public class MemberPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean
    private SavingService savingService;

    @MockBean
    private BorrowingService borrowingService;

    @MockBean
    private HelpService helpService;

    @MockBean
    private ChatService chatService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private ExerciseService exerciseService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Test récupération du profil membre")
    @WithMockUser(username = "test@example.com", authorities = "ROLE_MEMBER")
    void shouldGetProfile() throws Exception {
        User user = User.builder().id(1L).email("test@example.com").type(RoleType.MEMBER).build();
        Member member = Member.builder().id(1L).user(user).username("testuser").build();

        when(memberService.getMemberByEmail("test@example.com")).thenReturn(member);

        mockMvc.perform(get("/member/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Test mise à jour de l'avatar")
    @WithMockUser(username = "test@example.com", authorities = "ROLE_MEMBER")
    void shouldUpdateAvatar() throws Exception {
        User user = User.builder().id(1L).email("test@example.com").type(RoleType.MEMBER).build();
        Member member = Member.builder().id(1L).user(user).build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        when(memberService.getMemberByEmail("test@example.com")).thenReturn(member);
        when(fileStorageService.storeFile(any())).thenReturn("unique-file-name.jpg");
        when(memberService.updateAvatar(anyLong(), anyString())).thenReturn(member);

        mockMvc.perform(multipart("/member/profile/avatar")
                        .file(file)
                        .with(csrf())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
    }
}
