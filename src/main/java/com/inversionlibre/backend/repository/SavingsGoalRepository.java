package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.SavingsGoal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends MongoRepository<SavingsGoal, String> {
    List<SavingsGoal> findByUserId(String userId);
}
