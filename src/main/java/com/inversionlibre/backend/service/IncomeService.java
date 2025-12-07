package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.IncomeSource;
import com.inversionlibre.backend.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeSourceRepository incomeSourceRepository;

    public List<IncomeSource> getUserIncomes(String userId) {
        return incomeSourceRepository.findByUserId(userId);
    }

    public IncomeSource createIncome(IncomeSource income, String userId) {
        income.setUserId(userId);
        income.setCreatedAt(LocalDateTime.now());
        income.setUpdatedAt(LocalDateTime.now());
        return incomeSourceRepository.save(income);
    }

    public IncomeSource updateIncome(String id, IncomeSource incomeDetails, String userId) {
        IncomeSource income = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income source not found"));

        if (!income.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        income.setName(incomeDetails.getName());
        income.setAmount(incomeDetails.getAmount());
        income.setFrequency(incomeDetails.getFrequency());
        income.setColor(incomeDetails.getColor());
        income.setUpdatedAt(LocalDateTime.now());

        return incomeSourceRepository.save(income);
    }

    public void deleteIncome(String id, String userId) {
        IncomeSource income = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income source not found"));

        if (!income.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        incomeSourceRepository.delete(income);
    }
}
