package com.mutuelle.repository;

import com.mutuelle.entity.Member;
import com.mutuelle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByRegistrationNumber(String registrationNumber);
    Optional<Member> findByUser(User user);
}
