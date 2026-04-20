package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.NetWorthEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetWorthEntryRepository extends MongoRepository<NetWorthEntry, String> {
    List<NetWorthEntry> findByUserIdOrderByDateDesc(String userId);
    List<NetWorthEntry> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate start, LocalDate end);
    Optional<NetWorthEntry> findByUserIdAndCategoryIdAndDate(String userId, String categoryId, LocalDate date);
}
