package com.mutuelle.service;

import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Exercise;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    @Transactional
    public Exercise createExercise(Exercise exercise) {
        if (exerciseRepository.findByYear(exercise.getYear()).isPresent()) {
            throw new BusinessException("Exercise for year " + exercise.getYear() + " already exists");
        }
        
        // Deactivate current active exercise if any (optional, business rule says one at a time)
        exerciseRepository.findByActiveTrue().ifPresent(e -> {
            e.setActive(false);
            exerciseRepository.save(e);
        });

        exercise.setActive(true);
        return exerciseRepository.save(exercise);
    }

    public List<Exercise> getAllExercises() {
        return exerciseRepository.findAll();
    }

    public Exercise getActiveExercise() {
        return exerciseRepository.findByActiveTrue()
                .orElseThrow(() -> new BusinessException("No active exercise found"));
    }

    @Transactional
    public void closeExercise(Long id) {
        Exercise exercise = exerciseRepository.findById(id).orElseThrow();
        exercise.setActive(false);
        exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        exerciseRepository.deleteById(id);
    }
}
