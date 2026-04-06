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

import java.math.BigDecimal;

@SpringBootApplication
@EnableScheduling
public class MutuelleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MutuelleApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            AdministratorRepository administratorRepository,
            CashboxRepository cashboxRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                System.out.println("Début de l'initialisation des données...");
                // Initialiser les caisses si elles n'existent pas
                if (cashboxRepository.count() == 0) {
                    for (CashboxName name : CashboxName.values()) {
                        cashboxRepository.save(Cashbox.builder()
                                .name(name)
                                .balance(BigDecimal.ZERO)
                                .build());
                    }
                    System.out.println("Caisses initialisées.");
                }

                // Initialiser le super_admin si nécessaire
                if (userRepository.findByEmail("root").isEmpty()) {
                    System.out.println("Création du Super Admin...");
                    User user = User.builder()
                            .name("Admin")
                            .firstName("Root")
                            .email("root")
                            .tel("000000000")
                            .address("Système")
                            .type(RoleType.SUPER_ADMIN)
                            .password(passwordEncoder.encode("root"))
                            .build();

                    Administrator administrator = Administrator.builder()
                            .user(user)
                            .adminRole(AdminRole.PRESIDENT)
                            .username("root")
                            .active(true)
                            .build();
                    
                    administratorRepository.save(administrator);
                    System.out.println("Super Admin 'root' créé avec succès (password: root).");
                }
                System.out.println("Initialisation terminée avec succès.");
            } catch (Exception e) {
                System.err.println("ERREUR lors de l'initialisation : " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
