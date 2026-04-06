package com.mutuelle.service;

import com.mutuelle.dto.request.RegisterMemberRequest;
import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Member;
import com.mutuelle.entity.User;
import com.mutuelle.enums.RoleType;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.MemberRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AdministratorRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member register(RegisterMemberRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already in use");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank() && memberRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Username already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .firstName(request.getFirstName())
                .email(request.getEmail())
                .tel(request.getTel())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .type(RoleType.MEMBER)
                .build();

        User savedUser = userRepository.save(user);

        Administrator administrator = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new BusinessException("Administrator not found"));

        Member member = Member.builder()
                .user(savedUser)
                .administrator(administrator)
                .registrationNumber(request.getRegistrationNumber())
                .username(request.getUsername())
                .inscriptionDate(request.getInscriptionDate())
                .active(true)
                .build();

        return memberRepository.save(member);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Member getMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException("Member not found"));
    }

    public Member getMemberByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
        return memberRepository.findByUser(user).orElseThrow(() -> new BusinessException("Member profile not found"));
    }

    @Transactional
    public Member updateMember(Long id, RegisterMemberRequest request) {
        Member member = getMemberById(id);
        User user = member.getUser();
        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setTel(request.getTel());
        user.setAddress(request.getAddress());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        member.setUsername(request.getUsername());
        return memberRepository.save(member);
    }

    @Transactional
    public void deactivateMember(Long id) {
        Member member = getMemberById(id);
        member.setActive(false);
        memberRepository.save(member);
    }

    public String getMemberStatus(Long id) {
        Member member = getMemberById(id);
        if (!member.isActive()) return "INACTIF";
        // Simple logic for illustration:
        return "EN_REGLE";
    }

    public java.util.Map<String, Object> getMemberDebts(Long id) {
        Member member = getMemberById(id);
        java.util.Map<String, Object> debts = new java.util.HashMap<>();
        debts.put("solidarity", 0); // To integrate with SolidarityService later
        debts.put("borrowing", 0);
        return debts;
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = getMemberById(id);
        User user = member.getUser();
        memberRepository.delete(member);
        userRepository.delete(user);
    }
}
