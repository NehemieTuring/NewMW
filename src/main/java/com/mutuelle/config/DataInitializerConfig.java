package com.mutuelle.config;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.User;
import com.mutuelle.enums.AdminRole;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.enums.RoleType;
import com.mutuelle.repository.AdministratorRepository;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.UserRepository;
import com.mutuelle.service.SessionScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializerConfig {

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            AdministratorRepository administratorRepository,
            CashboxRepository cashboxRepository,
            PasswordEncoder passwordEncoder,
            SessionScheduler sessionScheduler) {
        return args -> {
            try {
                System.out.println("Début de l'initialisation des données...");
                
                // Clôturer les sessions expirées dès le démarrage
                sessionScheduler.autoCloseExpiredSessions();
                
                // Initialiser les caisses...
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
                User rootUser = userRepository.findByEmail("root").orElse(null);
                if (rootUser == null) {
                    System.out.println("Création du Super Admin...");
                    rootUser = User.builder()
                            .name("ADMINISTRATEUR")
                            .firstName("SUPER")
                            .email("root")
                            .tel("000000000")
                            .address("Système")
                            .type(RoleType.SUPER_ADMIN)
                            .password(passwordEncoder.encode("root"))
                            .build();

                    Administrator administrator = Administrator.builder()
                            .user(rootUser)
                            .adminRole(AdminRole.PRESIDENT)
                            .username("root")
                            .active(true)
                            .build();
                    
                    administratorRepository.save(administrator);
                    System.out.println("Super Admin 'root' créé avec succès.");
                } else {
                    // Mettre à jour si les infos sont vides
                    boolean updated = false;
                    if (rootUser.getName() == null || rootUser.getName().isEmpty()) {
                        rootUser.setName("ADMINISTRATEUR");
                        updated = true;
                    }
                    if (rootUser.getFirstName() == null || rootUser.getFirstName().isEmpty()) {
                        rootUser.setFirstName("SUPER");
                        updated = true;
                    }
                    if (updated) {
                        userRepository.save(rootUser);
                        System.out.println("Infos de base du Super Admin mises à jour.");
                    }
                }
                System.out.println("Initialisation terminée avec succès.");
            } catch (Exception e) {
                System.err.println("ERREUR lors de l'initialisation : " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    @Bean
    public CommandLineRunner cleanupDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                System.out.println("🚀 Tentative de nettoyage de la base de données...");
                // On tente de supprimer la colonne parasite 'exercise_year' si elle existe
                jdbcTemplate.execute("ALTER TABLE exercise DROP COLUMN exercise_year");
                System.out.println("✅ Colonne 'exercise_year' supprimée avec succès.");
            } catch (Exception e) {
                // Si elle n'existe pas, c'est normal, on ne fait rien
                System.out.println("ℹ️ Nettoyage : Colonne 'exercise_year' déjà absente.");
            }
            
            try {
                // On vérifie aussi 'name' qui pourrait avoir été créé par erreur
                jdbcTemplate.execute("ALTER TABLE exercise DROP COLUMN name");
                System.out.println("✅ Colonne 'name' supprimée avec succès.");
            } catch (Exception e) {
                // Ignorer
            }
        };
    }
}
