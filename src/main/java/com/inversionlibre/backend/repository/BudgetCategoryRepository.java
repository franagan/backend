package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.BudgetCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCategoryRepository extends MongoRepository<BudgetCategory, String> {
    List<BudgetCategory> findByUserId(String userId);
}
