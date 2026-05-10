package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Exercise;
import com.mutuelle.entity.Session;
import com.mutuelle.enums.SessionState;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — Tests Unitaires")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ExerciseService exerciseService;

    @InjectMocks
    private SessionService sessionService;

    private Administrator admin;
    private Exercise activeExercise;

    @BeforeEach
    void setUp() {
        admin = Administrator.builder().id(1L).username("admin").build();
        activeExercise = Exercise.builder().id(1L).year("2026").active(true).build();
    }

    @Nested
    @DisplayName("Création de session (createSession)")
    class CreateSessionTests {

        @Test
        @DisplayName("Crée une session et désactive la session précédente")
        void shouldCreateSessionAndDeactivatePrevious() {
            Session oldSession = Session.builder().id(10L).active(true).build();
            when(sessionRepository.findByActiveTrue()).thenReturn(Optional.of(oldSession));
            when(exerciseService.getActiveExercise()).thenReturn(activeExercise);
            when(sessionRepository.findMaxSessionNumberByExerciseId(1L)).thenReturn(5);
            when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

            Session newSession = Session.builder().date(LocalDate.now()).build();
            Session result = sessionService.createSession(newSession, admin);

            assertThat(oldSession.isActive()).isFalse();
            assertThat(result.isActive()).isTrue();
            assertThat(result.getSessionNumber()).isEqualTo(6);
            assertThat(result.getState()).isEqualTo(SessionState.OPEN);
            assertThat(result.getExercise()).isEqualTo(activeExercise);
            verify(sessionRepository, times(2)).save(any(Session.class));
        }

        @Test
        @DisplayName("Le numéro de session commence à 1 pour un nouvel exercice")
        void shouldStartSessionNumberAtOne() {
            when(sessionRepository.findByActiveTrue()).thenReturn(Optional.empty());
            when(exerciseService.getActiveExercise()).thenReturn(activeExercise);
            when(sessionRepository.findMaxSessionNumberByExerciseId(1L)).thenReturn(null);
            when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

            Session newSession = Session.builder().date(LocalDate.now()).build();
            Session result = sessionService.createSession(newSession, admin);

            assertThat(result.getSessionNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Clôture de session (closeSession)")
    class CloseSessionTests {

        @Test
        @DisplayName("Ferme une session existante")
        void shouldCloseExistingSession() {
            Session session = Session.builder().id(1L).active(true).state(SessionState.OPEN).build();
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

            Session result = sessionService.closeSession(1L);

            assertThat(result.isActive()).isFalse();
            assertThat(result.getState()).isEqualTo(SessionState.CLOSED);
            assertThat(result.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("Erreur si la session n'existe pas")
        void shouldThrowExceptionIfSessionNotFound() {
            when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.closeSession(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("introuvable");
        }
    }
}
