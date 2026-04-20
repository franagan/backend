package com.inversionlibre.backend.service;

import com.inversionlibre.backend.dto.networth.NetWorthSummaryDTO;
import com.inversionlibre.backend.model.NetWorthCategory;
import com.inversionlibre.backend.model.NetWorthEntry;
import com.inversionlibre.backend.repository.NetWorthCategoryRepository;
import com.inversionlibre.backend.repository.NetWorthEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetWorthService {

    private final NetWorthCategoryRepository categoryRepository;
    private final NetWorthEntryRepository entryRepository;

    public List<NetWorthCategory> getCategories(String userId) {
        List<NetWorthCategory> categories = categoryRepository.findByUserId(userId);
        if (categories.isEmpty()) {
            return seedDefaultCategories(userId);
        }
        return categories;
    }

    public NetWorthCategory createCategory(String userId, NetWorthCategory category) {
        category.setUserId(userId);
        return categoryRepository.save(category);
    }

    @Transactional
    public NetWorthCategory updateCategory(String userId, String id, NetWorthCategory updated) {
        NetWorthCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso");
        }

        category.setName(updated.getName());
        category.setGroupName(updated.getGroupName());
        category.setType(updated.getType());
        category.setIcon(updated.getIcon());
        category.setColor(updated.getColor());
        
        return categoryRepository.save(category);
    }

    @Transactional
    public NetWorthEntry saveEntry(String userId, String categoryId, LocalDate date, BigDecimal amount, String notes) {
        NetWorthCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso");
        }

        // Normalizar fecha al día 1
        LocalDate normalizedDate = date.withDayOfMonth(1);

        Optional<NetWorthEntry> existing = entryRepository.findByUserIdAndCategoryIdAndDate(userId, categoryId, normalizedDate);
        
        NetWorthEntry entry;
        if (existing.isPresent()) {
            entry = existing.get();
            entry.setAmount(amount);
            entry.setNotes(notes);
        } else {
            entry = NetWorthEntry.builder()
                    .userId(userId)
                    .categoryId(categoryId)
                    .date(normalizedDate)
                    .amount(amount)
                    .notes(notes)
                    .build();
        }
        
        return entryRepository.save(entry);
    }

    public NetWorthSummaryDTO getSummary(String userId) {
        List<NetWorthEntry> allEntries = entryRepository.findByUserIdOrderByDateDesc(userId);
        List<NetWorthCategory> categories = getCategories(userId);
        Map<String, NetWorthCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(NetWorthCategory::getId, c -> c));

        if (allEntries.isEmpty()) {
            return NetWorthSummaryDTO.builder()
                    .totalAssets(BigDecimal.ZERO)
                    .totalLiabilities(BigDecimal.ZERO)
                    .netWorth(BigDecimal.ZERO)
                    .monthlyChange(BigDecimal.ZERO)
                    .monthlyChangePercent(BigDecimal.ZERO)
                    .history(new ArrayList<>())
                    .assetsDistribution(new HashMap<>())
                    .liabilitiesDistribution(new HashMap<>())
                    .categoryBalances(new HashMap<>())
                    .build();
        }

        // Agrupar por fecha
        Map<LocalDate, List<NetWorthEntry>> entriesByDate = allEntries.stream()
                .collect(Collectors.groupingBy(NetWorthEntry::getDate));

        TreeMap<LocalDate, NetWorthSummaryDTO.HistoryEntry> historyMap = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Map.Entry<LocalDate, List<NetWorthEntry>> dateGroup : entriesByDate.entrySet()) {
            BigDecimal assets = BigDecimal.ZERO;
            BigDecimal liabilities = BigDecimal.ZERO;

            for (NetWorthEntry entry : dateGroup.getValue()) {
                NetWorthCategory cat = categoryMap.get(entry.getCategoryId());
                if (cat != null) {
                    if (cat.getType() == NetWorthCategory.CategoryType.ASSET) {
                        assets = assets.add(entry.getAmount());
                    } else {
                        liabilities = liabilities.add(entry.getAmount());
                    }
                }
            }

            historyMap.put(dateGroup.getKey(), new NetWorthSummaryDTO.HistoryEntry(
                    dateGroup.getKey().format(formatter),
                    assets,
                    liabilities,
                    assets.subtract(liabilities)
            ));
        }

        LocalDate latestDate = historyMap.lastKey();
        NetWorthSummaryDTO.HistoryEntry latest = historyMap.get(latestDate);
        
        // Calcular cambio mensual
        BigDecimal monthlyChange = BigDecimal.ZERO;
        BigDecimal monthlyChangePercent = BigDecimal.ZERO;
        
        LocalDate previousDate = historyMap.lowerKey(latestDate);
        if (previousDate != null) {
            NetWorthSummaryDTO.HistoryEntry prev = historyMap.get(previousDate);
            monthlyChange = latest.getNetWorth().subtract(prev.getNetWorth());
            if (prev.getNetWorth().compareTo(BigDecimal.ZERO) != 0) {
                monthlyChangePercent = monthlyChange.divide(prev.getNetWorth().abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        }

        // Distribución actual por GRUPO para el resumen global
        Map<String, BigDecimal> assetsDist = new HashMap<>();
        Map<String, BigDecimal> liabDist = new HashMap<>();
        Map<String, BigDecimal> catBalances = new HashMap<>();
        
        for (NetWorthEntry entry : entriesByDate.get(latestDate)) {
            NetWorthCategory cat = categoryMap.get(entry.getCategoryId());
            if (cat != null) {
                // Si no hay grupo, usamos el nombre de la cuenta para que salga desglosada individualmente
                String groupRaw = (cat.getGroupName() != null && !cat.getGroupName().trim().isEmpty()) 
                                    ? cat.getGroupName().trim() 
                                    : cat.getName();
                
                catBalances.put(cat.getId(), entry.getAmount());

                if (cat.getType() == NetWorthCategory.CategoryType.ASSET) {
                    assetsDist.merge(groupRaw, entry.getAmount(), BigDecimal::add);
                } else {
                    liabDist.merge(groupRaw, entry.getAmount(), BigDecimal::add);
                }
            }
        }

        return NetWorthSummaryDTO.builder()
                .totalAssets(latest.getAssets())
                .totalLiabilities(latest.getLiabilities())
                .netWorth(latest.getNetWorth())
                .monthlyChange(monthlyChange)
                .monthlyChangePercent(monthlyChangePercent)
                .history(new ArrayList<>(historyMap.values()))
                .assetsDistribution(assetsDist)
                .liabilitiesDistribution(liabDist)
                .categoryBalances(catBalances)
                .build();
    }

    @Transactional
    public void deleteCategory(String userId, String id) {
        NetWorthCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso");
        }

        // Eliminar entradas asociadas
        List<NetWorthEntry> entries = entryRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .filter(e -> e.getCategoryId().equals(id))
                .collect(Collectors.toList());
        
        entryRepository.deleteAll(entries);
        categoryRepository.delete(category);
    }

    private List<NetWorthCategory> seedDefaultCategories(String userId) {
        List<NetWorthCategory> defaults = Arrays.asList(
            NetWorthCategory.builder().userId(userId).name("Principal (Bancos)").groupName("Cuentas Bancarias").type(NetWorthCategory.CategoryType.ASSET).icon("landmark").color("#3b82f6").build(),
            NetWorthCategory.builder().userId(userId).name("Principal (Broker)").groupName("Inversiones").type(NetWorthCategory.CategoryType.ASSET).icon("bar-chart").color("#10b981").build(),
            NetWorthCategory.builder().userId(userId).name("Exchanges / Wallet").groupName("Criptomonedas").type(NetWorthCategory.CategoryType.ASSET).icon("bitcoin").color("#f59e0b").build(),
            NetWorthCategory.builder().userId(userId).name("Inmueble Principal").groupName("Inmuebles").type(NetWorthCategory.CategoryType.ASSET).icon("home").color("#ec4899").build(),
            NetWorthCategory.builder().userId(userId).name("Hipoteca Vivienda").groupName("Deudas").type(NetWorthCategory.CategoryType.LIABILITY).icon("home-minus").color("#ef4444").build()
        );
        return categoryRepository.saveAll(defaults);
    }
}
