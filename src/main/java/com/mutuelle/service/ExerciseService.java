package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Exercise;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    @Transactional
    public Exercise createExercise(Exercise exercise, Administrator administrator) {
        // Validate year
        if (exercise.getYear() == null || exercise.getYear().trim().isEmpty()) {
            throw new BusinessException("L'année de l'exercice est obligatoire.");
        }
        exercise.setYear(exercise.getYear().trim());

        // Check for duplicate year
        if (exerciseRepository.findByYear(exercise.getYear()).isPresent()) {
            throw new BusinessException("L'exercice pour l'année " + exercise.getYear() + " existe déjà.");
        }

        // Validate dates
        if (exercise.getStartDate() == null) {
            throw new BusinessException("La date de début est obligatoire.");
        }
        if (exercise.getEndDate() == null) {
            throw new BusinessException("La date de fin est obligatoire.");
        }

        // Check if an active exercise already exists
        if (exerciseRepository.findByActiveTrue().isPresent()) {
            throw new BusinessException("Impossible de créer un nouvel exercice : un exercice est déjà en cours. Veuillez le clôturer avant d'en créer un nouveau.");
        }

        // Set defaults for missing financial fields
        if (exercise.getSolidarityAmount() == null) {
            exercise.setSolidarityAmount(new BigDecimal("150000.00"));
        }
        if (exercise.getAgapeAmount() == null) {
            exercise.setAgapeAmount(new BigDecimal("45000.00"));
        }
        if (exercise.getPenaltyAmount() == null) {
            exercise.setPenaltyAmount(new BigDecimal("15000.00"));
        }
        if (exercise.getInterestRate() == null) {
            exercise.setInterestRate(new BigDecimal("3.00"));
        }
        if (exercise.getInscriptionAmount() == null) {
            exercise.setInscriptionAmount(new BigDecimal("50000.00"));
        }

        // Set the administrator and activate
        exercise.setAdministrator(administrator);
        exercise.setActive(true);
        exercise.setId(null); // Ensure we create a NEW exercise

        System.out.println("Saving exercise: year=" + exercise.getYear() 
            + ", start=" + exercise.getStartDate() 
            + ", end=" + exercise.getEndDate()
            + ", admin=" + administrator.getId());

        return exerciseRepository.save(exercise);
    }

    public List<Exercise> getAllExercises() {
        return exerciseRepository.findAll();
    }

    public Exercise getActiveExercise() {
        return exerciseRepository.findByActiveTrue()
                .orElseThrow(() -> new BusinessException("No active exercise found"));
    }

    public java.util.Optional<Exercise> findActiveExercise() {
        return exerciseRepository.findByActiveTrue();
    }

    @Transactional
    public void closeExercise(Long id) {
        Exercise exercise = exerciseRepository.findById(id).orElseThrow();
        exercise.setActive(false);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public Exercise updateExercise(Long id, Exercise details) {
        Exercise exercise = exerciseRepository.findById(id).orElseThrow();
        if (details.getSolidarityAmount() != null) exercise.setSolidarityAmount(details.getSolidarityAmount());
        if (details.getAgapeAmount() != null) exercise.setAgapeAmount(details.getAgapeAmount());
        if (details.getPenaltyAmount() != null) exercise.setPenaltyAmount(details.getPenaltyAmount());
        if (details.getInterestRate() != null) exercise.setInterestRate(details.getInterestRate());
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        exerciseRepository.deleteById(id);
    }
}
