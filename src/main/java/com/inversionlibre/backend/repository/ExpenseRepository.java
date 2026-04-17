package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {
    List<Expense> findByUserId(String userId);
    List<Expense> findByLinkedGoalId(String linkedGoalId);
    List<Expense> findByUserIdAndDateBetween(String userId, java.time.LocalDate start, java.time.LocalDate end);
}
