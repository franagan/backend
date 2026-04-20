package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.NetWorthCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NetWorthCategoryRepository extends MongoRepository<NetWorthCategory, String> {
    List<NetWorthCategory> findByUserId(String userId);
    List<NetWorthCategory> findByUserIdAndType(String userId, NetWorthCategory.CategoryType type);
}
