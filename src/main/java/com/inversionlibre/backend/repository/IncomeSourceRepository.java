package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.IncomeSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeSourceRepository extends MongoRepository<IncomeSource, String> {
    List<IncomeSource> findByUserId(String userId);
}
