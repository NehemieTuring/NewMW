# Mutuelle Web Backend

Backend pour la gestion d'une mutuelle web avec Spring Boot 3, JWT, et MySQL.

## Technologies
- Java 17
- Spring Boot 3.2.x
- Spring Security + JWT
- Hibernate / JPA
- MySQL 8.x
- Docker / Docker Compose
- Swagger / OpenAPI 3

## Installation (Docker)
1. Assurez-vous d'avoir Docker et Docker Compose installés.
2. Clonez le projet.
3. Exécutez la commande suivante à la racine :
   ```bash
   docker-compose up --build
   ```
4. L'API sera accessible sur `http://localhost:8080/api`.
5. La documentation Swagger est sur `http://localhost:8080/api/swagger-ui.html`.

## Installation (Locale)
1. Configurez une base de données MySQL nommée `mutuelle_db`.
2. Modifiez `src/main/resources/application.yml` avec vos identifiants DB.
3. Exécutez l'application avec Maven :
   ```bash
   mvn spring-boot:run
   ```

## Comptes par défaut (via init.sql)
- **Super Admin** : 
  - Email: `superadmin@mutuelle.com`
  - Password: `password` (Haché: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi)

## Endpoints principaux
- `/api/auth/login` : Authentification
- `/api/members/register` : Inscription d'un membre (SG/Super Admin)
- `/api/savings/deposit` : Dépôt d'épargne (SG)
- `/api/borrowings/request` : Demande d'emprunt (SG)
- `/api/refueling/calculate/{exerciseId}` : Calcul renflouement (SG)
