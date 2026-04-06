package com.mutuelle.repository;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
    Optional<Administrator> findByUsername(String username);
    Optional<Administrator> findByUser(User user);
}
