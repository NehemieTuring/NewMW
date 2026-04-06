package com.mutuelle.security;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Member;
import com.mutuelle.entity.User;
import com.mutuelle.enums.RoleType;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.MemberRepository;
import com.mutuelle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdministratorRepository administratorRepository;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User foundUser = null;
        Object profile = null;

        // 1. Try finding by email
        foundUser = userRepository.findByEmail(identifier).orElse(null);
        if (foundUser != null) {
            // Find profile associated with the user
            if (foundUser.getType() == RoleType.ADMIN || foundUser.getType() == RoleType.SUPER_ADMIN) {
                profile = administratorRepository.findByUser(foundUser).orElse(null);
            } else if (foundUser.getType() == RoleType.MEMBER) {
                profile = memberRepository.findByUser(foundUser).orElse(null);
            }
        }

        // 2. Try finding by Member username if not found by email
        if (foundUser == null) {
            Member member = memberRepository.findByUsername(identifier).orElse(null);
            if (member != null) {
                foundUser = member.getUser();
                profile = member;
            }
        }

        // 3. Try finding by Administrator username if not found by email or member username
        if (foundUser == null) {
            Administrator admin = administratorRepository.findByUsername(identifier).orElse(null);
            if (admin != null) {
                foundUser = admin.getUser();
                profile = admin;
            }
        }

        if (foundUser == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé : " + identifier);
        }

        final User user = foundUser;
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getType().name()));

        // Add additional roles for ADMIN or SUPER_ADMIN
        if (profile instanceof Administrator) {
            String adminRole = ((Administrator) profile).getAdminRole().name();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + adminRole));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}

