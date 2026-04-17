package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.SavingsGoal;
import com.inversionlibre.backend.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final SavingsGoalRepository savingsGoalRepository;

    public List<SavingsGoal> getUserGoals(String userId) {
        return savingsGoalRepository.findByUserId(userId);
    }

    public SavingsGoal createGoal(SavingsGoal goal, String userId) {
        goal.setUserId(userId);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        return savingsGoalRepository.save(goal);
    }

    public SavingsGoal updateGoal(String id, SavingsGoal goalDetails, String userId) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        goal.setName(goalDetails.getName());
        goal.setCurrentAmount(goalDetails.getCurrentAmount());
        goal.setTargetAmount(goalDetails.getTargetAmount());
        goal.setIcon(goalDetails.getIcon());
        goal.setStartDate(goalDetails.getStartDate());
        goal.setDeadline(goalDetails.getDeadline());
        goal.setUpdatedAt(LocalDateTime.now());

        return savingsGoalRepository.save(goal);
    }

    public void deleteGoal(String id, String userId) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        savingsGoalRepository.delete(goal);
    }
}
