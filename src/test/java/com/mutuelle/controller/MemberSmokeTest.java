package com.mutuelle.controller;

import com.mutuelle.entity.Member;
import com.mutuelle.entity.User;
import com.mutuelle.enums.RoleType;
import com.mutuelle.service.*;
import com.mutuelle.repository.UserRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class MemberSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MemberService memberService;
    @MockBean private SavingService savingService;
    @MockBean private BorrowingService borrowingService;
    @MockBean private HelpService helpService;
    @MockBean private ChatService chatService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private SessionService sessionService;
    @MockBean private ExerciseService exerciseService;
    @MockBean private AuthService authService;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email("user@test.com").type(RoleType.MEMBER).build();
        Member member = Member.builder().id(1L).user(user).username("testuser").build();
        
        when(memberService.getMemberByEmail(anyString())).thenReturn(member);
        when(memberService.getMemberById(anyLong())).thenReturn(member);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Member - Smoke Test - Profil et Finances")
    @WithMockUser(authorities = "ROLE_MEMBER")
    void testMemberFinancialEndpoints() throws Exception {
        mockMvc.perform(get("/member/profile")).andExpect(status().isOk());
        mockMvc.perform(get("/member/status")).andExpect(status().isOk());
        mockMvc.perform(get("/member/savings/balance")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Member - Smoke Test - Communication")
    @WithMockUser(authorities = "ROLE_MEMBER")
    void testMemberChatEndpoints() throws Exception {
        mockMvc.perform(get("/member/chat/conversations")).andExpect(status().isOk());
        mockMvc.perform(get("/member/members")).andExpect(status().isOk());
    }
}
