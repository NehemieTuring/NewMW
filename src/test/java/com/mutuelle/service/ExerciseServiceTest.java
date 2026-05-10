package com.mutuelle.service;

import com.mutuelle.entity.Exercise;
import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.User;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseService — Tests Unitaires")
class ExerciseServiceTest {

    @Mock private ExerciseRepository exerciseRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    private Administrator admin;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Admin").firstName("Super").email("admin@test.com").build();
        admin = Administrator.builder().id(1L).user(user).username("admin1").build();
    }

    // ========================================================================
    // Création d'exercice
    // ========================================================================
    @Nested
    @DisplayName("Création d'exercice (createExercise)")
    class CreateExerciseTests {

        @Test
        @DisplayName("Création refusée si l'année est vide")
        void shouldRejectEmptyYear() {
            Exercise exercise = Exercise.builder().year("").build();

            assertThatThrownBy(() -> exerciseService.createExercise(exercise, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("année");
        }

        @Test
        @DisplayName("Création refusée si l'année est null")
        void shouldRejectNullYear() {
            Exercise exercise = Exercise.builder().year(null).build();

            assertThatThrownBy(() -> exerciseService.createExercise(exercise, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("année");
        }

        @Test
        @DisplayName("Création refusée si l'année existe déjà")
        void shouldRejectDuplicateYear() {
            Exercise exercise = Exercise.builder().year("2026")
                    .startDate(LocalDate.of(2026, 1, 1))
                    .endDate(LocalDate.of(2026, 12, 31)).build();
            when(exerciseRepository.findByYear("2026")).thenReturn(Optional.of(new Exercise()));

            assertThatThrownBy(() -> exerciseService.createExercise(exercise, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("existe déjà");
        }

        @Test
        @DisplayName("Création refusée si un exercice actif existe")
        void shouldRejectWhenActiveExerciseExists() {
            Exercise exercise = Exercise.builder().year("2027")
                    .startDate(LocalDate.of(2027, 1, 1))
                    .endDate(LocalDate.of(2027, 12, 31)).build();
            when(exerciseRepository.findByYear("2027")).thenReturn(Optional.empty());
            when(exerciseRepository.findByActiveTrue()).thenReturn(Optional.of(new Exercise()));

            assertThatThrownBy(() -> exerciseService.createExercise(exercise, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("exercice est déjà en cours");
        }

        @Test
        @DisplayName("Création refusée si date de début manquante")
        void shouldRejectMissingStartDate() {
            Exercise exercise = Exercise.builder().year("2027").startDate(null)
                    .endDate(LocalDate.of(2027, 12, 31)).build();
            when(exerciseRepository.findByYear("2027")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exerciseService.createExercise(exercise, admin))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("date de début");
        }

        @Test
        @DisplayName("Création réussie avec valeurs par défaut")
        void shouldCreateWithDefaults() {
            Exercise exercise = Exercise.builder().year("2027")
                    .startDate(LocalDate.of(2027, 1, 1))
                    .endDate(LocalDate.of(2027, 12, 31)).build();

            when(exerciseRepository.findByYear("2027")).thenReturn(Optional.empty());
            when(exerciseRepository.findByActiveTrue()).thenReturn(Optional.empty());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(i -> {
                Exercise e = i.getArgument(0);
                e.setId(1L);
                return e;
            });

            Exercise result = exerciseService.createExercise(exercise, admin);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getAdministrator()).isEqualTo(admin);
            assertThat(result.getSolidarityAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
            assertThat(result.getAgapeAmount()).isEqualByComparingTo(new BigDecimal("45000.00"));
            assertThat(result.getPenaltyAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(result.getInterestRate()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.getInscriptionAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Création conserve les valeurs financières personnalisées")
        void shouldKeepCustomFinancialValues() {
            Exercise exercise = Exercise.builder().year("2027")
                    .startDate(LocalDate.of(2027, 1, 1))
                    .endDate(LocalDate.of(2027, 12, 31))
                    .solidarityAmount(new BigDecimal("200000"))
                    .interestRate(new BigDecimal("5.00"))
                    .build();

            when(exerciseRepository.findByYear("2027")).thenReturn(Optional.empty());
            when(exerciseRepository.findByActiveTrue()).thenReturn(Optional.empty());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(i -> i.getArgument(0));

            Exercise result = exerciseService.createExercise(exercise, admin);

            assertThat(result.getSolidarityAmount()).isEqualByComparingTo(new BigDecimal("200000"));
            assertThat(result.getInterestRate()).isEqualByComparingTo(new BigDecimal("5.00"));
        }
    }

    // ========================================================================
    // Clôture d'exercice
    // ========================================================================
    @Nested
    @DisplayName("Clôture d'exercice (closeExercise)")
    class CloseExerciseTests {

        @Test
        @DisplayName("Clôture désactive l'exercice")
        void shouldDeactivateExercise() {
            Exercise exercise = Exercise.builder().id(1L).year("2026").active(true).build();
            when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
            when(exerciseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            exerciseService.closeExercise(1L);

            assertThat(exercise.isActive()).isFalse();
            verify(exerciseRepository).save(exercise);
        }
    }

    // ========================================================================
    // Mise à jour de l'exercice
    // ========================================================================
    @Nested
    @DisplayName("Mise à jour (updateExercise)")
    class UpdateExerciseTests {

        @Test
        @DisplayName("Met à jour uniquement les champs non-null")
        void shouldUpdateOnlyNonNullFields() {
            Exercise existing = Exercise.builder().id(1L).year("2026")
                    .solidarityAmount(new BigDecimal("150000"))
                    .agapeAmount(new BigDecimal("45000"))
                    .penaltyAmount(new BigDecimal("15000"))
                    .interestRate(new BigDecimal("3.00"))
                    .build();
            when(exerciseRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(exerciseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Exercise details = new Exercise();
            details.setInterestRate(new BigDecimal("5.00"));

            Exercise result = exerciseService.updateExercise(1L, details);

            assertThat(result.getInterestRate()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.getSolidarityAmount()).isEqualByComparingTo(new BigDecimal("150000")); // Unchanged
        }
    }

    // ========================================================================
    // Exercice actif
    // ========================================================================
    @Nested
    @DisplayName("Récupération de l'exercice actif")
    class GetActiveExerciseTests {

        @Test
        @DisplayName("Exception si aucun exercice actif")
        void shouldThrowIfNoActiveExercise() {
            when(exerciseRepository.findByActiveTrue()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exerciseService.getActiveExercise())
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("findActiveExercise retourne Optional.empty() au lieu d'exception")
        void shouldReturnEmptyOptional() {
            when(exerciseRepository.findByActiveTrue()).thenReturn(Optional.empty());

            assertThat(exerciseService.findActiveExercise()).isEmpty();
        }
    }
}
