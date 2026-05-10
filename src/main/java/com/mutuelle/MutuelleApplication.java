package com.mutuelle;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.User;
import com.mutuelle.enums.AdminRole;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.RoleType;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

@SpringBootApplication
@EnableScheduling
public class MutuelleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MutuelleApplication.class, args);
    }
}
